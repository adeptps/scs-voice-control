package com.voicecontrol.core

import android.content.Context
import com.voicecontrol.core.stt.OnlineSpeechRecognizerEngine
import com.voicecontrol.core.stt.SttEngine
import com.voicecontrol.core.stt.SttListener
import com.voicecontrol.core.stt.VoskSttEngine
import com.voicecontrol.core.util.NetworkUtil
import com.voicecontrol.core.vosk.VoskModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * High-level orchestrator that selects STT engine and emits voice events.
 */
class VoiceController(
    private val appContext: Context,
    private val modelProvider: VoskModelProvider,
    private val interpreter: CommandInterpreter,
) {

    private val listeners = CopyOnWriteArrayList<(VoiceEvent) -> Unit>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var activeEngine: SttEngine? = null
    private var listeningJob: Job? = null

    @Volatile
    var config: VoiceConfig = VoiceConfig()

    fun addListener(listener: (VoiceEvent) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (VoiceEvent) -> Unit) {
        listeners -= listener
    }

    fun start() {
        if (activeEngine != null || listeningJob?.isActive == true) return
        emit(VoiceEvent.ListeningStarted)

        val job = scope.launch {
            try {
                val engine = selectEngine()
                activeEngine = engine

                engine.start(object : SttListener {
                    override fun onPartial(text: String) {
                        emit(VoiceEvent.PartialText(text))
                    }

                    override fun onFinal(text: String) {
                        emit(VoiceEvent.FinalText(text))
                        val localeTag = config.locale.toLanguageTag()
                        val normalized = TextNorm.normalize(localeTag, text)
                        val interpreted = interpreter.interpret(localeTag, normalized)
                        if (interpreted == null) {
                            emit(VoiceEvent.NoCommandMatch(text = text))
                            return
                        }

                        val command = interpreted.copy(rawText = text)
                        if (command.confidence >= config.minCommandConfidence) {
                            emit(VoiceEvent.CommandRecognized(command))
                        } else {
                            emit(
                                VoiceEvent.NoCommandMatch(
                                    text = text,
                                    bestActionId = command.actionId,
                                    confidence = command.confidence,
                                    matchedPhrase = command.matchedPhrase,
                                )
                            )
                        }
                    }

                    override fun onError(message: String, cause: Throwable?) {
                        emit(VoiceEvent.Error(message, cause))
                    }
                }, config)

            } catch (t: Throwable) {
                val msg = t.message?.takeIf { it.isNotBlank() } ?: "Failed to start voice recognition"
                emit(VoiceEvent.Error(msg, t))
                stop()
            }
        }
        listeningJob = job
        job.invokeOnCompletion {
            if (listeningJob == job) listeningJob = null
        }
    }

    fun stop() {
        listeningJob?.cancel()
        listeningJob = null
        activeEngine?.stop()
        activeEngine = null
        emit(VoiceEvent.ListeningStopped)
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun selectEngine(): SttEngine {
        val onlineAvailable = OnlineSpeechRecognizerEngine.isAvailable(appContext)
        val offlineAvailable = modelProvider.hasInstalledModel(config.locale)
        return when (config.mode) {
            EngineMode.OFFLINE_ONLY -> VoskSttEngine(appContext, modelProvider)
            EngineMode.ONLINE_ONLY -> {
                if (!onlineAvailable) {
                    throw IllegalStateException(
                        "Online recognition is unavailable on this device. " +
                            "Use OFFLINE_ONLY or AUTO with an installed Vosk model."
                    )
                }
                OnlineSpeechRecognizerEngine(appContext)
            }
            EngineMode.AUTO -> {
                val onlineAllowed = config.manualOnlineEnabled && config.allowAutoOnlineSwitch
                // For fixed commands, offline Vosk + grammar is usually more robust in noisy environments.
                if (offlineAvailable) {
                    VoskSttEngine(appContext, modelProvider)
                } else if (onlineAllowed && NetworkUtil.hasInternet(appContext) && onlineAvailable) {
                    OnlineSpeechRecognizerEngine(appContext)
                } else {
                    VoskSttEngine(appContext, modelProvider)
                }
            }
        }
    }

    private fun emit(event: VoiceEvent) {
        for (l in listeners) {
            try {
                l(event)
            } catch (_: Throwable) {
                // Listener errors must not break the controller.
            }
        }
    }
}

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
        if (listeningJob != null) return
        emit(VoiceEvent.ListeningStarted)

        listeningJob = scope.launch {
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
                        val cmd = interpreter.interpret(localeTag, normalized)
                        if (cmd != null) {
                            emit(VoiceEvent.CommandRecognized(cmd.copy(rawText = text)))
                        }
                    }

                    override fun onError(message: String, cause: Throwable?) {
                        emit(VoiceEvent.Error(message, cause))
                    }
                }, config)

            } catch (t: Throwable) {
                emit(VoiceEvent.Error("Failed to start voice recognition", t))
                stop()
            }
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
        return when (config.mode) {
            EngineMode.OFFLINE_ONLY -> VoskSttEngine(appContext, modelProvider)
            EngineMode.ONLINE_ONLY -> OnlineSpeechRecognizerEngine(appContext)
            EngineMode.AUTO -> {
                val onlineAllowed = config.manualOnlineEnabled && config.allowAutoOnlineSwitch
                if (onlineAllowed && NetworkUtil.hasInternet(appContext)) {
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

package com.voicecontrol.core.stt

import android.content.Context
import com.voicecontrol.core.TextNorm
import com.voicecontrol.core.VoiceConfig
import com.voicecontrol.core.vosk.VoskModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.vosk.Recognizer

/**
 * Offline STT engine based on Vosk.
 *
 * This class expects a model to be installed on device storage.
 */
class VoskSttEngine(
    private val context: Context,
    private val modelProvider: VoskModelProvider,
) : SttEngine {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun start(listener: SttListener, config: VoiceConfig) {
        if (job != null) return

        job = scope.launch {
            try {
                val model = modelProvider.getModelOrThrow(config.locale)

                val grammarJson = if (config.enableVoskGrammar) {
                    val phrases = modelProvider.getGrammarPhrases(config.locale)
                    if (phrases.isNotEmpty()) {
                        phrasesToGrammarJson(phrases)
                    } else null
                } else null

                val recognizer = if (grammarJson != null) {
                    Recognizer(model, config.voskSampleRateHz, grammarJson)
                } else {
                    Recognizer(model, config.voskSampleRateHz)
                }

                modelProvider.runMicrophoneLoop(context, recognizer, config.voskSampleRateHz, object : VoskModelProvider.VoskLoopListener {
                    override fun onPartial(text: String) {
                        listener.onPartial(text)
                    }

                    override fun onFinal(text: String) {
                        listener.onFinal(text)
                    }

                    override fun onError(message: String, cause: Throwable?) {
                        listener.onError(message, cause)
                    }
                })

            } catch (t: Throwable) {
                val details = t.message?.takeIf { it.isNotBlank() } ?: "Unknown error"
                listener.onError("Vosk start failed: $details", t)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        modelProvider.stopMicrophoneLoop()
    }

    private fun phrasesToGrammarJson(phrases: List<String>): String {
        val normalized = phrases
            .map { TextNorm.normalize("", it) }
            .filter { it.isNotBlank() }
            .distinct()

        val escaped = normalized.joinToString(",") { "\"" + it.replace("\\\"", "\\\\\"") + "\"" }
        return "[$escaped]"
    }
}

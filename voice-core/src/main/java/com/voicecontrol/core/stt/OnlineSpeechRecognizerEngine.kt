package com.voicecontrol.core.stt

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voicecontrol.core.VoiceConfig

class OnlineSpeechRecognizerEngine(
    private val context: Context,
) : SttEngine {

    companion object {
        fun isAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    @Volatile
    private var isStopping = false

    override fun start(listener: SttListener, config: VoiceConfig) {
        if (recognizer != null) return
        if (!isAvailable(context)) {
            listener.onError(
                "SpeechRecognizer is not available on this device. " +
                    "Install or enable a speech recognition service, or use OFFLINE_ONLY with a Vosk model."
            )
            return
        }

        isStopping = false
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        fun restartListening(delayMs: Long = 160L) {
            mainHandler.postDelayed({
                if (isStopping || recognizer !== sr) return@postDelayed
                runCatching { sr.startListening(intent) }
                    .onFailure { t ->
                        listener.onError("SpeechRecognizer restart failed: ${t.message ?: "Unknown error"}", t)
                    }
            }, delayMs)
        }

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                if (isStopping) return
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT,
                    -> restartListening(delayMs = 220L)
                    else -> {
                        listener.onError("SpeechRecognizer error: $error")
                        restartListening(delayMs = 320L)
                    }
                }
            }

            override fun onResults(results: android.os.Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = texts?.firstOrNull().orEmpty()
                if (best.isNotBlank()) listener.onFinal(best)
                if (!isStopping) restartListening()
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = texts?.firstOrNull().orEmpty()
                if (best.isNotBlank()) listener.onPartial(best)
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })

        runCatching { sr.startListening(intent) }
            .onFailure { t ->
                listener.onError("SpeechRecognizer start failed: ${t.message ?: "Unknown error"}", t)
            }
    }

    override fun stop() {
        isStopping = true
        mainHandler.removeCallbacksAndMessages(null)
        val sr = recognizer ?: return
        try {
            sr.stopListening()
        } catch (_: Throwable) {
        }
        try {
            sr.cancel()
        } catch (_: Throwable) {
        }
        try {
            sr.destroy()
        } catch (_: Throwable) {
        }
        recognizer = null
    }
}

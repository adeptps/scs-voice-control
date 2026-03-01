package com.voicecontrol.core.stt

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voicecontrol.core.VoiceConfig

class OnlineSpeechRecognizerEngine(
    private val context: Context,
) : SttEngine {

    private var recognizer: SpeechRecognizer? = null

    override fun start(listener: SttListener, config: VoiceConfig) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("SpeechRecognizer is not available on this device")
            return
        }

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                listener.onError("SpeechRecognizer error: $error")
            }

            override fun onResults(results: android.os.Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = texts?.firstOrNull().orEmpty()
                if (best.isNotBlank()) listener.onFinal(best)
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = texts?.firstOrNull().orEmpty()
                if (best.isNotBlank()) listener.onPartial(best)
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })

        sr.startListening(intent)
    }

    override fun stop() {
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

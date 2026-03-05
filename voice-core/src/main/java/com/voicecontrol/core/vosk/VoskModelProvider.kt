package com.voicecontrol.core.vosk

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Provides ready-to-use Vosk Model instances and manages the microphone recognition loop.
 */
class VoskModelProvider(
    private val modelManager: VoskModelManager,
    private val grammarProvider: GrammarProvider? = null,
) {

    interface GrammarProvider {
        fun phrasesFor(locale: Locale): List<String>
    }

    interface VoskLoopListener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String, cause: Throwable? = null)
    }

    private val modelCache = ConcurrentHashMap<String, Model>()

    @Volatile
    private var speechService: SpeechService? = null

    fun getModelOrThrow(locale: Locale): Model {
        val dir = modelManager.resolveModelDir(locale)
            ?: throw IllegalStateException("No Vosk model installed for ${locale.toLanguageTag()}")

        val key = dir.absolutePath
        return modelCache[key] ?: synchronized(this) {
            modelCache[key] ?: createModel(dir).also { modelCache[key] = it }
        }
    }

    fun getGrammarPhrases(locale: Locale): List<String> {
        return grammarProvider?.phrasesFor(locale).orEmpty()
    }

    fun hasInstalledModel(locale: Locale): Boolean {
        return modelManager.resolveModelDir(locale)?.exists() == true
    }

    fun runMicrophoneLoop(
        context: Context,
        recognizer: Recognizer,
        sampleRateHz: Float,
        listener: VoskLoopListener,
    ) {
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            try {
                val service = SpeechService(recognizer, sampleRateHz)
                speechService = service
                service.startListening(object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) {
                        val text = extractText(hypothesis, preferPartial = true)
                        if (text.isNotBlank()) listener.onPartial(text)
                    }

                    override fun onResult(hypothesis: String?) {
                        val text = extractText(hypothesis, preferPartial = false)
                        if (text.isNotBlank()) listener.onFinal(text)
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        val text = extractText(hypothesis, preferPartial = false)
                        if (text.isNotBlank()) listener.onFinal(text)
                    }

                    override fun onError(exception: Exception?) {
                        listener.onError("Vosk error", exception)
                    }

                    override fun onTimeout() {
                        // No-op. The controller can restart listening if needed.
                    }
                })
            } catch (t: Throwable) {
                listener.onError("Failed to start Vosk SpeechService", t)
            } finally {
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
    }

    fun stopMicrophoneLoop() {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            try {
                speechService?.stop()
            } catch (_: Throwable) {
            }
            try {
                speechService?.shutdown()
            } catch (_: Throwable) {
            }
            speechService = null
        }
    }

    private fun createModel(dir: File): Model {
        if (!dir.exists()) throw IllegalStateException("Model directory does not exist: ${dir.absolutePath}")
        return Model(dir.absolutePath)
    }

    private fun extractText(rawHypothesis: String?, preferPartial: Boolean): String {
        if (rawHypothesis.isNullOrBlank()) return ""
        val raw = rawHypothesis.trim()
        if (!raw.startsWith("{")) return raw

        return runCatching {
            val json = JSONObject(raw)
            if (preferPartial) {
                json.optString("partial", "")
            } else {
                json.optString("text", "").ifBlank { json.optString("partial", "") }
            }
        }.getOrDefault(raw).trim()
    }
}

package com.voicecontrol.core.vosk

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Manages installed Vosk models on the device.
 */
class VoskModelManager(
    private val context: Context,
) {

    private val baseDir: File = File(context.filesDir, "vosk_models")
    private val registryFile: File = File(baseDir, "installed_locales.json")

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
    }

    fun getOrCreateModelDir(installedDir: String): File {
        val d = File(baseDir, installedDir)
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun registerLocale(localeTag: String, installedDir: String) {
        val json = loadRegistry()
        json.put(localeTag, installedDir)
        saveRegistry(json)
    }

    fun resolveInstalledDir(locale: Locale): String? {
        val json = loadRegistry()
        val full = locale.toLanguageTag()
        if (json.has(full)) return json.optString(full)

        val lang = locale.language
        val candidate = json.keys().asSequence().firstOrNull { it.startsWith(lang) }
        return candidate?.let { json.optString(it) }
    }

    fun resolveModelDir(locale: Locale): File? {
        val installed = resolveInstalledDir(locale) ?: return null
        val dir = File(baseDir, installed)
        return if (dir.exists()) dir else null
    }

    private fun loadRegistry(): JSONObject {
        if (!registryFile.exists()) return JSONObject()
        return try {
            JSONObject(registryFile.readText(Charsets.UTF_8))
        } catch (_: Throwable) {
            JSONObject()
        }
    }

    private fun saveRegistry(json: JSONObject) {
        baseDir.mkdirs()
        registryFile.writeText(json.toString(2), Charsets.UTF_8)
    }
}

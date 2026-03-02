package com.voicecontrol.core.modelpack

import android.content.Context
import com.voicecontrol.core.util.HashUtil
import com.voicecontrol.core.util.HttpUtil
import com.voicecontrol.core.util.ZipUtil
import com.voicecontrol.core.vosk.VoskModelManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Downloads and installs model packs described by a remote index.
 */
class ModelPackManager(
    private val context: Context,
    private val voskModelManager: VoskModelManager,
) {

    fun fetchCatalog(indexUrl: String): ModelCatalog {
        val tmp = File(context.cacheDir, "voice_model_index.json")
        HttpUtil.downloadToFile(indexUrl, tmp)
        val text = tmp.readText(Charsets.UTF_8)
        return parseCatalog(text)
    }

    fun installPack(
        pack: ModelPack,
        onDownloadProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null,
    ): File {
        if (pack.engine.lowercase() != "vosk") {
            throw ModelPackException("Unsupported engine: ${pack.engine}")
        }

        val zipFile = File(context.cacheDir, "${pack.id}.zip")
        HttpUtil.downloadToFile(
            url = pack.url,
            outFile = zipFile,
            onProgress = onDownloadProgress,
        )

        val hash = HashUtil.sha256Hex(zipFile)
        if (!hash.equals(pack.sha256, ignoreCase = true)) {
            throw ModelPackException("SHA-256 mismatch for ${pack.id}")
        }

        val outDir = voskModelManager.getOrCreateModelDir(pack.installedDir)
        val marker = File(outDir, ".installed_${pack.id}")
        if (marker.exists()) return outDir

        outDir.deleteRecursively()
        outDir.mkdirs()

        ZipUtil.unzip(zipFile, outDir)

        marker.writeText("ok", Charsets.UTF_8)

        voskModelManager.registerLocale(pack.locale, pack.installedDir)

        return outDir
    }

    private fun parseCatalog(json: String): ModelCatalog {
        val root = JSONObject(json)
        val schema = root.optInt("schema", 1)
        val generatedAt = root.optString("generated_at", "")
        val packsJson = root.optJSONArray("packs") ?: JSONArray()

        val packs = mutableListOf<ModelPack>()
        for (i in 0 until packsJson.length()) {
            val o = packsJson.getJSONObject(i)
            packs += ModelPack(
                id = o.getString("id"),
                engine = o.optString("engine", "vosk"),
                lang = o.optString("lang", ""),
                locale = o.optString("locale", ""),
                variant = o.optString("variant", ""),
                version = o.optString("version", ""),
                url = o.getString("url"),
                sizeBytes = o.optLong("size_bytes", 0L),
                sha256 = o.getString("sha256"),
                license = o.optString("license", ""),
                installedDir = o.getString("installed_dir"),
                minSdk = o.optInt("min_sdk", 23),
                recommended = o.optBoolean("recommended", false),
            )
        }

        return ModelCatalog(schema = schema, generatedAt = generatedAt, packs = packs)
    }
}

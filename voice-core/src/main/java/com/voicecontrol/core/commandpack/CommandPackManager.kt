package com.voicecontrol.core.commandpack

import android.content.Context
import com.voicecontrol.core.util.HashUtil
import com.voicecontrol.core.util.HttpUtil
import com.voicecontrol.core.util.ZipUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Downloads and installs command packs described by a remote index.
 */
class CommandPackManager(
    private val context: Context,
) {

    private val baseDir: File = File(context.filesDir, "voice_command_packs")

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
    }

    fun fetchCatalog(indexUrl: String): CommandCatalog {
        val tmp = File(context.cacheDir, "voice_command_index.json")
        HttpUtil.downloadToFile(indexUrl, tmp)
        val text = tmp.readText(Charsets.UTF_8)
        return parseCatalog(text)
    }

    fun installPack(pack: CommandPack): File {
        val zipFile = File(context.cacheDir, "${pack.id}.zip")
        HttpUtil.downloadToFile(pack.url, zipFile)

        val hash = HashUtil.sha256Hex(zipFile)
        if (!hash.equals(pack.sha256, ignoreCase = true)) {
            throw CommandPackException("SHA-256 mismatch for ${pack.id}")
        }

        val outDir = File(baseDir, pack.id)
        val marker = File(outDir, ".installed_${pack.version}")
        if (marker.exists()) return outDir

        outDir.deleteRecursively()
        outDir.mkdirs()
        ZipUtil.unzip(zipFile, outDir)
        marker.writeText("ok", Charsets.UTF_8)

        return outDir
    }

    fun loadInstalledCommandSet(packId: String): CommandSet {
        val dir = File(baseDir, packId)
        val commandsJson = File(dir, "commands.json")
        if (!commandsJson.exists()) {
            throw CommandPackException("commands.json not found for pack $packId")
        }
        return CommandSet.fromJson(commandsJson.readText(Charsets.UTF_8))
    }

    private fun parseCatalog(json: String): CommandCatalog {
        val root = JSONObject(json)
        val schema = root.optInt("schema", 1)
        val generatedAt = root.optString("generated_at", "")
        val packsJson = root.optJSONArray("packs") ?: JSONArray()

        val packs = mutableListOf<CommandPack>()
        for (i in 0 until packsJson.length()) {
            val o = packsJson.getJSONObject(i)
            packs += CommandPack(
                id = o.getString("id"),
                lang = o.optString("lang", ""),
                locale = o.optString("locale", ""),
                version = o.optString("version", ""),
                url = o.getString("url"),
                sizeBytes = o.optLong("size_bytes", 0L),
                sha256 = o.getString("sha256"),
                license = o.optString("license", ""),
                recommended = o.optBoolean("recommended", false),
            )
        }

        return CommandCatalog(schema = schema, generatedAt = generatedAt, packs = packs)
    }
}

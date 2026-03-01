package com.voicecontrol.core.util

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object HttpUtil {

    fun downloadToFile(url: String, outFile: File, connectTimeoutMs: Int = 15_000, readTimeoutMs: Int = 60_000) {
        outFile.parentFile?.mkdirs()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
        }

        conn.inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        conn.disconnect()
    }
}

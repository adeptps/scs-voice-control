package com.voicecontrol.core.util

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object HttpUtil {

    fun downloadToFile(
        url: String,
        outFile: File,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 60_000,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null,
    ) {
        outFile.parentFile?.mkdirs()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
        }

        try {
            conn.connect()
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode} while downloading $url")
            }
            val totalBytes = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
            val buffer = ByteArray(64 * 1024)
            var downloaded = 0L

            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress?.invoke(downloaded, totalBytes)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}

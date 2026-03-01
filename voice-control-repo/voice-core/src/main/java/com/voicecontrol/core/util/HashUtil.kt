package com.voicecontrol.core.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object HashUtil {

    fun sha256Hex(file: File): String {
        file.inputStream().use { return sha256Hex(it) }
    }

    fun sha256Hex(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(64 * 1024)
        while (true) {
            val r = input.read(buf)
            if (r <= 0) break
            digest.update(buf, 0, r)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

package com.voicecontrol.core.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipUtil {

    fun unzip(zipFile: File, outDir: File) {
        if (!outDir.exists()) outDir.mkdirs()

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            while (true) {
                val entry: ZipEntry = zis.nextEntry ?: break
                val outPath = File(outDir, entry.name)

                val canonicalOutDir = outDir.canonicalPath
                val canonicalOutPath = outPath.canonicalPath
                if (!canonicalOutPath.startsWith(canonicalOutDir + File.separator)) {
                    throw IllegalArgumentException("Zip path traversal detected: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outPath.mkdirs()
                } else {
                    outPath.parentFile?.mkdirs()
                    FileOutputStream(outPath).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }
}

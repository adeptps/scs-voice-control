package com.voicecontrol.core

import java.text.Normalizer

object TextNorm {

    fun normalize(localeTag: String, input: String): String {
        val trimmed = input.trim()
        val lowered = trimmed.lowercase()
        val nfc = Normalizer.normalize(lowered, Normalizer.Form.NFC)
        return when {
            localeTag.startsWith("ar") -> normalizeArabic(nfc)
            else -> normalizeGeneric(nfc)
        }
    }

    private fun normalizeGeneric(s: String): String {
        return s
            .replace(Regex("[\\p{Punct}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeArabic(s: String): String {
        val withoutDiacritics = s.replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        val unified = withoutDiacritics
            .replace("\u0640", "") // tatweel
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace('ة', 'ه')
            .replace(Regex("(.)\\1{2,}"), "$1") // collapse elongated letters

        val compact = unified
            .replace(Regex("[\\p{Punct}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Remove common politeness fillers that may come from noisy speech.
        return compact
            .replace(Regex("^(لو سمحت|من فضلك|يا سياره|يا سيارة)\\s+"), "")
            .trim()
    }
}

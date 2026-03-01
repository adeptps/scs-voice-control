package com.voicecontrol.core.commandpack

import com.voicecontrol.core.TextNorm
import org.json.JSONArray
import org.json.JSONObject

/**
 * In-memory representation of a fixed-phrase command pack.
 */
class CommandSet private constructor(
    val packId: String,
    val localeTag: String,
    private val phraseToId: Map<String, String>,
    private val allPhrases: List<String>,
) {

    fun match(localeTag: String, normalizedText: String): String? {
        if (!this.localeTag.equals(localeTag, ignoreCase = true)) return null
        return phraseToId[normalizedText]
    }

    fun phrases(): List<String> = allPhrases

    companion object {
        fun fromJson(json: String): CommandSet {
            val root = JSONObject(json)
            val packId = root.optString("pack_id", root.optString("id", ""))
            val localeTag = root.optString("locale", "")

            val commands = root.optJSONArray("commands") ?: JSONArray()
            val map = linkedMapOf<String, String>()
            val phrases = mutableListOf<String>()

            for (i in 0 until commands.length()) {
                val cmd = commands.getJSONObject(i)
                val id = cmd.getString("id")
                val ph = cmd.optJSONArray("phrases") ?: JSONArray()
                for (j in 0 until ph.length()) {
                    val raw = ph.getString(j)
                    val norm = TextNorm.normalize(localeTag, raw)
                    if (norm.isBlank()) continue
                    map[norm] = id
                    phrases += norm
                }
            }

            return CommandSet(
                packId = packId,
                localeTag = localeTag,
                phraseToId = map,
                allPhrases = phrases.distinct(),
            )
        }
    }
}

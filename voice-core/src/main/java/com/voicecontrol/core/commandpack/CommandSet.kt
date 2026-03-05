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

    data class MatchResult(
        val actionId: String,
        val matchedPhrase: String,
        val confidence: Float,
        val strategy: Strategy,
    ) {
        enum class Strategy {
            EXACT,
            CONTAINED,
            APPROXIMATE,
        }
    }

    fun match(localeTag: String, normalizedText: String): String? {
        return matchDetailed(localeTag, normalizedText)?.actionId
    }

    fun matchDetailed(localeTag: String, normalizedText: String): MatchResult? {
        if (!this.localeTag.equals(localeTag, ignoreCase = true)) return null
        val exact = phraseToId[normalizedText]
        if (exact != null) {
            return MatchResult(
                actionId = exact,
                matchedPhrase = normalizedText,
                confidence = 1.0f,
                strategy = MatchResult.Strategy.EXACT,
            )
        }
        if (normalizedText.isBlank()) return null

        // If recognition returns extra words, allow contained fixed phrase.
        val contained = findContainedPhrase(normalizedText)
        if (contained != null) {
            val actionId = phraseToId[contained.first] ?: return null
            return MatchResult(
                actionId = actionId,
                matchedPhrase = contained.first,
                confidence = contained.second,
                strategy = MatchResult.Strategy.CONTAINED,
            )
        }

        // Fallback to fuzzy match for small recognition mistakes, e.g.:
        // "شاغيل التكييف" vs "شغل التكييف".
        val approximate = findApproximatePhrase(normalizedText) ?: return null
        val actionId = phraseToId[approximate.phrase] ?: return null
        val confidence = (1f - (approximate.distance.toFloat() / approximate.maxLen.toFloat()))
            .let { if (localeTag.startsWith("ar", ignoreCase = true)) it + 0.06f else it }
            .coerceIn(0f, 0.96f)

        return MatchResult(
            actionId = actionId,
            matchedPhrase = approximate.phrase,
            confidence = confidence,
            strategy = MatchResult.Strategy.APPROXIMATE,
        )
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

    private data class ApproximateCandidate(
        val phrase: String,
        val distance: Int,
        val maxLen: Int,
    )

    private fun findContainedPhrase(normalizedText: String): Pair<String, Float>? {
        val bestPhrase = phraseToId.keys
            .asSequence()
            .filter { p ->
                p.length >= 6 &&
                    (normalizedText.contains(p) || p.contains(normalizedText))
            }
            .maxByOrNull { p ->
                val overlap = tokenOverlap(normalizedText, p)
                (overlap * 1000f + p.length).toInt()
            } ?: return null

        val overlap = tokenOverlap(normalizedText, bestPhrase)
        val base = if (normalizedText.contains(bestPhrase)) 0.82f else 0.72f
        val confidence = (base + overlap * 0.18f).coerceIn(0.65f, 0.95f)
        return bestPhrase to confidence
    }

    private fun findApproximatePhrase(normalizedText: String): ApproximateCandidate? {
        val isArabic = localeTag.startsWith("ar", ignoreCase = true)
        val textTokens = normalizedText.split(' ').filter { it.isNotBlank() }
        if (textTokens.isEmpty()) return null

        val textSkeleton = if (isArabic) arabicSkeleton(normalizedText) else normalizedText
        var bestPhrase: String? = null
        var bestDistance = Int.MAX_VALUE

        for (phrase in phraseToId.keys) {
            val phraseTokens = phrase.split(' ').filter { it.isNotBlank() }
            if (kotlin.math.abs(phraseTokens.size - textTokens.size) > 2) continue

            val directDistance = levenshtein(normalizedText, phrase)
            var distance = directDistance

            if (isArabic) {
                val phraseSkeleton = arabicSkeleton(phrase)
                val skeletonDistance = levenshtein(textSkeleton, phraseSkeleton)
                distance = minOf(distance, skeletonDistance)
            }

            if (distance < bestDistance) {
                bestDistance = distance
                bestPhrase = phrase
            }
        }

        val phrase = bestPhrase ?: return null
        val maxLen = maxOf(normalizedText.length, phrase.length)
        val allowed = maxOf(2, (maxLen * 0.22f).toInt())
        if (bestDistance > allowed) return null
        return ApproximateCandidate(
            phrase = phrase,
            distance = bestDistance,
            maxLen = maxLen,
        )
    }

    private fun tokenOverlap(a: String, b: String): Float {
        val ta = a.split(' ').filter { it.isNotBlank() }.toSet()
        val tb = b.split(' ').filter { it.isNotBlank() }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0f
        val intersection = ta.intersect(tb).size.toFloat()
        val union = ta.union(tb).size.toFloat()
        if (union == 0f) return 0f
        return intersection / union
    }

    private fun arabicSkeleton(s: String): String {
        return s
            .replace(Regex("[اوي]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in 1..a.length) {
            curr[0] = i
            val ca = a[i - 1]
            for (j in 1..b.length) {
                val cost = if (ca == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,
                    curr[j - 1] + 1,
                    prev[j - 1] + cost
                )
            }
            for (j in 0..b.length) prev[j] = curr[j]
        }
        return prev[b.length]
    }
}

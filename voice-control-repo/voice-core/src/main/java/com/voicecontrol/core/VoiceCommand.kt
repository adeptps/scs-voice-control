package com.voicecontrol.core

/**
 * Parsed, validated command representation.
 */
data class VoiceCommand(
    val actionId: String,
    val args: Map<String, String> = emptyMap(),
    val rawText: String = "",
)

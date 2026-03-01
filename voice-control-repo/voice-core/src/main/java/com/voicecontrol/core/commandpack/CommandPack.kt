package com.voicecontrol.core.commandpack

/**
 * Describes a downloadable command pack.
 */
data class CommandPack(
    val id: String,
    val lang: String,
    val locale: String,
    val version: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
    val license: String,
    val recommended: Boolean,
)

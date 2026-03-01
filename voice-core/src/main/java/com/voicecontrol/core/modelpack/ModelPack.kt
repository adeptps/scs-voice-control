package com.voicecontrol.core.modelpack

/**
 * Describes a downloadable Vosk model bundle.
 */
data class ModelPack(
    val id: String,
    val engine: String,
    val lang: String,
    val locale: String,
    val variant: String,
    val version: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
    val license: String,
    val installedDir: String,
    val minSdk: Int,
    val recommended: Boolean,
)

package com.voicecontrol.core

import java.util.Locale

/**
 * Runtime configuration for voice recognition.
 */
data class VoiceConfig(
    val mode: EngineMode = EngineMode.AUTO,
    val locale: Locale = Locale("ru", "RU"),
    val manualOnlineEnabled: Boolean = true,
    val allowAutoOnlineSwitch: Boolean = true,
    val voskSampleRateHz: Float = 16000f,
    val enableVoskGrammar: Boolean = true,
)

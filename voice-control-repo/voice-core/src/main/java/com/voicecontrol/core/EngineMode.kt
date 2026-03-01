package com.voicecontrol.core

/**
 * Defines how the controller chooses between offline and online speech recognition.
 */
enum class EngineMode {
    OFFLINE_ONLY,
    ONLINE_ONLY,
    AUTO
}

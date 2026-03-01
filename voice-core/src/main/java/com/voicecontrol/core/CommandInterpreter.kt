package com.voicecontrol.core

/**
 * Converts recognized text into an action.
 */
fun interface CommandInterpreter {
    fun interpret(localeTag: String, text: String): VoiceCommand?
}

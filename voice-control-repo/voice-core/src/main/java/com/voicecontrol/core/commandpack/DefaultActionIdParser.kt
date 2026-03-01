package com.voicecontrol.core.commandpack

import com.voicecontrol.core.VoiceCommand

/**
 * Converts actionId patterns into structured arguments.
 */
object DefaultActionIdParser : FixedPhraseInterpreter.ActionIdParser {

    override fun toCommand(actionId: String, rawText: String): VoiceCommand {
        // Examples:
        // car.seat.driver.level.2 -> args: seat=driver, level=2
        // car.window.front_left.open.50 -> args: window=front_left, value=50
        val parts = actionId.split('.')

        if (parts.size >= 5 && parts[0] == "car" && parts[1] == "seat" && parts[3] == "level") {
            val seat = parts[2]
            val level = parts[4]
            return VoiceCommand(actionId = "car.seat.set_level", args = mapOf("seat" to seat, "level" to level), rawText = rawText)
        }

        if (parts.size >= 5 && parts[0] == "car" && parts[1] == "window" && parts[3] == "open") {
            val window = parts[2]
            val value = parts[4]
            return VoiceCommand(actionId = "car.window.open", args = mapOf("window" to window, "value" to value), rawText = rawText)
        }

        if (parts.size >= 5 && parts[0] == "car" && parts[1] == "climate" && parts[2] == "temp" && parts[3] == "set") {
            val value = parts[4]
            return VoiceCommand(actionId = "car.climate.temp_set", args = mapOf("value" to value), rawText = rawText)
        }

        return VoiceCommand(actionId = actionId, rawText = rawText)
    }
}

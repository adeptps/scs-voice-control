package com.voicecontrol.core.commandpack

import com.voicecontrol.core.CommandInterpreter
import com.voicecontrol.core.VoiceCommand

/**
 * Exact phrase matching interpreter.
 */
class FixedPhraseInterpreter(
    private val commandSets: () -> List<CommandSet>,
    private val actionIdParser: ActionIdParser = DefaultActionIdParser,
) : CommandInterpreter {

    interface ActionIdParser {
        fun toCommand(actionId: String, rawText: String): VoiceCommand
    }

    override fun interpret(localeTag: String, text: String): VoiceCommand? {
        val sets = commandSets()
        for (s in sets) {
            val id = s.match(localeTag, text)
            if (id != null) return actionIdParser.toCommand(id, rawText = text)
        }
        return null
    }
}

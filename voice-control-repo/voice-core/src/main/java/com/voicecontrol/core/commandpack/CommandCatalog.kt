package com.voicecontrol.core.commandpack

data class CommandCatalog(
    val schema: Int,
    val generatedAt: String,
    val packs: List<CommandPack>,
)

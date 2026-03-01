package com.voicecontrol.core.modelpack

data class ModelCatalog(
    val schema: Int,
    val generatedAt: String,
    val packs: List<ModelPack>,
)

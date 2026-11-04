package dev.siluet.blueprint.definition

import kotlinx.serialization.Serializable

@Serializable
data class SectionDefinition(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val blocks: Map<String, BlockDefinition> = emptyMap(),
    val blocksOrder: List<String> = emptyList(),
)
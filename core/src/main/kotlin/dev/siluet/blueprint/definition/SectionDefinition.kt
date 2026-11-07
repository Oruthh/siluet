package dev.siluet.blueprint.definition

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class SectionDefinition(
    val name: String,
    val properties: Map<String, JsonPrimitive> = emptyMap(),
    val blocks: Map<String, BlockDefinition> = emptyMap(),
    val blocksOrder: List<String> = emptyList(),
)
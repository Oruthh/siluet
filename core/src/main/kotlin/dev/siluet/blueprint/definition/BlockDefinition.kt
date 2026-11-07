package dev.siluet.blueprint.definition

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class BlockDefinition(
    val name: String,
    val properties: Map<String, JsonPrimitive> = emptyMap(),
)

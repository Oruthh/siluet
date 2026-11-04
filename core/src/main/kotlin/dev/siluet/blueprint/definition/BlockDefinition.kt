package dev.siluet.blueprint.definition

import kotlinx.serialization.Serializable

@Serializable
internal data class BlockDefinition(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
)

package dev.siluet.blueprint.definition

import kotlinx.serialization.Serializable

@Serializable
data class BlockDefinition(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
)

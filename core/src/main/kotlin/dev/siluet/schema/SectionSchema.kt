package dev.siluet.schema

import kotlinx.serialization.Serializable

@Serializable
internal data class SectionSchema(
    val name: String,
    val description: String? = null,
    val properties: Map<String, PropertySchema>? = null,
    val blocks: List<BlockSchema>? = null,
)

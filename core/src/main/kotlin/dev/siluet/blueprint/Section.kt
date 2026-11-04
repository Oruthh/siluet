package dev.siluet.blueprint

internal data class Section (
    val name: String,
    val properties: Map<String, PropertyValue>,
    val blocks: List<Block>
)
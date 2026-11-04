package dev.siluet.blueprint

data class Section (
    val name: String,
    val properties: Map<String, PropertyValue>,
    val blocks: List<Block>
)
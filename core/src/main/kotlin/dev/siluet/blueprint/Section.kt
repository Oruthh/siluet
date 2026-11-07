package dev.siluet.blueprint

import dev.siluet.blueprint.property.PropertyValue

internal data class Section (
    val name: String,
    val properties: Map<String, PropertyValue<*>>,
    val blocks: List<Block>
)
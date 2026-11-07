package dev.siluet.blueprint

import dev.siluet.blueprint.property.PropertyValue

internal data class Block(
    val name: String,
    val properties: Map<String, PropertyValue<*>>
)
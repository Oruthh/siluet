package dev.siluet.schema

import kotlinx.serialization.Serializable

@Serializable
internal data class PropertySchema(
    val type: PropertyType,
    val label: String,
    val default: String? = null,
    val options: List<PropertyOption>? = null,
    val listType: PropertyType? = null,
)

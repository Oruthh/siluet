package dev.siluet.schema

import kotlinx.serialization.Serializable

@Serializable
internal data class PropertyOption(
    val value: String,
    val label: String
)

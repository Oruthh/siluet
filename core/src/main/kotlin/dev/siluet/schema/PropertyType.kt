package dev.siluet.schema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class PropertyType {
    @SerialName("text")
    TEXT,
    @SerialName("textarea")
    TEXTAREA,
    @SerialName("select")
    SELECT,
    @SerialName("decimal")
    DECIMAL,
    @SerialName("list")
    LIST,
}
package dev.siluet.blueprint.definition

import kotlinx.serialization.Serializable

@Serializable
data class TemplateDefinition(
    val layout: String = "main",
    val sections: Map<String, SectionDefinition> = emptyMap(),
    val sectionsOrder: List<String> = emptyList(),
)
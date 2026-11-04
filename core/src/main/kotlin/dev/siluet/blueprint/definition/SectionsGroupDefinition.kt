package dev.siluet.blueprint.definition

import kotlinx.serialization.Serializable

@Serializable
data class SectionsGroupDefinition(
    val sections: Map<String, SectionDefinition> = emptyMap(),
    val sectionsOrder: List<String> = emptyList(),
)
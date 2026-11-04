package dev.siluet.loader

import kotlinx.serialization.json.Json
import dev.siluet.exception.SectionsGroupNotFoundException
import dev.siluet.blueprint.SectionsGroup
import dev.siluet.blueprint.compile.SectionCompiler
import dev.siluet.blueprint.definition.SectionsGroupDefinition

internal class SiluetSectionsGroupLoader(
    val assetLoader: SiluetAssetLoader,
    val json: Json,
) {
    private val sectionsGroupsCache: MutableMap<String, SectionsGroup> = mutableMapOf()

    fun getOrCompileSectionsGroup(sectionName: String): SectionsGroup {
        return sectionsGroupsCache.computeIfAbsent(sectionName, this::compileSectionsGroup)
    }

    private fun compileSectionsGroup(name: String): SectionsGroup {
        val sectionsGroupSource = assetLoader.loadSectionsGroup(name)
            ?: throw SectionsGroupNotFoundException(name)

        val definition = json.decodeFromString<SectionsGroupDefinition>(sectionsGroupSource)

        val sectionsOrder = definition.sectionsOrder.ifEmpty {
            definition.sections.keys.toList()
        }

        val sections = sectionsOrder.map { sectionKey ->
            val sectionDef = definition.sections[sectionKey]!!

            SectionCompiler.compile(sectionDef)
        }
        
        return SectionsGroup(
            sections = sections
        )
    }
}
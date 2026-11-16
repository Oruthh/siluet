package dev.siluet.loader

import kotlinx.serialization.json.Json
import dev.siluet.exception.internal.SectionsGroupNotFoundException
import dev.siluet.blueprint.SectionsGroup
import dev.siluet.blueprint.compile.SectionsCompiler
import dev.siluet.blueprint.definition.SectionsGroupDefinition
import dev.siluet.cache.SiluetCache
import dev.siluet.exception.internal.SectionsCompilationException
import dev.siluet.exception.internal.SectionsGroupLoadingException

internal class SiluetSectionsGroupLoader(
    val assetLoader: SiluetAssetLoader,
    val json: Json,
    val cache: SiluetCache<String, SectionsGroup>
) {
    fun getOrCompileSectionsGroup(sectionName: String): SectionsGroup {
        return cache.computeIfAbsent(sectionName, this::compileSectionsGroup)
    }

    private fun compileSectionsGroup(name: String): SectionsGroup {
        val sectionsGroupAsset = assetLoader.loadSectionsGroup(name)
            ?: throw SectionsGroupNotFoundException(name)

        val definition = json.decodeFromString<SectionsGroupDefinition>(sectionsGroupAsset.content)

        val sections = try{
            SectionsCompiler.compile(definition.sections, definition.sectionsOrder)
        } catch (e: SectionsCompilationException) {
            throw SectionsGroupLoadingException(e.message!!, sectionsGroupAsset.sourceName)
        }
        
        return SectionsGroup(
            sections = sections,
        )
    }
}
package dev.siluet.loader

import kotlinx.serialization.json.Json
import dev.siluet.exception.internal.SectionsGroupNotFoundException
import dev.siluet.blueprint.SectionsGroup
import dev.siluet.blueprint.compile.SectionsCompiler
import dev.siluet.blueprint.definition.SectionsGroupDefinition
import dev.siluet.cache.CacheKey
import dev.siluet.cache.ThemeCache
import dev.siluet.exception.internal.SectionsCompilationException
import dev.siluet.exception.internal.SectionsGroupLoadingException

internal class SiluetSectionsGroupLoader(
    val assetLoader: SiluetAssetLoader,
    val json: Json,
    val cache: ThemeCache
) {
    fun getOrCompileSectionsGroup(groupName: String): SectionsGroup {
        return cache.computeIfAbsent(CacheKey.SectionsGroup(groupName)) {
            compileSectionsGroup(groupName)
        }
    }

    private fun compileSectionsGroup(name: String): SectionsGroup {
        val asset = assetLoader.loadSectionsGroup(name)
            ?: throw SectionsGroupNotFoundException(name)

        val definition = json.decodeFromString<SectionsGroupDefinition>(asset.content)

        val sections = try{
            SectionsCompiler.compile(definition.sections, definition.sectionsOrder)
        } catch (e: SectionsCompilationException) {
            throw SectionsGroupLoadingException(e.message!!, asset.sourceName)
        }
        
        return SectionsGroup(
            sections = sections,
        )
    }
}
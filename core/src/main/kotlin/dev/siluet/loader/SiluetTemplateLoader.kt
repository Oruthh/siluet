package dev.siluet.loader

import dev.siluet.exception.internal.TemplateNotFoundException
import dev.siluet.blueprint.Template
import dev.siluet.blueprint.compile.TemplateCompiler
import dev.siluet.blueprint.definition.TemplateDefinition
import dev.siluet.cache.CacheKey
import dev.siluet.cache.ThemeCache
import dev.siluet.exception.internal.SectionsCompilationException
import dev.siluet.exception.internal.TemplateLoadingException
import kotlinx.serialization.json.Json

internal class SiluetTemplateLoader(
    val assetLoader: SiluetAssetLoader,
    val json: Json,
    val cache: ThemeCache,
) {
    fun getOrCompileTemplate(templateName: String): Template {
        return cache.computeIfAbsent(CacheKey.Template(templateName)){
            compileTemplate(templateName)
        }
    }
    private fun compileTemplate(templateName: String): Template {
        val asset = assetLoader.loadTemplate(templateName)
            ?: throw TemplateNotFoundException(templateName)

        val definition = json.decodeFromString<TemplateDefinition>(asset.content)
        val template = try{
            TemplateCompiler.compile(definition)
        } catch (e: SectionsCompilationException) {
            throw TemplateLoadingException(e.message!!, asset.sourceName)
        }

        return Template(
            layout = template.layout,
            sections = template.sections
        )
    }
}
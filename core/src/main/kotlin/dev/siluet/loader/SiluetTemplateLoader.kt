package dev.siluet.loader

import dev.siluet.exception.internal.TemplateNotFoundException
import dev.siluet.blueprint.Template
import dev.siluet.blueprint.compile.TemplateCompiler
import dev.siluet.blueprint.definition.TemplateDefinition
import dev.siluet.cache.SiluetCache
import dev.siluet.exception.internal.SectionsCompilationException
import dev.siluet.exception.internal.TemplateLoadingException
import kotlinx.serialization.json.Json

internal class SiluetTemplateLoader(
    val assetLoader: SiluetAssetLoader,
    val json: Json,
    val cache: SiluetCache<String, Template>,
) {
    fun getOrCompileTemplate(templateName: String): Template {
        return cache.computeIfAbsent(templateName, this::compileTemplate)
    }
    private fun compileTemplate(templateName: String): Template {
        val templateAsset = assetLoader.loadTemplate(templateName)
            ?: throw TemplateNotFoundException(templateName)

        val definition = json.decodeFromString<TemplateDefinition>(templateAsset.content)
        val template = try{
            TemplateCompiler.compile(definition)
        } catch (e: SectionsCompilationException) {
            throw TemplateLoadingException(e.message!!, templateAsset.sourceName)
        }

        return Template(
            layout = template.layout,
            sections = template.sections
        )
    }
}
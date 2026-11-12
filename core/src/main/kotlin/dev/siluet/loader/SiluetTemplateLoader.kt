package dev.siluet.loader

import dev.siluet.exception.TemplateNotFoundException
import dev.siluet.blueprint.Template
import dev.siluet.blueprint.compile.TemplateCompiler
import dev.siluet.blueprint.definition.TemplateDefinition
import dev.siluet.cache.SiluetCache
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
        val templateSource = assetLoader.loadTemplate(templateName)
            ?: throw TemplateNotFoundException(templateName)

        val definition = json.decodeFromString<TemplateDefinition>(templateSource)
        val template = TemplateCompiler.compile(definition)

        return Template(
            layout = template.layout,
            sections = template.sections
        )
    }
}
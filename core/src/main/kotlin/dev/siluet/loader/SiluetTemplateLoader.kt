package dev.siluet.loader

import dev.siluet.exception.TemplateNotFoundException
import dev.siluet.blueprint.Template
import dev.siluet.blueprint.compile.TemplateCompiler
import dev.siluet.blueprint.definition.TemplateDefinition
import kotlinx.serialization.json.Json

class SiluetTemplateLoader(
    val assetLoader: SiluetAssetLoader,
    val json: Json
) {
    private val templatesCache: MutableMap<String, Template> = mutableMapOf()

    fun getOrCompileTemplate(templateName: String): Template {
        return templatesCache.computeIfAbsent(templateName, this::compileTemplate)
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
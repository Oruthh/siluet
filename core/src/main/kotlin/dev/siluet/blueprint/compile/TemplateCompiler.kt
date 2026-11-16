package dev.siluet.blueprint.compile

import dev.siluet.blueprint.Template
import dev.siluet.blueprint.definition.TemplateDefinition

internal object TemplateCompiler {
    fun compile(definition: TemplateDefinition): Template {
        return Template(
            layout = definition.layout,
            sections = SectionsCompiler.compile(definition.sections, definition.sectionsOrder),
        )
    }
}
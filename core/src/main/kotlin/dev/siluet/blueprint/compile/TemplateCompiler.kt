package dev.siluet.blueprint.compile

import dev.siluet.blueprint.Template
import dev.siluet.blueprint.definition.TemplateDefinition

object TemplateCompiler {
    fun compile(definition: TemplateDefinition): Template {
        val sectionsOrder = definition.sectionsOrder.ifEmpty {
            definition.sections.keys.toList()
        }

        val sections = sectionsOrder.map { sectionKey ->
            val sectionDef = definition.sections[sectionKey]!!

            SectionCompiler.compile(sectionDef)
        }

        return Template(
            layout = definition.layout,
            sections = sections
        )
    }
}
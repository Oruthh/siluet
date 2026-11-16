package dev.siluet.blueprint.compile

import dev.siluet.blueprint.Block
import dev.siluet.blueprint.Section
import dev.siluet.blueprint.definition.SectionDefinition
import dev.siluet.exception.internal.SectionsCompilationException
import kotlin.collections.map

internal object SectionsCompiler {
    fun compile(
        sections: Map<String, SectionDefinition>,
        order: List<String>
    ): List<Section> {
        val orderedSectionsDefinitions = orderResolver(sections, order, "sectionsOrder")

        val resolvedSections = orderedSectionsDefinitions.map { (sectionKey, sectionDef) ->
            val orderedBlocksDefinitions = orderResolver(sectionDef.blocks, sectionDef.blocksOrder, "blocksOrder of section '$sectionKey'")

            val blocks = orderedBlocksDefinitions.map { (_, blockDef) ->
                Block(
                    name = blockDef.name,
                    properties = blockDef.properties.map { (propName, propValue) ->
                        propName to PropertyValueCompiler.compile(propValue)
                    }.toMap()
                )
            }

            Section(
                name = sectionDef.name,
                properties = sectionDef.properties.map { (propName, propValue) ->
                    propName to PropertyValueCompiler.compile(propValue)
                }.toMap(),
                blocks = blocks
            )
        }

        return resolvedSections
    }

    private fun <T> orderResolver(
        entries: Map<String, T>,
        order: List<String>,
        describe: String,
    ): List<Pair<String, T>> {
        val unknown = order.filter { it !in entries }
        if (unknown.isNotEmpty()) throw SectionsCompilationException(
            "${unknown.joinToString { "'$it'" }} listed in $describe ${if (unknown.size == 1) "is" else "are"} not defined. " +
                    "Defined: ${entries.keys.joinToString { "'$it'" }}."
        )

        val duplicates = order.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) throw SectionsCompilationException(
            "${duplicates.joinToString { "'$it'" }} listed more than once in $describe."
        )

        return (order + entries.keys.filter { it !in order }).map { it to entries.getValue(it) }
    }
}
package dev.siluet.blueprint.compile

import dev.siluet.blueprint.PropertyValue
import dev.siluet.blueprint.Section
import dev.siluet.blueprint.definition.SectionDefinition
import kotlin.collections.ifEmpty
import kotlin.collections.map

object SectionCompiler {
    fun compile(definition: SectionDefinition): Section{
        val blocksOrder = definition.blocksOrder.ifEmpty {
            definition.blocks.keys.toList()
        }

        val blocks = blocksOrder.map { blockKey ->
            val blockDef = definition.blocks[blockKey]!!

            BlockCompiler.compile(blockDef)
        }

        return Section(
            name = definition.name,
            properties = definition.properties.map { (propName, propValue) ->
                propName to object : PropertyValue {

                }
            }.toMap(),
            blocks = blocks
        )
    }
}
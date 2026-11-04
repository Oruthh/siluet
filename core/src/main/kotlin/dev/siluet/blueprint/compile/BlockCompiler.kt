package dev.siluet.blueprint.compile

import dev.siluet.blueprint.Block
import dev.siluet.blueprint.PropertyValue
import dev.siluet.blueprint.definition.BlockDefinition

internal object BlockCompiler {
    fun compile(definition: BlockDefinition): Block {
        return Block(
            name = definition.name,
            properties = definition.properties.map { (propName, propValue) ->
                propName to object : PropertyValue {

                }
            }.toMap()
        )
    }
}
package dev.siluet.blueprint.compile

import dev.siluet.blueprint.property.BooleanPropertyValue
import dev.siluet.blueprint.property.DoublePropertyValue
import dev.siluet.blueprint.property.IntPropertyValue
import dev.siluet.blueprint.property.LongPropertyValue
import dev.siluet.blueprint.property.PropertyValue
import dev.siluet.blueprint.property.StringPropertyValue
import dev.siluet.blueprint.property.TranslationPropertyValue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal object PropertyValueCompiler {
    fun compile(value: JsonPrimitive): PropertyValue<*> {
        if(value.isString && value.content.startsWith("t:")) {
            return TranslationPropertyValue(value = value.content.substring(2))
        } else {
            value.intOrNull?.let {
                return IntPropertyValue(value = it)
            }
            value.longOrNull?.let {
                return LongPropertyValue(value = it)
            }
            value.doubleOrNull?.let {
                return DoublePropertyValue(value = it)
            }
            value.booleanOrNull?.let {
                return BooleanPropertyValue(value = it)
            }
        }

        return StringPropertyValue(value = value.content)
    }
}
package dev.siluet.locale

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object LocaleParser {
    fun parse(jsonString: String, json: Json = Json): Map<String, String> {
        val rootElement = json.parseToJsonElement(jsonString)
        val result = mutableMapOf<String, String>()
        buildFlatMap(rootElement, prefix = "", result)
        return result
    }

    private fun buildFlatMap(
        element: JsonElement,
        prefix: String,
        target: MutableMap<String, String>
    ) {
        when (element) {
            is JsonObject -> {
                for ((key, child) in element) {
                    val newPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
                    buildFlatMap(child, newPrefix, target)
                }
            }
            is JsonPrimitive -> {
                target[prefix] = element.content
            }
            else -> {

            }
        }
    }
}
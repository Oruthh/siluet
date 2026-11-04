package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext

internal class LowerCaseFilter : Filter{
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any {
        if(value is String) {
            return value.lowercase()
        }
        throw IllegalArgumentException(
            "Filter expects a String input, but received: ${value?.let { it::class.simpleName } ?: "null"}"
        )
    }
}
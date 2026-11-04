package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext

internal class UpperCaseFilter : Filter{
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any {
        if(value is String) {
            return value.uppercase()
        }
        throw IllegalArgumentException(
            "Filter expects a String input, but received: ${value?.let { it::class.simpleName } ?: "null"}"
        )
    }
}
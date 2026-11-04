package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.html.SafeHTML
import dev.siluet.render.RenderContext

internal class RawHTMLFilter : Filter {
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any {
        return when (value) {
            null -> SafeHTML("")
            is SafeHTML -> value
            else -> SafeHTML(value.toString())
        }
    }
}
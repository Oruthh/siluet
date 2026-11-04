package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext

internal class DefaultFilter: Filter {
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any? {
        if(value == null) {
            return args[0]
        }
        return value
    }
}
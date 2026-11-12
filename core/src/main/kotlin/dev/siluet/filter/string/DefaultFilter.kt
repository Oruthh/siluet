package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext

internal class DefaultFilter: Filter {
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any? {
        if (args.isEmpty()) {
            throw IllegalArgumentException("Filter 'default' requires a fallback value argument.")
        }

        if(value == null || (value is CharSequence && value.isEmpty())) {
            return args[0]
        }
        return value
    }
}
package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext

internal class TruncateFilter : Filter {
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any {
        if (args.isEmpty()) {
            throw IllegalArgumentException("Filter requires a 'length' argument.")
        }

        val lengthArg = args[0]
        val length = (lengthArg as? Number)?.toInt()
            ?: throw IllegalArgumentException(
                "Expected 'length' argument to be an integer, but got: ${lengthArg?.let { it::class.simpleName } ?: "null"}"
            )

        if (length < 0) {
            throw IllegalArgumentException("'length' argument must be non-negative, but was: $length")
        }

        if (value !is String) {
            throw IllegalArgumentException(
                "Filter expects a String input, but received: ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }

        return value.take(length)
    }
}
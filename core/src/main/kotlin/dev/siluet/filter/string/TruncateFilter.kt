package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext

internal class TruncateFilter : Filter {
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any {
        if (args.isEmpty()) {
            throw IllegalArgumentException("requires a 'length' argument, e.g. truncate(20)")
        }

        val lengthArg = args[0]
        val length = (lengthArg as? Number)?.toInt()
            ?: throw IllegalArgumentException(
                "'length' argument must be a whole number, but received ${lengthArg?.let { it::class.simpleName } ?: "null"}"
            )

        if (length < 0) {
            throw IllegalArgumentException("'length' argument must not be negative, but was $length")
        }

        if (value !is String) {
            throw IllegalArgumentException(
                "expects a String value, but received ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }

        return value.take(length)
    }
}
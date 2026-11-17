package dev.siluet.filter.string

import dev.siluet.filter.Filter
import dev.siluet.locale.LocaleRegistry
import dev.siluet.render.RenderContext

internal class TranslationFilter(
    private val localeRegistry: LocaleRegistry,
): Filter {
    override fun apply(
        value: Any?,
        args: List<Any?>,
        renderContext: RenderContext
    ): Any {
        if (value !is String) {
            throw IllegalArgumentException(
                "Filter 't' expects a String input, but received: ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }

        return localeRegistry.getTranslation(renderContext.locale, value)
    }
}
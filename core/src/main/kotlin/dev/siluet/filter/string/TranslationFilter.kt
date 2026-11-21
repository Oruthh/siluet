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
                "expects a String translation key, but received ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }

        return localeRegistry.getTranslation(renderContext.locale, value)
    }
}
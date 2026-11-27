package dev.siluet.filter.string

import dev.siluet.exception.internal.MissingTranslationException
import dev.siluet.filter.Filter
import dev.siluet.locale.LocaleRegistry
import dev.siluet.render.RenderContext

internal class TranslationFilter: Filter {
    override fun apply(
        value: Any?,
        args: List<Any?>,
        renderContext: RenderContext
    ): Any {
        val registry = renderContext.localeRegistry
            ?: throw IllegalArgumentException("translations are not available in this context")

        if (value !is String) {
            throw IllegalArgumentException(
                "expects a String translation key, but received ${value?.let { it::class.simpleName } ?: "null"}"
            )
        }

        return try{
            registry.getTranslation(renderContext.locale, value)
        } catch (e: MissingTranslationException) {
            throw IllegalArgumentException(
                "missing translation '${e.key}' for locale '${e.locale.toLanguageTag()}'"
            )
        }
    }
}
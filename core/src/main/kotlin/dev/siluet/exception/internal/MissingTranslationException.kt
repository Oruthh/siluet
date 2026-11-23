package dev.siluet.exception.internal

import java.util.Locale

internal class MissingTranslationException(
    val key: String,
    val locale: Locale
) : RuntimeException("Missing translation '$key' for locale '${locale.toLanguageTag()}'")
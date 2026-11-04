package dev.siluet.locale

import java.util.Locale

internal class LocaleNotFoundException(locale: Locale) : RuntimeException("Locale ${locale.language} not found")
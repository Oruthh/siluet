package dev.siluet.locale

import java.util.Locale

class LocaleNotFoundException(locale: Locale) : RuntimeException("Locale ${locale.language} not found")
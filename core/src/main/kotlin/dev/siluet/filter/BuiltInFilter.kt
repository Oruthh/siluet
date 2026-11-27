package dev.siluet.filter

import dev.siluet.filter.date.DateFilter
import dev.siluet.filter.string.CapitalizeFilter
import dev.siluet.filter.string.DefaultFilter
import dev.siluet.filter.string.LowerCaseFilter
import dev.siluet.filter.string.RawHTMLFilter
import dev.siluet.filter.string.TranslationFilter
import dev.siluet.filter.string.TruncateFilter
import dev.siluet.filter.string.UpperCaseFilter
import dev.siluet.locale.LocaleRegistry

internal enum class BuiltInFilter(val filterName: String, private val create: () -> Filter) {
    RAW_HTML("rawHTML", { RawHTMLFilter() }),
    DEFAULT("default", { DefaultFilter() }),
    LOWERCASE("lowercase", { LowerCaseFilter() }),
    UPPERCASE("uppercase", { UpperCaseFilter() }),
    CAPITALIZE("capitalize", { CapitalizeFilter() }),
    TRUNCATE("truncate", { TruncateFilter() }),
    TRANSLATE("t", { TranslationFilter() }),
    DATE("date", { DateFilter() });

    companion object {
        val names: Set<String> = entries.map { it.filterName }.toSet()
        fun createAll(): Map<String, Filter> =
            entries.associate { it.filterName to it.create() }
    }
}
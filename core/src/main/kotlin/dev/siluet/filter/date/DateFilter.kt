package dev.siluet.filter.date

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext
import java.time.format.DateTimeFormatter

internal class DateFilter : Filter {
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any? {
        if(value == null) {
            return ""
        }

        val zdt = DateConverter.toZonedDateTime(value, renderContext.zoneId)
            ?: throw IllegalArgumentException("Cannot parse '${value}' as a valid date/time")

        val pattern = args.firstOrNull()?.toString() ?: "yyyy-MM-dd HH:mm"

        val formatter = when (pattern) {
            "short" -> DateTimeFormatter.ofPattern("dd.MM.yyyy", renderContext.locale)
            "iso" -> DateTimeFormatter.ISO_OFFSET_DATE_TIME
            else -> DateTimeFormatter.ofPattern(pattern, renderContext.locale)
        }

        return formatter.format(zdt)
    }
}
package dev.siluet.filter.date

import dev.siluet.filter.Filter
import dev.siluet.render.RenderContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class DateFilter : Filter {
    override fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any? {
        if(value == null) {
            return ""
        }

        val zdt = DateConverter.toZonedDateTime(value, renderContext.zoneId)
            ?: throw IllegalArgumentException(
                "cannot read '$value' (${value::class.simpleName}) as a date or time; " +
                    "expected a java.time value, epoch seconds/millis or an ISO-8601 string"
            )

        val pattern = args.firstOrNull()?.toString() ?: "yyyy-MM-dd HH:mm"

        val formatter = when (pattern) {
            "short"  -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            "medium" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            "long"   -> DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            "full"   -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            "shortDateTime" -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            "mediumDateTime" -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            "longDateTime" -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
            "fullDateTime" -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            "iso"           -> DateTimeFormatter.ISO_OFFSET_DATE_TIME
            else -> try {
                DateTimeFormatter.ofPattern(pattern)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("invalid date pattern '$pattern': ${e.message}", e)
            }
        }.withLocale(renderContext.locale)

        return formatter.format(zdt)
    }
}
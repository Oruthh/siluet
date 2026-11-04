package dev.siluet.filter.date

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

internal object DateConverter {
    fun toZonedDateTime(value: Any?, zoneId: ZoneId): ZonedDateTime? {
        return when (value) {
            null -> null
            is ZonedDateTime -> value.withZoneSameInstant(zoneId)
            is Instant -> value.atZone(zoneId)
            is LocalDateTime -> value.atZone(zoneId)
            is LocalDate -> value.atStartOfDay(zoneId)
            is Number -> {
                val epoch = value.toLong()
                val instant = if (epoch > 100_000_000_000L) {
                    Instant.ofEpochMilli(epoch)
                } else {
                    Instant.ofEpochSecond(epoch)
                }
                instant.atZone(zoneId)
            }
            is CharSequence -> {
                val str = value.toString().trim()
                if(str.isEmpty()) {
                    return null
                }
                try {
                    Instant.parse(str).atZone(zoneId)
                } catch (e: DateTimeParseException) {
                    try {
                        LocalDateTime.parse(str).atZone(zoneId)
                    } catch (e: DateTimeParseException) {
                        try {
                            LocalDate.parse(str).atStartOfDay(zoneId)
                        } catch (e: DateTimeParseException) {
                            null
                        }
                    }
                }
            }
            else -> null
        }
    }
}
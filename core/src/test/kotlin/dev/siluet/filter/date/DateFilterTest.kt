package dev.siluet.filter.date

import dev.siluet.render.RenderContext
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests for the `date` filter, focused on locale-aware format styles (issue #14).
 *
 * Expected values come from the CLDR data shipped with JDK 21 (the project toolchain).
 * Localized formats can change between JDK versions, e.g. since JDK 20 English time formats
 * use a narrow no-break space (U+202F) before "AM"/"PM".
 */
class DateFilterTest {

    private companion object {
        val US: Locale = Locale.US
        val POLISH: Locale = Locale.of("pl")
        const val NNBSP = ' '
        val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

        /** Tuesday, 10 November 2026, 13:30 UTC. */
        val DATE_TIME: ZonedDateTime = ZonedDateTime.of(2026, 11, 10, 13, 30, 0, 0, ZoneOffset.UTC)
    }

    private val filter = DateFilter()

    private fun format(
        value: Any?,
        locale: Locale,
        vararg args: Any?,
        zoneId: ZoneId = ZoneOffset.UTC,
    ): Any? = filter.apply(value, args.toList(), RenderContext(locale = locale, zoneId = zoneId))

    // --- Locale-aware styles (issue #14) -------------------------------------------------------

    @Test
    fun `short style follows the render locale`() {
        assertEquals("11/10/26", format(DATE_TIME, US, "short"))
        assertEquals("10.11.2026", format(DATE_TIME, POLISH, "short"))
    }

    @Test
    fun `medium style follows the render locale`() {
        assertEquals("Nov 10, 2026", format(DATE_TIME, US, "medium"))
        assertEquals("10 lis 2026", format(DATE_TIME, POLISH, "medium"))
    }

    @Test
    fun `long style follows the render locale`() {
        assertEquals("November 10, 2026", format(DATE_TIME, US, "long"))
        assertEquals("10 listopada 2026", format(DATE_TIME, POLISH, "long"))
    }

    @Test
    fun `full style follows the render locale`() {
        assertEquals("Tuesday, November 10, 2026", format(DATE_TIME, US, "full"))
        assertEquals("wtorek, 10 listopada 2026", format(DATE_TIME, POLISH, "full"))
    }

    @Test
    fun `shortDateTime style follows the render locale`() {
        assertEquals("11/10/26, 1:30${NNBSP}PM", format(DATE_TIME, US, "shortDateTime"))
        assertEquals("10.11.2026, 13:30", format(DATE_TIME, POLISH, "shortDateTime"))
    }

    @Test
    fun `mediumDateTime style follows the render locale`() {
        assertEquals("Nov 10, 2026, 1:30:00${NNBSP}PM", format(DATE_TIME, US, "mediumDateTime"))
        assertEquals("10 lis 2026, 13:30:00", format(DATE_TIME, POLISH, "mediumDateTime"))
    }

    @Test
    fun `longDateTime style follows the render locale and includes the time zone`() {
        assertEquals("November 10, 2026, 2:30:00${NNBSP}PM CET", format(DATE_TIME, US, "longDateTime", zoneId = WARSAW))
        assertEquals("10 listopada 2026 14:30:00 CET", format(DATE_TIME, POLISH, "longDateTime", zoneId = WARSAW))
    }

    @Test
    fun `fullDateTime style follows the render locale and includes the time zone name`() {
        assertEquals(
            "Tuesday, November 10, 2026, 2:30:00${NNBSP}PM Central European Standard Time",
            format(DATE_TIME, US, "fullDateTime", zoneId = WARSAW)
        )
        assertEquals(
            "wtorek, 10 listopada 2026 14:30:00 czas środkowoeuropejski standardowy",
            format(DATE_TIME, POLISH, "fullDateTime", zoneId = WARSAW)
        )
    }

    @Test
    fun `date time styles with time zone work for values without zone`() {
        // LocalDateTime has no zone; the filter must attach the render zone before formatting,
        // otherwise long/full date-time styles fail with "Unable to extract ZoneId".
        val local = LocalDateTime.of(2026, 11, 10, 14, 30)

        assertEquals("10 listopada 2026 14:30:00 CET", format(local, POLISH, "longDateTime", zoneId = WARSAW))
    }

    @Test
    fun `same value renders differently for different locales`() {
        assertNotEquals(format(DATE_TIME, US, "short"), format(DATE_TIME, POLISH, "short"))
    }

    @Test
    fun `styles respect the render time zone`() {
        // 23:30 UTC is already the next day in Warsaw (UTC+1 in November).
        val lateEvening = ZonedDateTime.of(2026, 11, 10, 23, 30, 0, 0, ZoneOffset.UTC)

        assertEquals("10.11.2026", format(lateEvening, POLISH, "short", zoneId = ZoneOffset.UTC))
        assertEquals("11.11.2026", format(lateEvening, POLISH, "short", zoneId = ZoneId.of("Europe/Warsaw")))
        assertEquals("11.11.2026, 00:30", format(lateEvening, POLISH, "shortDateTime", zoneId = ZoneId.of("Europe/Warsaw")))
    }

    @Test
    fun `styles work for LocalDate and LocalDateTime values`() {
        assertEquals("10 listopada 2026", format(LocalDate.of(2026, 11, 10), POLISH, "long"))
        assertEquals("Tuesday, November 10, 2026", format(LocalDateTime.of(2026, 11, 10, 13, 30), US, "full"))
    }

    // --- Unchanged behaviour (regression guards) -----------------------------------------------

    @Test
    fun `iso format is unchanged`() {
        assertEquals("2026-11-10T13:30:00Z", format(DATE_TIME, US, "iso"))
        assertEquals("2026-11-10T13:30:00Z", format(DATE_TIME, POLISH, "iso"))
    }

    @Test
    fun `custom patterns are unchanged and use the render locale`() {
        assertEquals("10 November 2026", format(DATE_TIME, US, "dd MMMM yyyy"))
        assertEquals("10 listopada 2026", format(DATE_TIME, POLISH, "dd MMMM yyyy"))
        assertEquals("2026/11/10 13:30", format(DATE_TIME, POLISH, "yyyy/MM/dd HH:mm"))
    }

    @Test
    fun `default format without argument is unchanged and locale independent`() {
        assertEquals("2026-11-10 13:30", format(DATE_TIME, US))
        assertEquals("2026-11-10 13:30", format(DATE_TIME, POLISH))
    }

    @Test
    fun `null value renders as empty string`() {
        assertEquals("", format(null, US, "short"))
    }
}
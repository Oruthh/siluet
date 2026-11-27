package dev.siluet.render

import dev.siluet.locale.LocaleRegistry
import java.time.ZoneId
import java.util.Locale

/**
 * Immutable runtime execution context passed to template evaluations, filters, and directives.
 *
 * Encapsulates regional and temporal configurations—such as the target [locale] for translations,
 * currency, and number formatting, alongside the [zoneId] for timezone-aware date and time representations.
 *
 * Instances are typically configured per render invocation via [RenderContext.Builder]:
 * ```kotlin
 * val html = theme.render("orderSummary", order) {
 *     locale = Locale.GERMANY
 *     zoneId = ZoneId.of("Europe/Berlin")
 * }
 * ```
 *
 * @property locale The active [Locale] used for localization dictionaries and regional formatting.
 * @property zoneId The active [ZoneId] used to normalize temporal values and timestamps during formatting.
 */
class RenderContext internal constructor(
    val locale: Locale,
    val zoneId: ZoneId,
    internal val localeRegistry: LocaleRegistry ?= null
) {
    /**
     * Builder for constructing customized [RenderContext] instances.
     *
     * Exposes mutable properties pre-populated with system defaults ([Locale.getDefault] and [ZoneId.systemDefault]),
     * designed to be modified via trailing configuration blocks in render calls.
     */
    class Builder {
        /**
        * The target locale for rendering.
        *
        * Defaults to [Locale.getDefault].
        */
        var locale: Locale = Locale.getDefault()

        /**
         * The target time zone for temporal formatting operations.
         *
         * Defaults to [ZoneId.systemDefault].
         */
        var zoneId: ZoneId = ZoneId.systemDefault()

        /**
         * Constructs a new immutable [RenderContext] using the configured settings.
         *
         * @return A populated [RenderContext] instance.
         */
        fun build(): RenderContext {
            return RenderContext(
                locale = locale,
                zoneId = zoneId,
                localeRegistry = null
            )
        }

        internal fun build(localeRegistry: LocaleRegistry): RenderContext {
            return RenderContext(
                locale = locale,
                zoneId = zoneId,
                localeRegistry = localeRegistry
            )
        }
    }
}

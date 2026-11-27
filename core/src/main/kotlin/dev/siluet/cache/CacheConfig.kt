package dev.siluet.cache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * DSL configuration of the theme caches.
 *
 * Every [dev.siluet.theme.SiluetTheme] keeps compiled layouts, sections and components, parsed templates and
 * section groups, and loaded translations in its own cache. Each of them counts as one entry. When a limit is
 * reached, the least recently used entries are evicted first; an evicted entry is loaded and compiled again
 * the next time it is needed, so eviction never makes a render fail. Limits are enabled by default.
 *
 * ### Example:
 * ```kotlin
 * val siluet = Siluet {
 *     cache {
 *         maxEntriesPerTheme = 1_000
 *         maxTotalEntries = 100_000
 *         evictIdleThemesAfter = 1.hours
 *     }
 * }
 * ```
 *
 * All values are validated when the engine is built; an invalid value throws [IllegalArgumentException].
 */
class CacheConfig {
    /**
     * Maximum number of cached entries per theme. A theme can lower it for itself with
     * [dev.siluet.theme.SiluetThemeConfig.maxCacheEntries].
     *
     * Must be greater than `0` and not greater than [maxTotalEntries]. Defaults to `500`.
     */
    var maxEntriesPerTheme: Int = 500

    /**
     * Maximum number of cached entries across all themes of the engine, which keeps memory use predictable
     * when there are many themes. When it is exceeded, entries of the themes that rendered least recently are
     * evicted first, down to 90% of the limit.
     *
     * The limit is soft: under concurrent rendering the total may exceed it briefly before entries are evicted.
     *
     * Must be greater than `0`. Defaults to `50_000`.
     */
    var maxTotalEntries: Int = 50_000

    /**
     * A theme that has not rendered for this long releases its whole cache. The theme stays valid and loads
     * its assets again on the next render. Idle themes are checked while other themes render; there is no
     * background thread.
     *
     * Use [Duration.INFINITE] to disable idle eviction. Must be positive. Defaults to `30` minutes.
     */
    var evictIdleThemesAfter: Duration = 30.minutes

    internal fun validate() {
        require(maxEntriesPerTheme > 0) { "maxEntriesPerTheme must be greater than 0" }
        require(maxTotalEntries > 0) { "maxTotalEntries must be greater than 0" }
        require(maxEntriesPerTheme <= maxTotalEntries) {
            "maxEntriesPerTheme ($maxEntriesPerTheme) must not exceed maxTotalEntries ($maxTotalEntries)"
        }
        require(evictIdleThemesAfter.isPositive()) { "evictIdleThemesAfter must be positive" }
    }
}
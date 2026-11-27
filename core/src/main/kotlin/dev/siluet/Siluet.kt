package dev.siluet

import dev.siluet.theme.SiluetTheme
import dev.siluet.theme.SiluetThemeConfig

/**
 * Primary entry point for the Siluet templating engine.
 *
 * The engine holds everything that is shared by all themes: filters, directives, plugins, the security policy,
 * render limits and cache limits. Pages are rendered through a [SiluetTheme], which brings its own asset loader,
 * compiled views and translations. One engine can serve a single theme or many themes, e.g. one per tenant.
 *
 * Instances are created with the DSL factory method provided by the companion object:
 * ```kotlin
 * val siluet = Siluet {
 *     strictMode = true
 * }
 *
 * val theme = siluet.theme {
 *     assetLoader = ResourceSiluetAssetLoader("theme")
 * }
 *
 * val html = theme.render("home", model) { locale = Locale.ENGLISH }
 * ```
 *
 * A [Siluet] instance and its themes are thread-safe. Create them once and share them across requests.
 *
 * @see SiluetConfig
 * @see SiluetTheme
 */
interface Siluet {
    /**
     * Creates a new [SiluetTheme] that renders with this engine.
     *
     * Every call creates a separate theme with its own, initially empty cache, so create a theme once
     * (e.g. at startup or when a tenant is first used) and reuse it for all renders.
     *
     * ### Example:
     * ```kotlin
     * val theme = siluet.theme {
     *     assetLoader = TenantDatabaseAssetLoader(tenantId)
     *     maxCacheEntries = 200
     * }
     * ```
     *
     * @param themeConfig Configuration lambda applied to [SiluetThemeConfig].
     * @return A new theme using this engine's filters, directives and settings.
     * @throws IllegalArgumentException if no [SiluetThemeConfig.assetLoader] is set, or if
     * [SiluetThemeConfig.maxCacheEntries] is not greater than `0` or exceeds
     * [dev.siluet.cache.CacheConfig.maxTotalEntries].
     */
    fun theme(themeConfig: SiluetThemeConfig.() -> Unit): SiluetTheme

    companion object {
        /**
         * Creates and configures a new [Siluet] engine instance using a DSL builder.
         *
         * ### Example:
         * ```kotlin
         * val siluet = Siluet {
         *     maxLoopIterations = 5_000
         *     cache {
         *         maxTotalEntries = 20_000
         *     }
         * }
         * ```
         *
         * @param block Configuration lambda applied to [SiluetConfig].
         * @return A configured [Siluet] instance backed by [DefaultSiluet].
         * @throws IllegalArgumentException if the configuration is invalid, e.g. cache limits that are not
         * greater than `0` (see [dev.siluet.cache.CacheConfig]).
         */
        operator fun invoke(block: SiluetConfig.() -> Unit = {}): Siluet {
            val config = SiluetConfig().apply(block)
            return DefaultSiluet(config)
        }
    }
}
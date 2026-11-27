package dev.siluet.theme

import dev.siluet.loader.SiluetAssetLoader

/**
 * DSL configuration for a [SiluetTheme], applied in [dev.siluet.Siluet.theme].
 *
 * ### Example:
 * ```kotlin
 * val theme = siluet.theme {
 *     assetLoader = TenantDatabaseAssetLoader(tenantId)
 *     maxCacheEntries = 200
 * }
 * ```
 */
class SiluetThemeConfig {
    /**
     * The loader responsible for resolving templates, layouts, sections, components,
     * and locale files from the filesystem, classpath resources, or a remote store.
     *
     * Required; creating a theme without it throws [IllegalArgumentException].
     */
    var assetLoader: SiluetAssetLoader? = null

    /**
     * Maximum number of cached entries for this theme. Defaults to the engine's
     * [dev.siluet.cache.CacheConfig.maxEntriesPerTheme].
     *
     * Must be greater than `0` and not greater than [dev.siluet.cache.CacheConfig.maxTotalEntries].
     */
    var maxCacheEntries: Int? = null
}

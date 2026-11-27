package dev.siluet.support

import dev.siluet.cache.CacheBudget
import dev.siluet.cache.ThemeCache
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.loader.SiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetTemplateLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.render.SiluetRenderer
import dev.siluet.theme.SiluetTheme
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Builds a [SiluetTheme] around [renderer] for tests that drive the renderer directly, without the `Siluet { }` engine.
 * All loaders share one [ThemeCache], as in a real theme; idle eviction is disabled.
 */
internal fun testTheme(
    renderer: SiluetRenderer,
    assetLoader: SiluetAssetLoader = ResourceSiluetAssetLoader(),
    maxEntries: Int = 1000,
): SiluetTheme {
    val timeSource = TimeSource.Monotonic
    val budget = CacheBudget(
        maxTotalEntries = maxEntries,
        evictIdleAfter = Duration.INFINITE,
        timeSource = timeSource,
    )
    val cache = ThemeCache(maxSize = maxEntries, sizeListener = budget, createdAt = timeSource.markNow())
    val json = Json { ignoreUnknownKeys = true }

    return SiluetTheme(
        viewsLoader = SiluetViewsLoader(assetLoader = assetLoader, cache = cache),
        sectionsGroupsLoader = SiluetSectionsGroupLoader(assetLoader = assetLoader, json = json, cache = cache),
        templateLoader = SiluetTemplateLoader(assetLoader = assetLoader, json = json, cache = cache),
        translations = LocaleRegistry(assetLoader = assetLoader, strictMode = false, cache = cache),
        renderer = renderer,
        cache = cache,
        cacheBudget = budget,
    )
}

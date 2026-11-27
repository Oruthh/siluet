package dev.siluet

import dev.siluet.cache.CacheBudget
import dev.siluet.cache.ThemeCache
import kotlinx.serialization.json.Json
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.render.SiluetRenderer
import dev.siluet.loader.SiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetTemplateLoader
import dev.siluet.filter.BuiltInFilter
import dev.siluet.locale.LocaleRegistry
import dev.siluet.reflect.CachedReflectionPropertyAccessor
import dev.siluet.theme.SiluetTheme
import dev.siluet.theme.SiluetThemeConfig
import kotlin.time.TimeSource

class DefaultSiluet(
    private val config: SiluetConfig
) : Siluet {
    private val json = Json { ignoreUnknownKeys = true }

    private val timeSource: TimeSource.WithComparableMarks = config.timeSource

    private val renderer: SiluetRenderer

    private val cacheBudget: CacheBudget

    init {
        config.cacheConfig.validate()

        val defaultFilters = BuiltInFilter.createAll()

        val propertyAccessor = CachedReflectionPropertyAccessor(
            securityPolicy = config.securityPolicy,
        )

        renderer = SiluetRenderer(
            maxLoopIterations = config.maxLoopIterations,
            maxRenderDepth = config.maxRenderDepth,
            strictMode = config.strictMode,
            accessor = propertyAccessor,
            filters = config.filters + defaultFilters,
            directives = config.directives
        )
        
        cacheBudget = CacheBudget(
            maxTotalEntries = config.cacheConfig.maxTotalEntries,
            evictIdleAfter = config.cacheConfig.evictIdleThemesAfter,
            timeSource = timeSource,
        )
    }

    override fun theme(themeConfig: SiluetThemeConfig.() -> Unit): SiluetTheme {
        val themeConfig = SiluetThemeConfig().apply(themeConfig)

        val assetLoader: SiluetAssetLoader = themeConfig.assetLoader
            ?: throw IllegalArgumentException("SiluetAssetLoader must be configured in SiluetThemeConfig.")

        val maxEntries = themeConfig.maxCacheEntries
            ?.also { limit ->
                require(limit > 0) { "maxCacheEntries must be greater than 0" }
                require(limit <= config.cacheConfig.maxTotalEntries) {
                    "maxCacheEntries ($limit) must not exceed maxTotalEntries (${config.cacheConfig.maxTotalEntries})"
                }
            }
            ?: config.cacheConfig.maxEntriesPerTheme

        val themeCache = ThemeCache(
            maxSize = maxEntries,
            sizeListener = cacheBudget,
            createdAt = timeSource.markNow()
        )

        val theme = SiluetTheme(
            viewsLoader = SiluetViewsLoader(
                cache = themeCache,
                assetLoader = assetLoader,
            ),
            sectionsGroupsLoader = SiluetSectionsGroupLoader(
                assetLoader = assetLoader,
                json = json,
                cache = themeCache,
            ),
            templateLoader = SiluetTemplateLoader(
                assetLoader = assetLoader,
                json = json,
                cache = themeCache,
            ),
            translations = LocaleRegistry(
                assetLoader = assetLoader,
                strictMode = config.strictMode,
                cache = themeCache
            ),
            renderer = renderer,
            cache = themeCache,
            cacheBudget = cacheBudget
        )

        cacheBudget.register(theme, themeCache)

        return theme
    }
}

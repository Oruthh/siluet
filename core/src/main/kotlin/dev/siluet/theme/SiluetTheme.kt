package dev.siluet.theme

import dev.siluet.blueprint.SectionsGroup
import dev.siluet.cache.CacheBudget
import dev.siluet.cache.CacheKey
import dev.siluet.cache.SiluetCacheStats
import dev.siluet.cache.ThemeCache
import dev.siluet.exception.SiluetAssetNotFoundException
import dev.siluet.exception.SiluetBlueprintException
import dev.siluet.exception.SiluetViewException
import dev.siluet.exception.internal.LexerException
import dev.siluet.exception.internal.ParseException
import dev.siluet.exception.internal.RenderException
import dev.siluet.exception.internal.TemplateLoadingException
import dev.siluet.exception.internal.TemplateNotFoundException
import dev.siluet.loader.SiluetAssetType
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetTemplateLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.render.RenderContext
import dev.siluet.render.SiluetRenderer
import dev.siluet.view.SiluetSectionView
import dev.siluet.view.SiluetView
import java.util.Locale

/**
 * A theme: a set of templates, layouts, sections, components and translations, read through one
 * [dev.siluet.loader.SiluetAssetLoader] and rendered with the filters, directives and settings of the
 * [dev.siluet.Siluet] engine it was created from.
 *
 * Each theme keeps its own cache of compiled views, parsed templates and section groups, and translations.
 * Themes do not see each other's assets or translations, which makes it possible to serve many tenants,
 * each with its own theme, from one engine. Cache limits and idle eviction are configured on the engine with
 * [dev.siluet.SiluetConfig.cache].
 *
 * Create themes with [dev.siluet.Siluet.theme]. A theme is thread-safe; create it once and reuse it for all
 * renders, because every new theme starts with an empty cache.
 *
 * ### Example:
 * ```kotlin
 * val theme = siluet.theme {
 *     assetLoader = ResourceSiluetAssetLoader("theme")
 * }
 *
 * val html = theme.render("home", model) {
 *     locale = Locale.ENGLISH
 *     zoneId = ZoneId.of("Europe/London")
 * }
 * ```
 */
class SiluetTheme internal constructor(
    private val viewsLoader: SiluetViewsLoader,
    private val sectionsGroupsLoader: SiluetSectionsGroupLoader,
    private val templateLoader: SiluetTemplateLoader,
    internal val translations: LocaleRegistry,
    private val renderer: SiluetRenderer,
    private val cache: ThemeCache,
    private val cacheBudget: CacheBudget,
) {
    internal fun getComponentView(name: String): SiluetView {
        return viewsLoader.getOrCompileComponentView(name)
    }

    internal fun getSectionView(name: String): SiluetSectionView {
        return viewsLoader.getOrCompileSectionView(name)
    }

    internal fun getSectionsGroup(name: String): SectionsGroup {
        return sectionsGroupsLoader.getOrCompileSectionsGroup(name)
    }

    /**
     * Renders a fully orchestrated template defined by a JSON configuration.
     *
     * Resolves the template configuration from `templates/<templateName>.json` through this theme's asset loader,
     * loads the associated layout, binds the configured sections and renders the result. Assets are compiled on
     * first use and served from this theme's cache afterwards.
     *
     * ### Example:
     * ```kotlin
     * val html = theme.render("main", model) {
     *     locale = Locale.ENGLISH
     *     zoneId = ZoneId.of("Europe/London")
     * }
     * ```
     *
     * @param templateName The identifier of the template configuration (without the `.json` extension).
     * @param model The data model exposed to layouts and sections. Defaults to [Unit] when no data is required.
     * @param contextBuilder Optional DSL builder configuring render-time context parameters such as locale or timezone.
     * @return The fully rendered HTML string.
     * @throws SiluetViewException If rendering fails at a position in an HTML view.
     * @throws SiluetBlueprintException If the template or a section group it uses is invalid.
     * @throws SiluetAssetNotFoundException If the template does not exist.
     */
    fun render(templateName: String, model: Any = Unit, contextBuilder: RenderContext.Builder.() -> Unit = {}): String {
        cacheBudget.beforeRender(cache)
        return try{
            val template = templateLoader.getOrCompileTemplate(templateName)
            val layoutView = viewsLoader.getOrCompileLayoutView(template.layout)
            renderer.render(layoutView.ast, model, template, this, contextBuilder)
        } catch (e: RenderException) {
            throw SiluetViewException(e.message!!, e.sourceLocation)
        } catch (e: ParseException) {
            throw SiluetViewException(e.message!!, e.sourceLocation)
        } catch (e: LexerException) {
            throw SiluetViewException(e.message!!, e.sourceLocation)
        } catch (e: TemplateLoadingException) {
            throw SiluetBlueprintException(e.message!!, e.sourceName)
        } catch (_: TemplateNotFoundException) {
            throw SiluetAssetNotFoundException("template", templateName)
        }
    }

    /**
     * Removes everything this theme has cached, so the next render loads and compiles every asset again.
     *
     * Only this theme is affected. Use it after many assets of the theme have changed; for a single asset
     * prefer [invalidate]. The counters in [cacheStats] are kept.
     */
    fun clearCache() {
        cache.invalidateAll()
    }

    /**
     * Removes a single asset from this theme's cache, so the next render loads and compiles it again,
     * e.g. after the asset was changed in a theme editor or in the database.
     *
     * Only this theme is affected; other themes keep their cached copy. Invalidating an asset that is not cached
     * does nothing. For [SiluetAssetType.LOCALE] the [name] is a language tag (e.g. `"en"`) and the translations of
     * every cached locale with that language are removed.
     *
     * ### Example:
     * ```kotlin
     * theme.invalidate(SiluetAssetType.SECTION, "featuredProducts")
     * ```
     *
     * @param assetType The kind of asset to remove.
     * @param name The asset name as used in templates, e.g. `"featuredProducts"` for `sections/featuredProducts.html`.
     */
    fun invalidate(assetType: SiluetAssetType, name: String) {
        when (assetType) {
            SiluetAssetType.TEMPLATE -> cache.invalidate(CacheKey.Template(name))
            SiluetAssetType.LAYOUT -> cache.invalidate(CacheKey.Layout(name))
            SiluetAssetType.SECTION -> cache.invalidate(CacheKey.Section(name))
            SiluetAssetType.COMPONENT -> cache.invalidate(CacheKey.Component(name))
            SiluetAssetType.SECTIONS_GROUP -> cache.invalidate(CacheKey.SectionsGroup(name))
            SiluetAssetType.LOCALE -> {
                val language = Locale.forLanguageTag(name).language
                cache.invalidateIf { it is CacheKey.Translations && it.locale.language == language }
            }
        }
    }

    /**
     * Returns a snapshot of this theme's cache statistics: hits, misses, evictions and the current number of entries.
     *
     * Counters accumulate for the lifetime of the theme; [clearCache] and [invalidate] reset only the size.
     */
    fun cacheStats(): SiluetCacheStats {
        return cache.stats()
    }
}
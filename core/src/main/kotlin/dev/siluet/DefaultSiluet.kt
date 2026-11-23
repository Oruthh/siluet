package dev.siluet

import dev.siluet.cache.InMemorySiluetCache
import kotlinx.serialization.json.Json
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.render.SiluetRenderer
import dev.siluet.exception.internal.LexerException
import dev.siluet.loader.SiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetTemplateLoader
import dev.siluet.exception.internal.ParseException
import dev.siluet.render.RenderContext
import dev.siluet.exception.internal.RenderException
import dev.siluet.exception.SiluetAssetNotFoundException
import dev.siluet.exception.SiluetBlueprintException
import dev.siluet.exception.SiluetViewException
import dev.siluet.exception.internal.TemplateLoadingException
import dev.siluet.exception.internal.TemplateNotFoundException
import dev.siluet.filter.BuiltInFilter
import dev.siluet.locale.LocaleRegistry
import dev.siluet.reflect.CachedReflectionPropertyAccessor

class DefaultSiluet(
    config: SiluetConfig
) : Siluet {
    private val assetLoader: SiluetAssetLoader = config.assetLoader
        ?: throw IllegalStateException("SiluetAssetLoader must be configured in SiluetConfig.")

    private val json = Json { ignoreUnknownKeys = true }

    private val viewsLoader: SiluetViewsLoader
    private val sectionsGroupsLoader: SiluetSectionsGroupLoader
    private val templateLoader: SiluetTemplateLoader
    private val localeRegistry = LocaleRegistry(assetLoader)

    private val renderer: SiluetRenderer

    init {
        val defaultFilters = BuiltInFilter.createAll(localeRegistry)

        viewsLoader = SiluetViewsLoader(
            cache = InMemorySiluetCache(config.cacheConfig.maxViews),
            assetLoader = assetLoader,
        )

        sectionsGroupsLoader = SiluetSectionsGroupLoader(
            assetLoader = assetLoader,
            json = json,
            cache = InMemorySiluetCache(config.cacheConfig.maxSectionsGroups),
        )

        templateLoader = SiluetTemplateLoader(
            assetLoader = assetLoader,
            json = json,
            cache = InMemorySiluetCache(config.cacheConfig.maxTemplates),
        )

        val propertyAccessor = CachedReflectionPropertyAccessor(
            securityPolicy = config.securityPolicy,
        )

        renderer = SiluetRenderer(
            maxLoopIterations = config.maxLoopIterations,
            maxRenderDepth = config.maxRenderDepth,
            accessor = propertyAccessor,
            filters = config.filters + defaultFilters,
            directives = config.directives,
            viewsLoader = viewsLoader,
            sectionsGroupsLoader = sectionsGroupsLoader,
            localeRegistry = localeRegistry
        )
    }

    override fun render(templateName: String, model: Any, contextBuilder: RenderContext.Builder.() -> Unit): String {
        return try{
            val template = templateLoader.getOrCompileTemplate(templateName)
            val layoutView = viewsLoader.getOrCompileLayoutView(template.layout)
            renderer.render(layoutView.ast, model, template, contextBuilder)
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
}

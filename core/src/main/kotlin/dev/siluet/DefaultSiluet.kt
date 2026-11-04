package dev.siluet

import dev.siluet.exception.SiluetException
import kotlinx.serialization.json.Json
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.render.SiluetRenderer
import dev.siluet.filter.string.CapitalizeFilter
import dev.siluet.filter.string.DefaultFilter
import dev.siluet.filter.string.LowerCaseFilter
import dev.siluet.filter.string.RawHTMLFilter
import dev.siluet.filter.string.TruncateFilter
import dev.siluet.filter.string.UpperCaseFilter
import dev.siluet.exception.LexerException
import dev.siluet.loader.SiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetTemplateLoader
import dev.siluet.exception.ParseException
import dev.siluet.filter.date.DateFilter
import dev.siluet.render.RenderContext
import dev.siluet.exception.RenderException
import dev.siluet.locale.LocaleRegistry

class DefaultSiluet(
    private val builder: SiluetBuilder
) : Siluet {
    private val assetLoader: SiluetAssetLoader = builder.assetLoader
        ?: throw IllegalStateException("SiluetAssetLoader must be configured in SiluetConfig.")

    private val json = Json { ignoreUnknownKeys = true }

    private val viewsLoader = SiluetViewsLoader(builder.viewCache, assetLoader)
    private val sectionsGroupsLoader = SiluetSectionsGroupLoader(assetLoader, json)
    private val templateLoader = SiluetTemplateLoader(assetLoader, json)
    private val localeRegistry = LocaleRegistry(assetLoader)

    private val renderer: SiluetRenderer

    init {
        val defaultFilters = mapOf(
            "rawHTML" to RawHTMLFilter(),
            "default" to DefaultFilter(),
            "lowercase" to LowerCaseFilter(),
            "uppercase" to UpperCaseFilter(),
            "capitalize" to CapitalizeFilter(),
            "truncate" to TruncateFilter(),

            "date" to DateFilter(),
        )

        renderer = SiluetRenderer(
            maxLoopIterations = builder.maxLoopIterations,
            accessor = builder.resolvePropertyAccessor(),
            filters = builder.filters.toMap() + defaultFilters,
            directives = builder.directives.toMap(),
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
            throw SiluetException(e.message!!, e.sourceLocation)
        } catch (e: ParseException) {
            throw SiluetException(e.message!!, e.sourceLocation)
        } catch (e: LexerException) {
            throw SiluetException(e.message!!, e.sourceLocation)
        }
    }
}

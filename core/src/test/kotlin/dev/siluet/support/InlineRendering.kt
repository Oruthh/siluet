package dev.siluet.support

import dev.siluet.blueprint.Template
import dev.siluet.cache.InMemorySiluetCache
import dev.siluet.filter.Filter
import dev.siluet.lexer.SiluetLexer
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.parser.SiluetParser
import dev.siluet.directive.Directive
import dev.siluet.render.SiluetRenderer
import kotlinx.serialization.json.Json

/**
 * Runs a template string through lexer → parser → renderer without a theme.
 * Built-in filters are not registered; pass the ones a test needs.
 */
internal fun renderInline(
    template: String,
    model: Any? = Unit,
    filters: Map<String, Filter> = emptyMap(),
    directives: Map<String, Directive> = emptyMap(),
    strictMode: Boolean = false,
): String {
    val tokens = SiluetLexer(template, "views/inline.html").tokenize()
    val ast = SiluetParser(tokens).parseAsNodeList()
    val renderer = SiluetRenderer(
        maxLoopIterations = 100,
        maxRenderDepth = 32,
        strictMode = strictMode,
        filters = filters,
        directives = directives,
        viewsLoader = SiluetViewsLoader(
            cache = InMemorySiluetCache(),
            assetLoader = ResourceSiluetAssetLoader(),
        ),
        sectionsGroupsLoader = SiluetSectionsGroupLoader(
            assetLoader = ResourceSiluetAssetLoader(),
            json = Json { ignoreUnknownKeys = true },
            cache = InMemorySiluetCache(),
        ),
        localeRegistry = LocaleRegistry(
            assetLoader = ResourceSiluetAssetLoader(),
            strictMode = false
        ),
    )
    return renderer.render(ast, model, Template(layout = "", sections = emptyList()))
}

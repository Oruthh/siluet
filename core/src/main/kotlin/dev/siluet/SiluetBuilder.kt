package dev.siluet

import dev.siluet.cache.InMemoryViewCache
import dev.siluet.cache.ViewCache
import dev.siluet.loader.SiluetAssetLoader
import dev.siluet.reflect.CachedReflectionPropertyAccessor
import dev.siluet.reflect.SecurityPolicy
import dev.siluet.render.Directive
import dev.siluet.filter.Filter
import dev.siluet.plugin.SiluetPlugin
import dev.siluet.reflect.PropertyAccessor
import dev.siluet.render.RenderContext

/**
 * DSL builder for configuring and instantiating a [Siluet] template engine.
 *
 * Provides options for asset resolution, view caching, reflection security,
 * iteration guardrails, custom directives, filters, and third-party plugins.
 *
 * ### Example:
 * ```kotlin
 * val siluet = Siluet {
 *     assetLoader = ResourceSiluetAssetLoader("theme")
 *     maxLoopIterations = 5_000
 *
 *     filter("reverse") { value, _, _ ->
 *         value?.toString()?.reversed()
 *     }
 *
 *     install(MoneyPlugin) {
 *         defaultCurrency = "EUR"
 *     }
 * }
 * ```
 */
class SiluetBuilder {
    /**
     * The loader responsible for resolving templates, layouts, sections, components,
     * and locale files from the filesystem, classpath resources, or a remote store.
     */
    var assetLoader: SiluetAssetLoader? = null

    /**
     * Cache provider used to store and retrieve compiled view templates and AST structures.
     *
     * Defaults to an [InMemoryViewCache].
     */
    var viewCache: ViewCache = InMemoryViewCache()

    /**
     * Security policy regulating reflection, member access, and forbidden types
     * during expression evaluation on context data models.
     *
     * Defaults to [SecurityPolicy.Default].
     */
    var securityPolicy: SecurityPolicy = SecurityPolicy.Default

    /**
     * Custom property accessor used to resolve object properties and method calls in expressions.
     *
     * If set to `null`, Siluet defaults to a [CachedReflectionPropertyAccessor] initialized
     * with the current [securityPolicy].
     */
    var propertyAccessor: PropertyAccessor? = null

    /**
     * Maximum allowed iterations for loop directives (e.g., `@for`) to prevent infinite
     * loops or denial-of-service conditions during template rendering.
     *
     * Defaults to `1000`.
     */
    var maxLoopIterations: Int = 1_000

    /**
     * Mutable map of registered template filters, keyed by their filter identifier.
     */
    val filters: MutableMap<String, Filter> = mutableMapOf()

    /**
     * Mutable map of registered custom directives, keyed by their directive name.
     */
    val directives: MutableMap<String, Directive> = mutableMapOf()

    /**
     * Registers a custom [Filter] instance under the specified [name].
     *
     * Registered filters can be applied in templates using the pipe operator:
     * `{{ value | name(args) }}`.
     *
     * @param name The filter name used within template expressions.
     * @param filter The filter implementation instance.
     */
    fun filter(name: String, filter: Filter) {
        filters[name] = filter
    }

    /**
     * Registers an inline filter using a functional lambda expression.
     *
     * ### Example:
     * ```kotlin
     * filter("prefix") { value, args, _ ->
     *     val prefix = args.firstOrNull()?.toString() ?: ""
     *     "$prefix$value"
     * }
     * ```
     *
     * @param name The filter name used within template expressions.
     * @param filterFn Lambda receiving the piped value, argument list args, and current [RenderContext].
     */
    fun filter(name: String, filterFn: (value: Any?, args: List<Any?>, renderContext: RenderContext) -> Any?) {
        filters[name] = Filter { value, args, renderContext -> filterFn(value, args, renderContext) }
    }

    /**
     * Registers a custom [Directive] under the specified [name].
     *
     * Custom directives can be invoked within templates via the `@` prefix:
     * `@name(args) { ... }`.
     *
     * @param name The directive name without the leading `@`.
     * @param directive The directive implementation instance.
     */
    fun directive(name: String, directive: Directive) {
        directives[name] = directive
    }

    /**
     * Installs and configures a [SiluetPlugin].
     *
     * Plugins encapsulate bundles of filters, directives
     *
     * ### Example:
     * ```kotlin
     * install(MoneyPlugin) {
     *     defaultCurrency = "USD"
     * }
     * ```
     *
     * @param TConfig Configuration type expected by the plugin.
     * @param plugin The plugin to install into this builder.
     * @param configure Optional configuration lambda applied to the plugin's config object.
     * @return This [SiluetBuilder] instance for method chaining.
     */
    fun <TConfig : Any> install(
        plugin: SiluetPlugin<TConfig>,
        configure: TConfig.() -> Unit = {}
    ): SiluetBuilder = apply {
        plugin.install(this, configure)
    }

    /**
     * Resolves the active [PropertyAccessor], falling back to a reflection-based accessor
     * guarded by [securityPolicy] if no custom accessor was provided.
     */
    internal fun resolvePropertyAccessor(): PropertyAccessor {
        return propertyAccessor ?: CachedReflectionPropertyAccessor(securityPolicy)
    }
}
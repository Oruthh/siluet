package dev.siluet

import dev.siluet.cache.CacheConfig
import dev.siluet.directive.BuiltInDirective
import dev.siluet.loader.SiluetAssetLoader
import dev.siluet.reflect.SecurityPolicy
import dev.siluet.directive.Directive
import dev.siluet.filter.BuiltInFilter
import dev.siluet.filter.Filter
import dev.siluet.plugin.SiluetPlugin
import dev.siluet.render.RenderContext

/**
 * DSL configuration for instantiating a [Siluet] template engine.
 *
 * Provides options for asset resolution, cache limits, reflection security,
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
class SiluetConfig {
    private class Registration<T>(
        val value: T,
        val plugin: Class<SiluetPlugin<*>>?
    )

    private val installedPlugins = mutableSetOf<Class<SiluetPlugin<*>>>()

    private var installingPlugin: SiluetPlugin<*>? = null

    /**
     * Configuration defining capacity limits for internal engine caches.
     *
     * Configurable via the [cache] DSL block.
     */
    var cacheConfig: CacheConfig = CacheConfig()
        private set

    /**
     * Configures internal cache capacities using a [CacheConfig] DSL block.
     *
     * ### Example:
     * ```kotlin
     * cache {
     *     maxViews = 2_000
     *     maxTemplates = 100
     * }
     * ```
     *
     * @param block Configuration lambda applied to a new [CacheConfig] instance.
     */
    fun cache(block: CacheConfig.() -> Unit) {
        cacheConfig = CacheConfig().apply(block)
    }

    /**
     * The loader responsible for resolving templates, layouts, sections, components,
     * and locale files from the filesystem, classpath resources, or a remote store.
     */
    var assetLoader: SiluetAssetLoader? = null

    /**
     * Security policy regulating reflection, member access, and forbidden types
     * during expression evaluation on context data models.
     *
     * Templates can read every public getter of the objects in the model (including computed
     * and interface properties); the policy is checked before each getter is invoked.
     * Prefer passing dedicated view models to templates and use [dev.siluet.reflect.DefaultSecurityPolicy]
     * with `blockedMembers` / `blockedPackages` to deny additional members.
     *
     * Defaults to [SecurityPolicy.Default].
     */
    var securityPolicy: SecurityPolicy = SecurityPolicy.Default

    /**
     * Maximum allowed iterations for loop directives (e.g., `@for`) to prevent infinite
     * loops or denial-of-service conditions during template rendering.
     *
     * The limit applies to each loop separately, not to the whole render. It is checked while
     * iterating, so it also protects against infinite or very large lazy sources (e.g., [Sequence])
     * without traversing them in advance. Exceeding the limit aborts the entire render with a
     * [dev.siluet.exception.SiluetViewException]; no partial output is returned.
     *
     * Defaults to `1000`.
     */
    var maxLoopIterations: Int = 1_000

    /**
     * Maximum allowed nesting depth of views (components and sections) during a single render.
     *
     * Protects against infinite recursion, such as a component that includes itself directly
     * or through other components (`a` → `b` → `a`), or a section that renders `@sections`
     * containing itself, which would otherwise end in a [StackOverflowError].
     *
     * Every component rendered with `@component` and every section rendered with `@sections`
     * adds one level; the layout itself does not count. For example, a section rendered from
     * a layout has depth `1`, and a component rendered inside that section has depth `2`.
     * Views rendered side by side do not add up, so only the depth of nesting is limited,
     * not the total number of views on a page.
     *
     * Legitimate recursion, such as rendering a category tree, works up to this limit.
     * Exceeding it aborts the entire render with a [dev.siluet.exception.SiluetViewException]
     * that points to the directive call in the template and shows the repeating chain of views,
     * e.g. `/theme/components/ping.html → /theme/components/pong.html → /theme/components/ping.html`;
     * no partial output is returned.
     *
     * Must be greater than `0`. Defaults to `32`.
     */
    var maxRenderDepth: Int = 32

    /**
     * Fails the render on template expressions that point at data that is not there.
     *
     * When enabled, rendering is aborted with a [dev.siluet.exception.SiluetViewException] that points
     * at the expression in the view when a template:
     * - uses a variable that does not exist (`{{ prodcut }}`),
     * - reads a property or map key that does not exist (`{{ product.prise }}`),
     * - reads a property of `null` (`{{ user.address.city }}` while `user.address` is `null`),
     * - uses a translation key that is missing in the locale file, through the `t` filter or a
     *   `t:` property in a JSON blueprint.
     *
     * A variable or property that exists but holds `null` is allowed: it prints nothing, the
     * `default` filter replaces it and `@if` treats it as false, so optional data can be guarded with
     * `@if(user.address) { ... }`. The `default` filter does not hide a variable that does not exist.
     *
     * When disabled, such expressions resolve to `null` and a missing translation prints its key.
     * Useful in development and tests to catch typos and missing data early.
     *
     * Defaults to `false`.
     */
    var strictMode: Boolean = false

    private val registeredFilters = mutableMapOf<String, Registration<Filter>>()

    /**
     * Read-only view of the custom filters registered so far, keyed by filter name.
     *
     * Register filters with [filter]; built-in filters are not included.
     */
    val filters: Map<String, Filter>
        get() = registeredFilters.mapValues { it.value.value }

    private val registeredDirectives = mutableMapOf<String, Registration<Directive>>()

    /**
     * Read-only view of the custom directives registered so far, keyed by directive name.
     *
     * Register directives with [directive].
     */
    val directives: Map<String, Directive>
        get() = registeredDirectives.mapValues { it.value.value }

    /**
     * Registers a custom [Filter] instance under the specified [name].
     *
     * Registered filters can be applied in templates using the pipe operator:
     * `{{ value | name(args) }}`.
     *
     * Each name can be registered only once, by the application or by one [SiluetPlugin].
     *
     * @param name The filter name used within template expressions.
     * @param filter The filter implementation instance.
     * @throws IllegalArgumentException if [name] is the name of a built-in filter (such as `date`
     * or `t`), or if a filter with this name is already registered. The message names who
     * registered it first, e.g. `Filter 'money' is already registered by MoneyPlugin.`
     */
    fun filter(name: String, filter: Filter) {
        if(BuiltInFilter.names.contains(name)) {
            throw IllegalArgumentException("Filter '$name' is built in and cannot be replaced.")
        }
        if(registeredFilters.containsKey(name)){
            val registeredFilter = registeredFilters[name]
            val registrationOwner = registeredFilter?.plugin?.simpleName?.ifEmpty { "an anonymous plugin" } ?: "the application"
            throw IllegalArgumentException("Filter '$name' is already registered by $registrationOwner.")
        }
        registeredFilters[name] = Registration(filter, installingPlugin?.javaClass)
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
     * @throws IllegalArgumentException if [name] is a built-in filter name or is already registered;
     * see the [Filter] overload of this function.
     */
    fun filter(name: String, filterFn: (value: Any?, args: List<Any?>, renderContext: RenderContext) -> Any?) {
        filter(name, Filter { value, args, renderContext -> filterFn(value, args, renderContext) })
    }

    /**
     * Registers a custom [Directive] under the specified [name].
     *
     * Custom directives can be invoked within templates via the `@` prefix:
     * `@name(args) { ... }`.
     *
     * Each name can be registered only once, by the application or by one [SiluetPlugin].
     * A filter and a directive may share a name.
     *
     * @param name The directive name without the leading `@`.
     * @param directive The directive implementation instance.
     * @throws IllegalArgumentException if [name] is the name of a built-in directive (`if`, `else`,
     * `for`, `component`, `sections`, `paginate`, `schema`), or if a directive with this name is
     * already registered. The message names who registered it first.
     */
    fun directive(name: String, directive: Directive) {
        if(BuiltInDirective.keywords.contains(name)) {
            throw IllegalArgumentException("Directive '$name' is built in and cannot be replaced.")
        }
        if(registeredDirectives.containsKey(name)){
            val registeredDirective = registeredDirectives[name]
            val registrationOwner = registeredDirective?.plugin?.simpleName?.ifEmpty { "an anonymous plugin" } ?: "the application"
            throw IllegalArgumentException("Directive '$name' is already registered by $registrationOwner.")
        }
        registeredDirectives[name] = Registration(directive, installingPlugin?.javaClass)
    }

    /**
     * Installs and configures a [SiluetPlugin].
     *
     * Plugins encapsulate bundles of filters and directives. Filters and directives registered
     * while the plugin is installed belong to that plugin, which is named in the error when
     * another registration uses the same name. A plugin may install other plugins.
     *
     * ### Example:
     * ```kotlin
     * install(MoneyPlugin) {
     *     defaultCurrency = "USD"
     * }
     * ```
     *
     * @param TConfig Configuration type expected by the plugin.
     * @param plugin The plugin to install into this configuration.
     * @param configure Optional configuration lambda applied to the plugin's config object.
     * @return This [SiluetConfig] instance for method chaining.
     * @throws IllegalArgumentException if a plugin of the same class is already installed, or if
     * the plugin registers a built-in or already registered filter or directive name.
     */
    fun <TConfig : Any> install(
        plugin: SiluetPlugin<TConfig>,
        configure: TConfig.() -> Unit = {}
    ): SiluetConfig = apply {
        if(installedPlugins.contains(plugin.javaClass)){
            throw IllegalArgumentException("${plugin.javaClass.simpleName.ifEmpty { "An anonymous plugin" }} is already installed.")
        }
        val previous = installingPlugin
        installingPlugin = plugin

        installedPlugins.add(plugin.javaClass)

        try {
            plugin.install(this, configure)
        } finally {
            installingPlugin = previous
        }
    }
}
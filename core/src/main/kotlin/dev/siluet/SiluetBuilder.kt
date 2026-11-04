package dev.siluet

import dev.siluet.cache.InMemoryViewCache
import dev.siluet.cache.ViewCache
import dev.siluet.loader.SiluetAssetLoader
import dev.siluet.reflect.CachedReflectionPropertyAccessor
import dev.siluet.reflect.SecurityPolicy
import dev.siluet.render.Directive
import dev.siluet.filter.Filter
import dev.siluet.plugin.SiluetPlugin
import dev.siluet.render.PropertyAccessor
import dev.siluet.render.RenderContext

class SiluetBuilder {
    var assetLoader: SiluetAssetLoader? = null
    var viewCache: ViewCache = InMemoryViewCache()
    var securityPolicy: SecurityPolicy = SecurityPolicy.Default
    var propertyAccessor: PropertyAccessor? = null
    var maxLoopIterations: Int = 1_000

    val filters: MutableMap<String, Filter> = mutableMapOf()
    val directives: MutableMap<String, Directive> = mutableMapOf()


    fun filter(name: String, filter: Filter) {
        filters[name] = filter
    }

    fun filter(name: String, filterFn: (value: Any?, args: List<Any?>, renderContext: RenderContext) -> Any?) {
        filters[name] = Filter { value, args, renderContext -> filterFn(value, args, renderContext) }
    }

    fun directive(name: String, directive: Directive) {
        directives[name] = directive
    }

    fun <TConfig : Any> install(
        plugin: SiluetPlugin<TConfig>,
        configure: TConfig.() -> Unit = {}
    ): SiluetBuilder = apply {
        plugin.install(this, configure)
    }

    internal fun resolvePropertyAccessor(): PropertyAccessor {
        return propertyAccessor ?: CachedReflectionPropertyAccessor(securityPolicy)
    }
}
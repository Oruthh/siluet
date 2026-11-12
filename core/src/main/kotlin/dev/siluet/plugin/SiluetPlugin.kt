package dev.siluet.plugin

import dev.siluet.SiluetConfig

/**
 * Extension contract for modular plugins extending the Siluet templating engine.
 *
 * Plugins bundle cohesive sets of template filters, custom directives, specialized
 * asset loaders, or runtime security policies into reusable modules.
 *
 * ### Example Implementation:
 * ```kotlin
 * class MoneyConfig {
 *     var defaultCurrency: String = "USD"
 * }
 *
 * object MoneyPlugin : SiluetPlugin<MoneyConfig> {
 *     override fun install(builder: SiluetConfig, configure: MoneyConfig.() -> Unit) {
 *         val config = MoneyConfig().apply(configure)
 *
 *         builder.filter("currency") { value, args, _ ->
 *             val currency = args.firstOrNull()?.toString() ?: config.defaultCurrency
 *             "$value$currency"
 *         }
 *     }
 * }
 * ```
 *
 * Installed into an engine instance via [SiluetConfig.install]:
 * ```kotlin
 * val siluet = Siluet {
 *     install(MoneyPlugin) {
 *         defaultCurrency = "EUR"
 *     }
 * }
 * ```
 *
 * @param TConfig The configuration object type exposed to consumer setup blocks.
 */
interface SiluetPlugin<TConfig: Any> {
    /**
     * Installs plugin capabilities and components into the provided engine configuration.
     *
     * @param builder The engine configuration being populated.
     * @param configure Optional configuration lambda applied to the plugin's [TConfig] instance.
     */
    fun install(builder: SiluetConfig, configure: TConfig.() -> Unit = {})
}
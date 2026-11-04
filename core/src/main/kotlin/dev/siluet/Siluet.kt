package dev.siluet

import dev.siluet.render.RenderContext

/**
 * Primary entry point for the Siluet templating engine.
 *
 * Provides methods for rendering structured templates
 *
 * Instances can be constructed using the DSL factory method provided by the companion object:
 * ```kotlin
 * val siluet = Siluet {
 *     assetLoader = ResourceSiluetAssetLoader("theme")
 * }
 * ```
 * @see SiluetBuilder
 */
interface Siluet {
    /**
     * Renders a fully orchestrated template defined by a JSON configuration.
     *
     * This method resolves the template configuration file from `templates/<templateName>.json`,
     * loads the associated layout, binds configured dynamic sections, and renders the result.
     *
     * ### Example:
     * ```kotlin
     * val html = siluet.render("main", model) {
     *     locale = Locale.ENGLISH
     *     zoneId = ZoneId.of("Europe/London")
     * }
     * ```
     *
     * @param templateName The identifier of the template configuration (without the `.json` extension).
     * @param model The data model exposed to the template context. Defaults to [Unit] when no data is required.
     * @param contextBuilder Optional DSL builder configuring render-time context parameters such as locale or timezone.
     * @return The fully rendered HTML string.
     */
    fun render(templateName: String, model: Any = Unit, contextBuilder: RenderContext.Builder.() -> Unit = {}): String

    companion object {
        /**
         * Creates and configures a new [Siluet] engine instance using a DSL builder.
         *
         * ### Example:
         * ```kotlin
         * val siluet = Siluet {
         *     assetLoader = ResourceSiluetAssetLoader("theme")
         * }
         * ```
         *
         * @param block Configuration lambda applied to [SiluetBuilder].
         * @return A configured [Siluet] instance backed by [DefaultSiluet].
         */
        operator fun invoke(block: SiluetBuilder.() -> Unit = {}): Siluet {
            val config = SiluetBuilder().apply(block)
            return DefaultSiluet(config)
        }
    }
}
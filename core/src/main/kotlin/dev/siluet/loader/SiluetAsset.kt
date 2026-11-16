package dev.siluet.loader

/**
 * An asset returned by a [SiluetAssetLoader]: the content of a template, section group, layout,
 * section, component or locale, together with the name of its source.
 *
 * ### Source names
 * [sourceName] is chosen by the loader and should say where the asset really comes from, e.g.
 * `/theme/sections/hero.html` for a classpath resource, `s3://shop-themes/acme/sections/hero.html`
 * for a remote file or `db:theme_sections#42` for a database record. Siluet shows it in error
 * messages and in [dev.siluet.SourceLocation]s.
 *
 * **It must be unique per asset.** Siluet also uses it to tell views apart when detecting infinite
 * recursion, so two different assets with the same source name are treated as the same view.
 *
 * @property sourceName Unique, human-readable identifier of the asset's origin.
 * @property content Raw content of the asset: HTML markup for views, JSON for templates,
 *   section groups and locales.
 */
data class SiluetAsset(
    val sourceName: String,
    val content: String,
)

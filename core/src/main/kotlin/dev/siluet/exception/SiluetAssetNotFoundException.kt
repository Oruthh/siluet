package dev.siluet.exception

/**
 * Thrown when an asset requested directly by the application does not exist, e.g. when
 * `theme.render("homee")` is called and the asset loader has no template named `homee`.
 *
 * Assets referenced from other assets (a component used in a view, a section group used by
 * `@sections`) are reported as [SiluetViewException] at the place where they are referenced.
 *
 * @property assetType Kind of the missing asset, e.g. `template`.
 * @property name Name of the missing asset as requested by the application.
 */
class SiluetAssetNotFoundException(val assetType: String, val name: String)
    : SiluetException("Could not find $assetType '$name'")

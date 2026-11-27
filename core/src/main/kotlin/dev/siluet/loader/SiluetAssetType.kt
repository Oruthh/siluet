package dev.siluet.loader

/**
 * Kind of theme asset resolved by a [SiluetAssetLoader] and kept in a theme's cache.
 *
 * Used with [dev.siluet.theme.SiluetTheme.invalidate] to drop a single cached entry,
 * for example after the asset was changed in the visual editor or in the database.
 */
enum class SiluetAssetType {
    /** A page definition from `templates/<name>.json`. */
    TEMPLATE,

    /** A layout view from `layouts/<name>.html`. */
    LAYOUT,

    /** A section view from `sections/<name>.html`. */
    SECTION,

    /** A component view from `components/<name>.html`. */
    COMPONENT,

    /** A sections group definition from `sections/groups/<name>.json`. */
    SECTIONS_GROUP,

    /**
     * Translations from `locales/<language>.json`. The name is a language tag such as `"en"`;
     * invalidating it drops the cached translations of every locale with that language (e.g. `en-US`, `en-GB`).
     */
    LOCALE,
}
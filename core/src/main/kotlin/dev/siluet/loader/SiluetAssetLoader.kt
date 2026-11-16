package dev.siluet.loader

import java.util.Locale

/**
 * Strategy contract for loading theme assets, templates, layouts, sections, components, and translations.
 *
 * Implementations decouple the Siluet rendering pipeline from underlying storage mechanisms,
 * allowing templates and configurations to be loaded from classpath resources, the local filesystem,
 * databases, or remote stores (e.g., S3 or a headless CMS).
 *
 * All methods return a [SiluetAsset] with the raw content (HTML markup or JSON) and a source name,
 * or `null` if the requested asset does not exist. The source name is shown in error messages and must
 * be unique per asset; see [SiluetAsset] for details.
 *
 * ### Example:
 * ```kotlin
 * class DatabaseAssetLoader(private val repository: ThemeRepository) : SiluetAssetLoader {
 *     override fun loadSection(name: String): SiluetAsset? =
 *         repository.findSection(name)?.let { row ->
 *             SiluetAsset(sourceName = "db:theme_sections#${row.id}", content = row.html)
 *         }
 *
 *     // ... other methods
 * }
 * ```
 *
 * @see ResourceSiluetAssetLoader
 */
interface SiluetAssetLoader {
    /**
     * Loads a page orchestration configuration by its template [name].
     *
     * Typically maps to a JSON configuration file (e.g., `templates/<name>.json`) defining
     * the layout, sections, and block properties for a specific route or view.
     *
     * @param name The template identifier without file extension.
     * @return The template asset with its JSON configuration, or `null` if not found.
     */
    fun loadTemplate(name: String): SiluetAsset?

    /**
     * Loads a shared section group configuration by its [name].
     *
     * Typically maps to a reusable JSON configuration file (e.g., `sections/groups/<name>.json`)
     * representing shared UI regions such as headers or footers across multiple templates.
     *
     * @param name The section group identifier without file extension.
     * @return The section group asset with its JSON configuration, or `null` if not found.
     */
    fun loadSectionsGroup(name: String): SiluetAsset?

    /**
     * Loads an outer layout template by its [name].
     *
     * Typically maps to an HTML template file (e.g., `layouts/<name>.html`) containing
     * the document frame and placeholders where sections or views are injected.
     *
     * @param name The layout identifier without file extension.
     * @return The layout asset with its HTML markup, or `null` if not found.
     */
    fun loadLayout(name: String): SiluetAsset?

    /**
     * Loads an individual section template by its [name].
     *
     * Typically maps to an HTML template file (e.g., `sections/<name>.html`) representing
     * a modular, configurable building block within a page layout.
     *
     * @param name The section identifier without file extension.
     * @return The section asset with its HTML markup, or `null` if not found.
     */
    fun loadSection(name: String): SiluetAsset?

    /**
     * Loads a reusable component template by its [name].
     *
     * Typically maps to an HTML template file (e.g., `components/<name>.html`) representing
     * lightweight snippets or partials invoked directly by other templates or sections.
     *
     * @param name The component identifier without file extension.
     * @return The component asset with its HTML markup, or `null` if not found.
     */
    fun loadComponent(name: String): SiluetAsset?

    /**
     * Loads localization translation data for the given [locale].
     *
     * Typically maps to a JSON dictionary (e.g., `locales/<language>.json`) containing
     * key-value translation mappings used by translation directives (such as `@t`).
     *
     * @param locale The requested [Locale].
     * @return The locale asset with its JSON translations, or `null` if no translations exist for this locale.
     */
    fun loadLocale(locale: Locale): SiluetAsset?
}
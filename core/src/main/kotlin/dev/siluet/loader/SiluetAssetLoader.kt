package dev.siluet.loader

import java.util.Locale

/**
 * Strategy contract for loading theme assets, templates, layouts, sections, components, and translations.
 *
 * Implementations decouple the Siluet rendering pipeline from underlying storage mechanisms,
 * allowing templates and configurations to be loaded from classpath resources, the local filesystem,
 * databases, or remote stores (e.g., S3 or a headless CMS).
 *
 * All methods return raw strings representing the file contents (HTML markup or JSON strings)
 * or `null` if the requested asset does not exist.
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
     * @return The raw JSON configuration string, or `null` if not found.
     */
    fun loadTemplate(name: String): String?

    /**
     * Loads a shared section group configuration by its [name].
     *
     * Typically maps to a reusable JSON configuration file (e.g., `sections/groups/<name>.json`)
     * representing shared UI regions such as headers or footers across multiple templates.
     *
     * @param name The section group identifier without file extension.
     * @return The raw JSON group configuration string, or `null` if not found.
     */
    fun loadSectionsGroup(name: String): String?

    /**
     * Loads an outer layout template by its [name].
     *
     * Typically maps to an HTML template file (e.g., `layouts/<name>.html`) containing
     * the document frame and placeholders where sections or views are injected.
     *
     * @param name The layout identifier without file extension.
     * @return The raw layout template string, or `null` if not found.
     */
    fun loadLayout(name: String): String?

    /**
     * Loads an individual section template by its [name].
     *
     * Typically maps to an HTML template file (e.g., `sections/<name>.html`) representing
     * a modular, configurable building block within a page layout.
     *
     * @param name The section identifier without file extension.
     * @return The raw section template string, or `null` if not found.
     */
    fun loadSection(name: String): String?

    /**
     * Loads a reusable component template by its [name].
     *
     * Typically maps to an HTML template file (e.g., `components/<name>.html`) representing
     * lightweight snippets or partials invoked directly by other templates or sections.
     *
     * @param name The component identifier without file extension.
     * @return The raw component template string, or `null` if not found.
     */
    fun loadComponent(name: String): String?

    /**
     * Loads localization translation data for the given [locale].
     *
     * Typically maps to a JSON dictionary (e.g., `locales/<language>.json`) containing
     * key-value translation mappings used by translation directives (such as `@t`).
     *
     * @param locale The requested [Locale].
     * @return The raw JSON localization string, or `null` if no translations exist for this locale.
     */
    fun loadLocale(locale: Locale): String?
}
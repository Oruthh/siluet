package dev.siluet.loader

import java.util.Locale

/**
 * Classpath resource-based implementation of [SiluetAssetLoader].
 *
 * Resolves template JSON definitions, HTML layouts, sections, components, section groups,
 * and localization files packaged within JARs or the application's classpath resources
 * (e.g., `src/main/resources`).
 *
 * Relative paths are resolved against an optional root [prefix]:
 * ```kotlin
 * // Resolves resources from "src/main/resources/theme/..."
 * val loader = ResourceSiluetAssetLoader("theme")
 *
 * // Loads "theme/templates/index.json"
 * val templateJson = loader.loadTemplate("index")
 * ```
 *
 * @param prefix Root directory path prepended to all asset paths (e.g., `"theme"` or `"templates/v1"`).
 *               Defaults to an empty string (root of classpath).
 */
class ResourceSiluetAssetLoader(
    private val prefix: String = "",
) : SiluetAssetLoader {
    /**
     * Loads a page orchestration configuration from `templates/<name>.json`.
     *
     * @param name The template identifier (without the `.json` extension).
     * @return Raw JSON configuration string, or `null` if the resource does not exist.
     */
    override fun loadTemplate(name: String): String? {
        return load("templates/$name.json")
    }

    /**
     * Loads a reusable section group configuration from `sections/groups/<name>.json`.
     *
     * @param name The section group identifier (without the `.json` extension).
     * @return Raw JSON configuration string, or `null` if the resource does not exist.
     */
    override fun loadSectionsGroup(name: String): String? {
        return load("sections/groups/$name.json")
    }

    /**
     * Loads an outer layout template from `layouts/<name>.html`.
     *
     * @param name The layout file identifier (without the `.html` extension).
     * @return Raw HTML/template markup string, or `null` if the resource does not exist.
     */
    override fun loadLayout(name: String): String? {
        return load("layouts/$name.html")
    }

    /**
     * Loads an individual section template from `sections/<name>.html`.
     *
     * @param name The section file identifier (without the `.html` extension).
     * @return Raw HTML/template markup string, or `null` if the resource does not exist.
     */
    override fun loadSection(name: String): String? {
        return load("sections/$name.html")
    }

    /**
     * Loads a reusable component template from `components/<name>.html`.
     *
     * @param name The component file identifier (without the `.html` extension).
     * @return Raw HTML/template markup string, or `null` if the resource does not exist.
     */
    override fun loadComponent(name: String): String? {
        return load("components/$name.html")
    }

    /**
     * Loads localization translation data for the specified [locale] from `locales/<language>.json`.
     *
     * Resolution uses the two-letter ISO 639-1 language code (e.g., `locales/en.json`, `locales/pl.json`).
     *
     * @param locale The target [Locale].
     * @return Raw JSON locale dictionary string, or `null` if the resource does not exist.
     */
    override fun loadLocale(locale: Locale): String? {
        return load("locales/${locale.language}.json")
    }

    /**
     * Reads a resource from the classpath by its relative [path], normalized with the configured [prefix].
     *
     * @param path The relative resource path (e.g., `"templates/index.json"`).
     * @return UTF-8 decoded file content as a [String], or `null` if the resource cannot be found on the classpath.
     */
    private fun load(path: String): String? {
        val normalized = if(prefix.isEmpty()) {
            path
        } else {
            "$prefix/$path"
        }
        val resourcePath = if (normalized.startsWith("/")) {
            normalized
        } else {
            "/$normalized"
        }
        return javaClass.getResourceAsStream(resourcePath)
            ?.bufferedReader()
            ?.use { it.readText() }
    }
}
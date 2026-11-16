package dev.siluet.support

import dev.siluet.loader.SiluetAsset
import dev.siluet.loader.SiluetAssetLoader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Mutable, in-memory [SiluetAssetLoader] for tests.
 *
 * Assets are keyed like files (`"templates/home.json"`, `"layouts/main.html"`, `"sections/groups/header.json"`)
 * and can be replaced between renders, e.g. to check that a fixed asset is picked up after a failed compilation.
 * The reported source name is `"memory:<key>"`, so tests can verify that Siluet uses the loader's source name
 * instead of building a file path itself.
 */
class InMemorySiluetAssetLoader(assets: Map<String, String> = emptyMap()) : SiluetAssetLoader {
    private val assets = ConcurrentHashMap(assets)

    operator fun set(key: String, content: String) {
        assets[key] = content
    }

    override fun loadTemplate(name: String) = load("templates/$name.json")
    override fun loadSectionsGroup(name: String) = load("sections/groups/$name.json")
    override fun loadLayout(name: String) = load("layouts/$name.html")
    override fun loadSection(name: String) = load("sections/$name.html")
    override fun loadComponent(name: String) = load("components/$name.html")
    override fun loadLocale(locale: Locale) = load("locales/${locale.language}.json")

    private fun load(key: String): SiluetAsset? =
        assets[key]?.let { SiluetAsset(sourceName = "memory:$key", content = it) }
}

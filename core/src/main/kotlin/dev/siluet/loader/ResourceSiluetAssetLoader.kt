package dev.siluet.loader

import java.util.Locale

class ResourceSiluetAssetLoader(
    private val prefix: String = "",
) : SiluetAssetLoader {
    override fun loadTemplate(name: String): String? {
        return load("templates/$name.json")
    }

    override fun loadSectionsGroup(name: String): String? {
        return load("sections/groups/$name.json")
    }

    override fun loadLayout(name: String): String? {
        return load("layouts/$name.html")
    }

    override fun loadSection(name: String): String? {
        return load("sections/$name.html")
    }

    override fun loadComponent(name: String): String? {
        return load("components/$name.html")
    }

    override fun loadLocale(locale: Locale): String? {
        return load("locales/${locale.language}.json")
    }

    fun load(path: String): String? {
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
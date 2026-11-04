package dev.siluet.loader

import dev.siluet.exception.ComponentViewNotFoundException
import dev.siluet.exception.LayoutViewNotFoundException
import dev.siluet.exception.SectionViewNotFoundException
import dev.siluet.cache.ViewCache
import dev.siluet.lexer.SiluetLexer
import dev.siluet.parser.SiluetParser
import dev.siluet.parser.SiluetView

internal class SiluetViewsLoader(
    val cache: ViewCache,
    val assetLoader: SiluetAssetLoader
) {
    fun getOrCompileSectionView(name: String): SiluetView {
        return getOrCompileHtml("sections", name) {
            assetLoader.loadSection(name)
                ?: throw SectionViewNotFoundException(name)
        }
    }

    fun getOrCompileLayoutView(name: String): SiluetView {
        return getOrCompileHtml("layouts", name) {
            assetLoader.loadLayout(name)
                ?: throw LayoutViewNotFoundException(name)
        }
    }

    fun getOrCompileComponentView(name: String): SiluetView {
        return getOrCompileHtml("components", name) {
            assetLoader.loadComponent(name)
                ?: throw ComponentViewNotFoundException(name)
        }
    }

    private fun getOrCompileHtml(type: String, name: String, loader: () -> String): SiluetView {
        cache.get("$type/$name")?.let { return it }

        val tokens = SiluetLexer(loader(), "$type/$name.html").tokenize()
        val nodes = SiluetParser(tokens).parse()
        val view = SiluetView(nodes)
        cache.put(name, view)

        return view
    }
}
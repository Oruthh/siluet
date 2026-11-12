package dev.siluet.loader

import dev.siluet.cache.SiluetCache
import dev.siluet.exception.ComponentViewNotFoundException
import dev.siluet.exception.LayoutViewNotFoundException
import dev.siluet.exception.SectionViewNotFoundException
import dev.siluet.lexer.SiluetLexer
import dev.siluet.parser.SiluetParser
import dev.siluet.view.SiluetSectionView
import dev.siluet.view.SiluetView

internal class SiluetViewsLoader(
    val cache: SiluetCache<String, SiluetView>,
    val assetLoader: SiluetAssetLoader
) {
    fun getOrCompileSectionView(name: String): SiluetSectionView {
        return getOrCompileSectionView(name) {
            assetLoader.loadSection(name)
                ?: throw SectionViewNotFoundException(name)
        }
    }

    fun getOrCompileLayoutView(name: String): SiluetView {
        return getOrCompileView("layouts", name) {
            assetLoader.loadLayout(name)
                ?: throw LayoutViewNotFoundException(name)
        }
    }

    fun getOrCompileComponentView(name: String): SiluetView {
        return getOrCompileView("components", name) {
            assetLoader.loadComponent(name)
                ?: throw ComponentViewNotFoundException(name)
        }
    }

    private fun getOrCompileView(type: String, name: String, loader: () -> String): SiluetView {
        val cacheKey = "$type/$name"
        val filePath = "$cacheKey.html"
        cache.get(cacheKey)?.let { return it }

        val tokens = SiluetLexer(loader(), filePath).tokenize()
        val parseResult = SiluetParser(tokens).parse()

        val view = SiluetView(
            ast = parseResult.ast,
            filePath = filePath
        )

        cache.put(cacheKey, view)

        return view
    }
    private fun getOrCompileSectionView(name: String, loader: () -> String): SiluetSectionView {
        val cacheKey = "sections/$name"
        val filePath = "$cacheKey.html"
        cache.get(cacheKey)?.let { return it as SiluetSectionView }

        val tokens = SiluetLexer(loader(), filePath).tokenize()
        val parseResult = SiluetParser(tokens).parse()

        val view = SiluetSectionView(
            ast = parseResult.ast,
            filePath = filePath,
            schema = parseResult.sectionSchema
        )

        cache.put(cacheKey, view)

        return view
    }
}
package dev.siluet.loader

import dev.siluet.cache.SiluetCache
import dev.siluet.exception.internal.ComponentViewNotFoundException
import dev.siluet.exception.internal.LayoutViewNotFoundException
import dev.siluet.exception.internal.SectionViewNotFoundException
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

    private fun getOrCompileView(type: String, name: String, loader: () -> SiluetAsset): SiluetView {
        val cacheKey = "$type/$name"
        cache.get(cacheKey)?.let { return it }

        val asset = loader()

        val tokens = SiluetLexer(asset.content, asset.sourceName).tokenize()
        val parseResult = SiluetParser(tokens).parse()

        val view = SiluetView(
            ast = parseResult.ast,
            sourceName = asset.sourceName
        )

        cache.put(cacheKey, view)

        return view
    }
    private fun getOrCompileSectionView(name: String, loader: () -> SiluetAsset): SiluetSectionView {
        val cacheKey = "sections/$name"
        cache.get(cacheKey)?.let { return it as SiluetSectionView }

        val asset = loader()

        val tokens = SiluetLexer(asset.content, asset.sourceName).tokenize()
        val parseResult = SiluetParser(tokens).parse()

        val view = SiluetSectionView(
            ast = parseResult.ast,
            sourceName = asset.sourceName,
            schema = parseResult.sectionSchema
        )

        cache.put(cacheKey, view)

        return view
    }
}
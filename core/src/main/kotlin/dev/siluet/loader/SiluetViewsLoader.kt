package dev.siluet.loader

import dev.siluet.cache.CacheKey
import dev.siluet.cache.ThemeCache
import dev.siluet.exception.internal.ComponentViewNotFoundException
import dev.siluet.exception.internal.LayoutViewNotFoundException
import dev.siluet.exception.internal.SectionViewNotFoundException
import dev.siluet.lexer.SiluetLexer
import dev.siluet.parser.ParseResult
import dev.siluet.parser.SiluetParser
import dev.siluet.view.SiluetSectionView
import dev.siluet.view.SiluetView

internal class SiluetViewsLoader(
    val assetLoader: SiluetAssetLoader,
    val cache: ThemeCache
) {
    fun getOrCompileLayoutView(name: String): SiluetView {
        return cache.computeIfAbsent(CacheKey.Layout(name)) {
            compileView(assetLoader.loadLayout(name) ?: throw LayoutViewNotFoundException(name))
        }
    }

    fun getOrCompileSectionView(name: String): SiluetSectionView {
        return cache.computeIfAbsent(CacheKey.Section(name)) {
            val asset = assetLoader.loadSection(name)
                ?: throw SectionViewNotFoundException(name)
            val parsed = parse(asset)

            SiluetSectionView(
                ast = parsed.ast,
                sourceName = asset.sourceName,
                schema = parsed.sectionSchema,
            )
        }
    }

    fun getOrCompileComponentView(name: String): SiluetView {
        return cache.computeIfAbsent(CacheKey.Component(name)) {
            compileView(assetLoader.loadComponent(name) ?: throw ComponentViewNotFoundException(name))
        }
    }

    private fun compileView(asset: SiluetAsset): SiluetView {
        return SiluetView(
            ast = parse(asset).ast,
            sourceName = asset.sourceName,
        )
    }

    private fun parse(asset: SiluetAsset): ParseResult {
        return SiluetParser(
            tokens = SiluetLexer(
                source = asset.content,
                filePath = asset.sourceName).tokenize()
        ).parse()
    }
}
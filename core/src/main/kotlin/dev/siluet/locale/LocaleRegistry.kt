package dev.siluet.locale

import dev.siluet.cache.CacheKey
import dev.siluet.cache.ThemeCache
import dev.siluet.exception.internal.MissingTranslationException
import dev.siluet.loader.SiluetAssetLoader
import java.util.Locale

internal class LocaleRegistry(
    private val assetLoader: SiluetAssetLoader,
    private val strictMode: Boolean,
    private val cache: ThemeCache,
) {
    fun getTranslation(locale: Locale, translationId: String): String {
        val translations = cache.computeIfAbsent(CacheKey.Translations(locale)) {
            val asset = assetLoader.loadLocale(locale)
                ?: assetLoader.loadLocale(Locale.getDefault())
                ?: throw LocaleNotFoundException(locale) //TODO implement something better

            LocaleParser.parse(asset.content)
        }

        return translations[translationId] ?: if(strictMode){
            throw MissingTranslationException(translationId, locale)
        } else {
            translationId
        }
    }
}
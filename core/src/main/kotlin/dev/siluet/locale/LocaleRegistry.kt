package dev.siluet.locale

import dev.siluet.exception.internal.MissingTranslationException
import dev.siluet.loader.SiluetAssetLoader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal class LocaleRegistry(
    private val assetLoader: SiluetAssetLoader,
    private val strictMode: Boolean,
) {
    private val translationCache = ConcurrentHashMap<Locale, Map<String, String>>()

    fun getTranslation(locale: Locale, translationId: String): String {
        val translations = translationCache.computeIfAbsent(locale) { locale ->
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
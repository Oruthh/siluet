package dev.siluet.locale

import dev.siluet.loader.SiluetAssetLoader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal class LocaleRegistry(
    private val assetLoader: SiluetAssetLoader
) {
    private val translationCache = ConcurrentHashMap<Locale, Map<String, String>>()

    fun getTranslation(locale: Locale, translationId: String): String {
        val translations = translationCache.computeIfAbsent(locale) { locale ->
            val jsonSource = assetLoader.loadLocale(locale)
                ?: assetLoader.loadLocale(Locale.getDefault())
                ?: throw LocaleNotFoundException(locale) //TODO implement something better

            LocaleParser.parse(jsonSource)
        }

        return translations[translationId] ?: translationId //TODO implement something better
    }
}
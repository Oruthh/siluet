package dev.siluet.cache

import dev.siluet.view.SiluetSectionView
import dev.siluet.view.SiluetView
import java.util.Locale

internal sealed interface CacheKey<V : Any> {
    data class Template(val name: String) : CacheKey<dev.siluet.blueprint.Template>
    data class Layout(val name: String) : CacheKey<SiluetView>
    data class Component(val name: String) : CacheKey<SiluetView>
    data class Section(val name: String) : CacheKey<SiluetSectionView>
    data class SectionsGroup(val name: String) : CacheKey<dev.siluet.blueprint.SectionsGroup>
    data class Translations(val locale: Locale) : CacheKey<Map<String, String>>
}

@Suppress("UNCHECKED_CAST")
internal fun <V : Any> CacheKey<V>.cast(value: Any): V = value as V
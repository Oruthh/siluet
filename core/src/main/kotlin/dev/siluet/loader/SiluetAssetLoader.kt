package dev.siluet.loader

import java.util.Locale

interface SiluetAssetLoader {
    fun loadTemplate(name: String): String?
    fun loadSectionsGroup(name: String): String?
    fun loadLayout(name: String): String?
    fun loadSection(name: String): String?
    fun loadComponent(name: String): String?
    fun loadLocale(locale: Locale): String?
}
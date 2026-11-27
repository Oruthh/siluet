package dev.siluet.quickstart

import dev.siluet.Siluet
import dev.siluet.loader.ResourceSiluetAssetLoader
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

fun main() {
    // 1. Create the engine. Everything shared by all themes is configured here:
    //    filters, directives, plugins, the security policy and render limits.
    val siluet = Siluet {}

    // 2. Create a theme pointing to the resources directory.
    //    A theme owns its templates, views and translations together with their caches,
    //    so create it once and reuse it for every render.
    //    (You can replace the loader with your own implementation, e.g. a database-backed one.)
    val theme = siluet.theme {
        assetLoader = ResourceSiluetAssetLoader("theme")
    }

    // 3. Prepare the model data
    val model = StorefrontModel(
        storeName = "Siluet Tech Store",
        activeUser = "Jane Doe",
        generatedAt = LocalDateTime.now(),
        featuredProducts = listOf(
            Product("p_101", "Mechanical Keyboard", 129.99),
            Product("p_102", "Wireless Mouse", 59.99),
            Product("p_103", "USB-C Hub", 34.50)
        )
    )

    // 4. Render the "home" template configuration
    // The locale determines which translation file (e.g., en.json) is loaded.
    val rendered = theme.render("home", model) {
        locale = Locale.ENGLISH
        zoneId = ZoneId.of("Europe/London")
    }

    println(rendered)
}
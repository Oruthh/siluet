package dev.siluet.quickstart

import dev.siluet.Siluet
import dev.siluet.loader.ResourceSiluetAssetLoader
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

fun main() {
    // 1. Initialize the engine pointing to the resources directory
    // (You can replace this with your own custom asset loader implementation)
    val siluet = Siluet {
        assetLoader = ResourceSiluetAssetLoader("theme")
    }

    // 2. Prepare the model data
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

    // 3. Render the "home" template configuration
    // The locale determines which translation file (e.g., en.json) is loaded.
    val rendered = siluet.render("home", model) {
        locale = Locale.ENGLISH
        zoneId = ZoneId.of("Europe/London")
    }

    println(rendered)
}
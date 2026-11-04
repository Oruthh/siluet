package dev.siluet.quickstart

import dev.siluet.Siluet
import dev.siluet.loader.ResourceSiluetAssetLoader
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

fun main() {
    val siluet = Siluet{
        assetLoader = ResourceSiluetAssetLoader("theme")
    }

    val model = QuickStartModel(
        projectName = "Siluet",
        date = LocalDateTime.now(),
        showDate = true,
        randomNumbers = List(10) { Random.nextInt(0, 100) }
    )

    val rendered = siluet.render("main", model) {
        locale = Locale.ENGLISH
        zoneId = ZoneId.of("Europe/London")
    }

    println(rendered)
}
package dev.siluet

import dev.siluet.loader.ResourceSiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * A component used in a section does not see the section's `section` variable unless it is
 * passed explicitly.
 *
 * Resources: `test23/`
 * - `sections/hero.html`: uses `components/title.html` without and with `section = section`,
 * - `components/title.html`: prints `[section.properties.title]`, `-` when it is missing.
 */
class SiluetComponentScopeReportingTest {

    @Test
    fun `section variable is visible in a component only when passed`() {
        val siluet = Siluet { }

        assertEquals(
            "<main><section>[-] [Summer sale]</section></main>",
            siluet.theme {
                assetLoader = ResourceSiluetAssetLoader("test23")
            }.render("main").trim()
        )
    }
}

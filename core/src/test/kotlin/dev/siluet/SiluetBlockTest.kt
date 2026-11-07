package dev.siluet

import dev.siluet.loader.ResourceSiluetAssetLoader
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals

class SiluetBlockTest {

    @Test
    fun `rendering layout with section properties and blocks`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test14")
        }

        val rendered = siluet.render("main"){
            locale = Locale.ENGLISH
        }
        assertEquals("<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>LayoutTest</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <section>\n" +
                    "    <h1>Store Features</h1>\n" +
                    "    <span class=\"columns\">3</span>\n" +
                    "    <span class=\"badge\">New Features Badge</span>\n" +
                    "    \n" +
                    "        <div class=\"featureCard\">\n" +
                    "            <strong>truck</strong>: Free Delivery\n" +
                    "        </div>\n" +
                    "    \n" +
                    "        <div class=\"featureCard\">\n" +
                    "            <strong>shield</strong>: Secure Payments\n" +
                    "        </div>\n" +
                    "    \n" +
                    "</section>\n" +
                    "</body>\n" +
                    "</html>",
            rendered
        )
    }

    @Test
    fun `rendering layout with blocks custom ordering`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test15")
        }

        val rendered = siluet.render("main"){
            locale = Locale.ENGLISH
        }
        assertEquals(
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>LayoutTest</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div>Second Block</div><div>First Block</div>\n" +
            "</body>\n" +
            "</html>",
            rendered
        )
    }
}

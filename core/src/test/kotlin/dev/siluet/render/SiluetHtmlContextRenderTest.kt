package dev.siluet.render

import dev.siluet.exception.internal.RenderException
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * End-to-end (lexer → parser → renderer) checks for the HTML-aware lexer (issue #18, closes #1 and #2).
 * Tests marked `guard` already pass today.
 */
class SiluetHtmlContextRenderTest {

    data class SliderProps(val slider: Boolean)
    data class SliderSection(val properties: SliderProps)
    data class SliderPage(val section: SliderSection)

    data class Theme(val color: String)
    data class ThemePage(val theme: Theme)

    data class Product(val name: String, val price: String)
    data class ProductPage(val product: Product)

    data class Option(val id: Int, val name: String, val selected: Boolean)
    data class SelectPage(val options: List<Option>)

    // --- #2 ------------------------------------------------------------------------------------

    private val sliderTemplate = """
        |@if(section.properties.slider) {
        |    <style>.a { color: red; }</style>
        |    <script>if (window.Slider) { new Slider('.slider'); }</script>
        |    <div class="slider" data-config='{"autoplay": true, "delay": 3000}'>
        |        <button onclick="if (cart) { cart.open() }">Cart</button>
        |    </div>
        |    <p>after</p>
        |} @else {
        |    <p>static</p>
        |}
    """.trimMargin()

    @Test
    fun `issue 2 - inline css, js and attributes render inside the if block`() {
        val result = renderInline(sliderTemplate, SliderPage(SliderSection(SliderProps(slider = true))))

        assertEquals(
            """
            |
            |    <style>.a { color: red; }</style>
            |    <script>if (window.Slider) { new Slider('.slider'); }</script>
            |    <div class="slider" data-config='{"autoplay": true, "delay": 3000}'>
            |        <button onclick="if (cart) { cart.open() }">Cart</button>
            |    </div>
            |    <p>after</p>
            |
            """.trimMargin(),
            result
        )
    }

    @Test
    fun `issue 2 - else branch renders only the else body`() {
        val result = renderInline(sliderTemplate, SliderPage(SliderSection(SliderProps(slider = false))))

        assertEquals("\n    <p>static</p>\n", result)
    }

    // --- #1 ------------------------------------------------------------------------------------

    @Test
    fun `issue 1 - css at-rules render unchanged and print expressions are evaluated`() {
        val template = """
            <style>
              @font-face { font-family: "Inter"; src: url(/inter.woff2) format("woff2"); }
              @media (max-width: 600px) { .nav { position: sticky; } }
              @keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }
              @supports(display: grid) { .grid { display: grid; } }
              .hero { color: {{ theme.color }}; }
            </style>
        """.trimIndent()

        val result = renderInline(template, ThemePage(Theme("#ff0000")))

        assertEquals(template.replace("{{ theme.color }}", "#ff0000"), result)
    }

    @Test
    fun `json-ld renders unchanged with print expressions evaluated`() {
        val template = """
            <script type="application/ld+json">
              {
                "@context": "https://schema.org",
                "@type": "Product",
                "name": "{{ product.name }}",
                "offers": { "@type": "Offer", "price": "{{ product.price }}" }
              }
            </script>
        """.trimIndent()

        val result = renderInline(template, ProductPage(Product("Lamp", "49.99")))

        assertEquals(
            template.replace("{{ product.name }}", "Lamp").replace("{{ product.price }}", "49.99"),
            result
        )
    }

    @Test
    fun `at signs in attributes are literal and escaped at signs in content are printed`() {
        val template = """
            <p>Contact: hello\@shop.com</p>
            <a href="mailto:hello@shop.com">Mail</a>
            <a href="https://medium.com/@siluet">\@siluet</a>
        """.trimIndent()

        assertEquals(
            """
            <p>Contact: hello@shop.com</p>
            <a href="mailto:hello@shop.com">Mail</a>
            <a href="https://medium.com/@siluet">@siluet</a>
            """.trimIndent(),
            renderInline(template)
        )
    }

    @Test
    fun `attribute values with at signs and braces render unchanged inside a block`() {
        val template = """@if(section.properties.slider) {<a href="mailto:a@b.c" data-x='{"k": "@v"}'>x</a>}"""

        assertEquals(
            """<a href="mailto:a@b.c" data-x='{"k": "@v"}'>x</a>""",
            renderInline(template, SliderPage(SliderSection(SliderProps(slider = true))))
        )
    }

    @Test
    fun `backslashes in script render verbatim`() {
        val template = """<script>var re = /\d+\@\{\}/; var s = "a\\b";</script>"""

        assertEquals(template, renderInline(template))
    }

    @Test
    fun `guard - conditional attribute inside a for loop`() {
        val template =
            """<select>@for(option in options) {<option value="{{ option.id }}" @if(option.selected) { selected }>{{ option.name }}</option>}</select>"""

        val result = renderInline(
            template,
            SelectPage(listOf(Option(1, "A", selected = false), Option(2, "B", selected = true)))
        )

        assertEquals("""<select><option value="1" >A</option><option value="2"  selected >B</option></select>""", result)
    }

    @Test
    fun `guard - escaped directive renders literally`() {
        assertEquals("<p>Literal directive: @if(x)</p>", renderInline("""<p>Literal directive: \@if(x)</p>"""))
    }
}

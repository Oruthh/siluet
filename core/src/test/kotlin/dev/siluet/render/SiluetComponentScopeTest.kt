package dev.siluet.render

import dev.siluet.filter.Filter
import dev.siluet.filter.string.DefaultFilter
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * A component sees only the parameters passed to `@component(...)`. Neither the root model nor
 * variables of the place it is used in (loop variables, pagination, parameters of an outer
 * component) are visible inside it unless they are passed explicitly.
 *
 * Components used here (`components/` in test resources):
 * - `scope-probe`: prints `[product.name|title|siteName]`, `-` for a missing value
 * - `scope-outer`: prints `title`, then includes `scope-probe` without parameters
 * - `scope-page`: prints `[page]`
 * - `scope-loop`: loops over `items` with the loop variable `item`
 */
class SiluetComponentScopeTest {

    private val filters: Map<String, Filter> = mapOf("default" to DefaultFilter())

    private val model = mapOf(
        "siteName" to "Shop",
        "products" to listOf(mapOf("name" to "Mug"), mapOf("name" to "Tee")),
    )

    private fun render(template: String, model: Any? = this.model) = renderInline(template, model, filters)

    // --- the root model and caller variables are not visible ------------------------------------------

    @Test
    fun `root model is not visible in the component`() {
        assertEquals("[-|-|-]", render("""@component("scope-probe")"""))
    }

    @Test
    fun `loop variable of the caller is not visible in the component`() {
        assertEquals(
            "[-|-|-][-|-|-]",
            render("""@for(product in products) {@component("scope-probe")}""")
        )
    }

    @Test
    fun `pagination variable of the caller is not visible in the component`() {
        assertEquals(
            "[-]",
            render("""@paginate(products as page by 1) {@component("scope-page")}""")
        )
    }

    @Test
    fun `parameters of an outer component are not visible in a nested component`() {
        assertEquals(
            "Hi:[-|-|-]",
            render("""@component("scope-outer", title = "Hi")""")
        )
    }

    // --- passed parameters --------------------------------------------------------------------------

    @Test
    fun `passed parameter is visible in the component`() {
        assertEquals("[-|Hi|-]", render("""@component("scope-probe", title = "Hi")"""))
    }

    @Test
    fun `loop variable passed explicitly is visible in the component`() {
        assertEquals(
            "[Mug|-|-][Tee|-|-]",
            render("""@for(product in products) {@component("scope-probe", product = product)}""")
        )
    }

    @Test
    fun `parameter value is evaluated in the scope of the caller`() {
        assertEquals(
            "[-|Mug|-][-|Tee|-]",
            render("""@for(product in products) {@component("scope-probe", title = product.name)}""")
        )
    }

    @Test
    fun `root model value passed explicitly is visible in the component`() {
        assertEquals("[-|Shop|-]", render("""@component("scope-probe", title = siteName)"""))
    }

    // --- the caller's scope is kept --------------------------------------------------------------

    @Test
    fun `caller variables are visible again after the component`() {
        assertEquals(
            "[-|-|-]Mug [-|-|-]Tee ",
            render("""@for(product in products) {@component("scope-probe"){{ product.name }} }""")
        )
    }

    @Test
    fun `variables of the component do not leak to the caller`() {
        assertEquals(
            "<i>a</i><i>b</i>|-",
            render("""@component("scope-loop", items = list)|{{ item | default("-") }}""", model + ("list" to listOf("a", "b")))
        )
    }

    @Test
    fun `component parameter does not leak to the caller`() {
        assertEquals(
            "[-|Hi|-]|-",
            render("""@component("scope-probe", title = "Hi")|{{ title | default("-") }}""")
        )
    }
}

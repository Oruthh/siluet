package dev.siluet.render

import dev.siluet.exception.internal.RenderException
import dev.siluet.filter.Filter
import dev.siluet.filter.string.DefaultFilter
import dev.siluet.reflect.fixtures.Product
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * In strict mode a template fails on a variable or property that does not exist and on reading a
 * property of `null`. A variable or property that exists but holds `null` is still allowed.
 * Outside strict mode such expressions resolve to `null`, as before.
 *
 * Components used here (`components/` in test resources):
 * - `scope-probe`: prints `[product.name|title|siteName]` with `default("-")`
 * - `scope-page`: prints `[page]` with `default("-")`
 */
class SiluetStrictModeTest {

    private val filters: Map<String, Filter> = mapOf("default" to DefaultFilter())

    private val model = mapOf(
        "product" to Product(price = 100, discount = 20),
        "user" to mapOf("name" to "John", "address" to null),
        "customer" to mapOf("name" to "Anna", "address" to mapOf("city" to "Kraków")),
        "products" to listOf(mapOf("name" to "Mug"), mapOf("name" to "Tee")),
        "nothing" to null,
    )

    private fun render(template: String) = renderInline(template, model, filters, strictMode = true)

    private fun renderError(template: String): RenderException =
        assertFailsWith<RenderException>("No error for:\n$template") { render(template) }

    // --- undefined variables and properties -------------------------------------------------------

    @Test
    fun `undefined variable fails at the variable`() {
        val exception = renderError("""<p>{{ prodcut }}</p>""")

        assertEquals("Undefined variable 'prodcut'", exception.message)
        assertEquals(1 to 7, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `undefined property of an object fails at the property`() {
        val exception = renderError("""{{ product.prise }}""")

        assertEquals("Undefined property 'prise' of 'product'", exception.message)
        assertEquals(1 to 12, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `undefined key of a map fails`() {
        assertEquals("Undefined property 'nmae' of 'user'", renderError("""{{ user.nmae }}""").message)
    }

    @Test
    fun `undefined property message shows the whole path of the receiver`() {
        assertEquals(
            "Undefined property 'ctiy' of 'customer.address'",
            renderError("""{{ customer.address.ctiy }}""").message
        )
    }

    @Test
    fun `undefined variable fails in a condition`() {
        assertEquals("Undefined variable 'isAdmin'", renderError("""@if(isAdmin) {admin}""").message)
    }

    @Test
    fun `undefined variable fails as a loop collection`() {
        assertEquals("Undefined variable 'itmes'", renderError("""@for(item in itmes) {x}""").message)
    }

    @Test
    fun `undefined variable fails as a filter argument`() {
        assertEquals(
            "Undefined variable 'fallbak'",
            renderError("""{{ nothing | default(fallbak) }}""").message
        )
    }

    @Test
    fun `default filter does not hide an undefined variable`() {
        assertEquals(
            "Undefined variable 'prodcut'",
            renderError("""{{ prodcut | default("-") }}""").message
        )
    }

    @Test
    fun `undefined variable fails in a component argument`() {
        assertEquals(
            "Undefined variable 'missing'",
            renderError("""@component("scope-probe", title = missing)""").message
        )
    }

    @Test
    fun `argument not passed to a component is undefined inside it`() {
        val exception = renderError("""@paginate(products as page by 1) {@component("scope-page")}""")

        assertEquals("Undefined variable 'page'", exception.message)
        assertTrue(
            exception.sourceLocation.filePath.endsWith("components/scope-page.html"),
            "Error should point at the component: ${exception.sourceLocation}"
        )
    }

    // --- reading a property of null ---------------------------------------------------------------

    @Test
    fun `reading a property of null fails at the property`() {
        val exception = renderError("""{{ user.address.city }}""")

        assertEquals("Cannot read property 'city' of null 'user.address'", exception.message)
        assertEquals(1 to 17, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `reading a property of a null variable fails`() {
        assertEquals(
            "Cannot read property 'length' of null 'nothing'",
            renderError("""{{ nothing.length }}""").message
        )
    }

    // --- what strict mode allows ------------------------------------------------------------------

    @Test
    fun `variable holding null prints nothing`() {
        assertEquals("[]", render("""[{{ nothing }}]"""))
    }

    @Test
    fun `property holding null prints nothing`() {
        assertEquals("[]", render("""[{{ user.address }}]"""))
    }

    @Test
    fun `default filter works for null`() {
        assertEquals("-|-", render("""{{ nothing | default("-") }}|{{ user.address | default("-") }}"""))
    }

    @Test
    fun `condition guards an optional property`() {
        assertEquals(
            "none|<b>Kraków</b>",
            render(
                """@if(user.address) {<b>{{ user.address.city }}</b>} @else {none}|""" +
                    """@if(customer.address) {<b>{{ customer.address.city }}</b>} @else {none}"""
            )
        )
    }

    @Test
    fun `short-circuited operand is not evaluated`() {
        assertEquals(
            "no|yes",
            render("""@if(user.address && user.address.city) {yes} @else {no}|@if(true || missing) {yes}""")
        )
    }

    @Test
    fun `existing properties loop variables and component arguments resolve`() {
        assertEquals(
            "John 80 <i>Mug</i><i>Tee</i>[2]",
            render(
                """{{ user.name }} {{ product.finalPrice }} @for(item in products) {<i>{{ item.name }}</i>}""" +
                    """@component("scope-page", page = 2)"""
            )
        )
    }

    @Test
    fun `pagination works without page variables in the model`() {
        assertEquals(
            "<i>1/2</i>",
            render("""@paginate(products as page by 1) {<i>{{ page.currentPage }}/{{ page.totalPages }}</i>}""")
        )
    }

    // --- strict mode off --------------------------------------------------------------------------

    @Test
    fun `without strict mode undefined and null receivers resolve to null`() {
        assertEquals(
            "[||||-]",
            renderInline(
                """[{{ prodcut }}|{{ product.prise }}|{{ user.nmae }}|{{ user.address.city }}|{{ prodcut | default("-") }}]""",
                model,
                filters
            )
        )
    }
}

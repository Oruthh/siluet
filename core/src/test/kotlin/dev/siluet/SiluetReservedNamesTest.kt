package dev.siluet

import dev.siluet.plugin.SiluetPlugin
import dev.siluet.directive.Directive
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Names of built-in filters and directives are reserved. Registering a filter or a directive
 * under such a name fails when the engine is built, instead of being silently ignored.
 */
class SiluetReservedNamesTest {

    private val builtInFilters = listOf("rawHTML", "default", "lowercase", "uppercase", "capitalize", "truncate", "t", "date")
    private val builtInDirectives = listOf("if", "else", "for", "component", "sections", "paginate", "schema")

    private val noop = Directive { _, _, _, _, _ -> }

    private fun build(configure: SiluetConfig.() -> Unit): Siluet = Siluet {
        assetLoader = InMemorySiluetAssetLoader(emptyMap())
        configure()
    }

    // --- filters ----------------------------------------------------------------------------------

    @Test
    fun `every built-in filter name is rejected`() {
        builtInFilters.forEach { name ->
            val exception = assertFailsWith<IllegalArgumentException>("No error for filter '$name'") {
                build { filter(name) { value, _, _ -> value } }
            }

            assertEquals("Filter '$name' is built in and cannot be replaced.", exception.message)
        }
    }

    @Test
    fun `built-in filter name is rejected when the filter is registered as an instance`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            build { filter("date", dev.siluet.filter.Filter { value, _, _ -> value }) }
        }

        assertEquals("Filter 'date' is built in and cannot be replaced.", exception.message)
    }

    // --- directives -------------------------------------------------------------------------------

    @Test
    fun `every built-in directive name is rejected`() {
        builtInDirectives.forEach { name ->
            val exception = assertFailsWith<IllegalArgumentException>("No error for directive '$name'") {
                build { directive(name, noop) }
            }

            assertEquals("Directive '$name' is built in and cannot be replaced.", exception.message)
        }
    }

    // --- plugins ----------------------------------------------------------------------------------

    @Test
    fun `plugin registering a built-in filter name is rejected`() {
        val plugin = object : SiluetPlugin<Unit> {
            override fun install(builder: SiluetConfig, configure: Unit.() -> Unit) {
                builder.filter("truncate") { value, _, _ -> value }
            }
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            build { install(plugin) }
        }

        assertEquals("Filter 'truncate' is built in and cannot be replaced.", exception.message)
    }

    @Test
    fun `plugin registering a built-in directive name is rejected`() {
        val plugin = object : SiluetPlugin<Unit> {
            override fun install(builder: SiluetConfig, configure: Unit.() -> Unit) {
                builder.directive("component", noop)
            }
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            build { install(plugin) }
        }

        assertEquals("Directive 'component' is built in and cannot be replaced.", exception.message)
    }

    // --- guards -----------------------------------------------------------------------------------

    @Test
    fun `guard - custom filter and directive names are accepted`() {
        build {
            filter("money") { value, _, _ -> value }
            directive("alert", noop)
        }
    }

    @Test
    fun `guard - names are case-sensitive`() {
        build {
            filter("Date") { value, _, _ -> value }
            filter("UPPERCASE") { value, _, _ -> value }
            directive("If", noop)
        }
    }

    @Test
    fun `guard - a filter and a directive may share a custom name`() {
        build {
            filter("alert") { value, _, _ -> value }
            directive("alert", noop)
        }
    }
}

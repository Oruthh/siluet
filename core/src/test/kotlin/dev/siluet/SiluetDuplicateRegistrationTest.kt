package dev.siluet

import dev.siluet.plugin.SiluetPlugin
import dev.siluet.directive.Directive
import dev.siluet.support.InMemorySiluetAssetLoader
import dev.siluet.theme.SiluetTheme
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val noop = Directive { _, _, _, _, _ -> }

private object MoneyPlugin : SiluetPlugin<Unit> {
    override fun install(builder: SiluetConfig, configure: Unit.() -> Unit) {
        builder.filter("money") { value, _, _ -> value }
        builder.directive("price", noop)
    }
}

private object ShopPlugin : SiluetPlugin<Unit> {
    override fun install(builder: SiluetConfig, configure: Unit.() -> Unit) {
        builder.filter("money") { value, _, _ -> value }
    }
}

private object PricingPlugin : SiluetPlugin<Unit> {
    override fun install(builder: SiluetConfig, configure: Unit.() -> Unit) {
        builder.directive("price", noop)
    }
}

private object InnerPlugin : SiluetPlugin<Unit> {
    override fun install(builder: SiluetConfig, configure: Unit.() -> Unit) {
        builder.filter("inner") { value, _, _ -> value }
    }
}

private object OuterPlugin : SiluetPlugin<Unit> {
    override fun install(builder: SiluetConfig, configure: Unit.() -> Unit) {
        builder.install(InnerPlugin)
        builder.filter("outer") { value, _, _ -> value }
    }
}

/**
 * A filter or directive name can be registered only once. A second registration fails at once,
 * naming who registered the name first: the application or the plugin that installed it.
 */
class SiluetDuplicateRegistrationTest {

    private fun build(configure: SiluetConfig.() -> Unit): SiluetTheme = Siluet {
        configure()
    }.theme {
        assetLoader = InMemorySiluetAssetLoader(emptyMap())
    }

    private fun duplicateError(configure: SiluetConfig.() -> Unit): String? =
        assertFailsWith<IllegalArgumentException> { build(configure) }.message

    // --- filters ----------------------------------------------------------------------------------

    @Test
    fun `filter registered twice by the application`() {
        assertEquals(
            "Filter 'money' is already registered by the application.",
            duplicateError {
                filter("money") { value, _, _ -> value }
                filter("money") { value, _, _ -> value }
            }
        )
    }

    @Test
    fun `application filter with a name registered by a plugin`() {
        assertEquals(
            "Filter 'money' is already registered by MoneyPlugin.",
            duplicateError {
                install(MoneyPlugin)
                filter("money") { value, _, _ -> value }
            }
        )
    }

    @Test
    fun `plugin filter with a name registered by the application`() {
        assertEquals(
            "Filter 'money' is already registered by the application.",
            duplicateError {
                filter("money") { value, _, _ -> value }
                install(MoneyPlugin)
            }
        )
    }

    @Test
    fun `two plugins registering the same filter`() {
        assertEquals(
            "Filter 'money' is already registered by MoneyPlugin.",
            duplicateError {
                install(MoneyPlugin)
                install(ShopPlugin)
            }
        )
    }

    @Test
    fun `filter registered as an instance counts as a registration`() {
        assertEquals(
            "Filter 'money' is already registered by the application.",
            duplicateError {
                filter("money", dev.siluet.filter.Filter { value, _, _ -> value })
                filter("money") { value, _, _ -> value }
            }
        )
    }

    // --- directives -------------------------------------------------------------------------------

    @Test
    fun `directive registered twice by the application`() {
        assertEquals(
            "Directive 'alert' is already registered by the application.",
            duplicateError {
                directive("alert", noop)
                directive("alert", noop)
            }
        )
    }

    @Test
    fun `two plugins registering the same directive`() {
        assertEquals(
            "Directive 'price' is already registered by MoneyPlugin.",
            duplicateError {
                install(MoneyPlugin)
                install(PricingPlugin)
            }
        )
    }

    @Test
    fun `application directive with a name registered by a plugin`() {
        assertEquals(
            "Directive 'price' is already registered by MoneyPlugin.",
            duplicateError {
                install(MoneyPlugin)
                directive("price", noop)
            }
        )
    }

    // --- plugins ----------------------------------------------------------------------------------

    @Test
    fun `same plugin installed twice`() {
        assertEquals(
            "MoneyPlugin is already installed.",
            duplicateError {
                install(MoneyPlugin)
                install(MoneyPlugin)
            }
        )
    }

    @Test
    fun `owner is restored after a plugin installs another plugin`() {
        assertEquals(
            "Filter 'outer' is already registered by OuterPlugin.",
            duplicateError {
                install(OuterPlugin)
                filter("outer") { value, _, _ -> value }
            }
        )
    }

    @Test
    fun `filter of a plugin installed by another plugin is owned by the inner plugin`() {
        assertEquals(
            "Filter 'inner' is already registered by InnerPlugin.",
            duplicateError {
                install(OuterPlugin)
                filter("inner") { value, _, _ -> value }
            }
        )
    }

    @Test
    fun `registrations after a plugin belong to the application again`() {
        assertEquals(
            "Filter 'local' is already registered by the application.",
            duplicateError {
                install(MoneyPlugin)
                filter("local") { value, _, _ -> value }
                filter("local") { value, _, _ -> value }
            }
        )
    }

    // --- the error is raised at the registration -------------------------------------------------------

    @Test
    fun `duplicate is rejected at the registration call`() {
        val config = SiluetConfig()
        config.filter("money") { value, _, _ -> value }

        val exception = assertFailsWith<IllegalArgumentException> {
            config.filter("money") { value, _, _ -> value }
        }

        assertEquals("Filter 'money' is already registered by the application.", exception.message)
    }

    @Test
    fun `built-in name is rejected at the registration call`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            SiluetConfig().filter("date") { value, _, _ -> value }
        }

        assertEquals("Filter 'date' is built in and cannot be replaced.", exception.message)
    }

    @Test
    fun `built-in directive name is rejected at the registration call`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            SiluetConfig().directive("if", noop)
        }

        assertEquals("Directive 'if' is built in and cannot be replaced.", exception.message)
    }

    // --- guards -----------------------------------------------------------------------------------

    @Test
    fun `guard - different plugins with different names install together`() {
        build {
            install(MoneyPlugin)
            install(InnerPlugin)
        }
    }

    @Test
    fun `guard - a filter and a directive may share a name`() {
        build {
            filter("alert") { value, _, _ -> value }
            directive("alert", noop)
        }
    }

    @Test
    fun `guard - registered filters and directives are visible in the configuration`() {
        val config = SiluetConfig().apply {
            install(MoneyPlugin)
            directive("alert", noop)
        }

        assertEquals(setOf("money"), config.filters.keys)
        assertEquals(setOf("price", "alert"), config.directives.keys)
    }
}

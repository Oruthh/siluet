package dev.siluet.render

import dev.siluet.blueprint.Template
import dev.siluet.cache.InMemorySiluetCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.filter.Filter
import dev.siluet.lexer.SiluetLexer
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.parser.SiluetParser
import kotlinx.serialization.json.Json
import java.util.Locale

class SiluetRendererTest {

    // Sample DTOs for testing
    data class Address(val city: String, val zipCode: String?)
    data class Customer(val name: String, val email: String, val address: Address?)
    data class OrderItem(val title: String, val price: Double)
    data class OrderDto(
        val orderId: String,
        val customer: Customer,
        val items: List<OrderItem>,
        val isDelivered: Boolean,
        val discount: Double? = null
    )

    private val json = Json { ignoreUnknownKeys = true }

    private fun render(
        template: String,
        model: Any = Unit,
        filters: Map<String, Filter> = emptyMap(),
        directives: Map<String, Directive> = emptyMap()
    ): String {
        val tokens = SiluetLexer(template).tokenize()
        val ast = SiluetParser(tokens).parseAsNodeList()
        val renderer = SiluetRenderer(
            maxLoopIterations = 100,
            maxRenderDepth = 32,
            filters = filters,
            directives = directives,
            viewsLoader = SiluetViewsLoader(
                cache = InMemorySiluetCache(),
                assetLoader = ResourceSiluetAssetLoader(),
            ),
            sectionsGroupsLoader = SiluetSectionsGroupLoader(
                assetLoader = ResourceSiluetAssetLoader(),
                json = json,
                cache = InMemorySiluetCache(),
            ),
            localeRegistry = LocaleRegistry(
                assetLoader = ResourceSiluetAssetLoader(),
            ),
        )
        return renderer.render(ast, model, Template(
            layout = "",
            sections = emptyList()
        ))
    }

    @Test
    fun `should render static text unchanged`() {
        val result = render("Hello, this is pure static text.")
        assertEquals("Hello, this is pure static text.", result)
    }

    @Test
    fun `should read top-level properties from DTO`() {
        val model = OrderDto(
            orderId = "ORD-100",
            customer = Customer("John Doe", "john@example.com", null),
            items = emptyList(),
            isDelivered = false
        )

        val result = render("Order ID: {{ orderId }}", model)
        assertEquals("Order ID: ORD-100", result)
    }

    @Test
    fun `should navigate nested DTO properties`() {
        val model = OrderDto(
            orderId = "ORD-101",
            customer = Customer(
                name = "Alice",
                email = "alice@example.com",
                address = Address(city = "Warsaw", zipCode = "00-001")
            ),
            items = emptyList(),
            isDelivered = false
        )

        val result = render("City: {{ customer.address.city }}", model)
        assertEquals("City: Warsaw", result)
    }

    @Test
    fun `should render empty string for null values without throwing`() {
        val model = OrderDto(
            orderId = "ORD-102",
            customer = Customer("Bob", "bob@example.com", address = null),
            items = emptyList(),
            isDelivered = false,
            discount = null
        )

        val result = render("Discount: [{{ discount }}], Address: [{{ customer.address.city }}]", model)
        assertEquals("Discount: [], Address: []", result)
    }

    @Test
    fun `should apply registered single filter without arguments`() {
        val filters = mapOf(
            "uppercase" to Filter { value, _, _ -> value?.toString()?.uppercase() }
        )
        val model = Customer("alice", "alice@example.com", null)

        val result = render("Name: {{ name | uppercase }}", model, filters = filters)
        assertEquals("Name: ALICE", result)
    }

    @Test
    fun `should apply filter with custom arguments`() {
        val filters = mapOf(
            "currency" to Filter { value, args, _ ->
                val symbol = args.firstOrNull()?.toString() ?: "$"
                val num = (value as? Number)?.toDouble() ?: 0.0
                String.format(Locale.US, "%s%.2f", symbol, num)
            }
        )
        val model = OrderItem(title = "Widget", price = 49.99)

        val result = render("Price: {{ price | currency(\"EUR \") }}", model, filters = filters)
        assertEquals("Price: EUR 49.99", result)
    }

    @Test
    fun `should render then-block when if-condition evaluates to true`() {
        val model = OrderDto(
            orderId = "ORD-200",
            customer = Customer("Charlie", "charlie@example.com", null),
            items = emptyList(),
            isDelivered = true
        )

        val template = "@if(isDelivered == true) { DELIVERED } @else { PENDING }"
        val result = render(template, model)

        assertEquals(" DELIVERED ", result)
    }

    @Test
    fun `should render else-block when if-condition evaluates to false`() {
        val model = OrderDto(
            orderId = "ORD-201",
            customer = Customer("Charlie", "charlie@example.com", null),
            items = emptyList(),
            isDelivered = false
        )

        val template = "@if(isDelivered == true) { DELIVERED } @else { PENDING }"
        val result = render(template, model)

        assertEquals(" PENDING ", result)
    }

    @Test
    fun `should iterate over collection in for-loop and resolve item scope`() {
        val model = OrderDto(
            orderId = "ORD-300",
            customer = Customer("Diana", "diana@example.com", null),
            items = listOf(
                OrderItem("Keyboard", 100.0),
                OrderItem("Mouse", 50.0)
            ),
            isDelivered = false
        )

        val template = "@for(item in items) { [{{ item.title }}: {{ item.price }}] }"
        val result = render(template, model)

        assertEquals(" [Keyboard: 100.0]  [Mouse: 50.0] ", result)
    }

    @Test
    fun `should execute custom block directive with transformed body`() {
        val directives = mapOf(
            "quote" to Directive { _, _, body, _, out ->
                val inner = body?.invoke() ?: ""
                out.append("> ").append(inner.trim())
            }
        )

        val template = "@quote { Important announcement: {{ message }} }"
        val result = render(template, mapOf("message" to "Maintenance at midnight"), directives = directives)

        assertEquals("> Important announcement: Maintenance at midnight", result)
    }
}
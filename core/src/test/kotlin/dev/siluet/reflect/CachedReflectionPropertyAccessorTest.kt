package dev.siluet.reflect

import dev.siluet.exception.internal.PropertyAccessException
import dev.siluet.reflect.fixtures.Account
import dev.siluet.reflect.fixtures.FatalTestError
import dev.siluet.reflect.fixtures.FluentBean
import dev.siluet.reflect.fixtures.JavaBean
import dev.siluet.reflect.fixtures.JavaInheritance
import dev.siluet.reflect.fixtures.JavaRecord
import dev.siluet.reflect.fixtures.LabeledItem
import dev.siluet.reflect.fixtures.LegacyBean
import dev.siluet.reflect.fixtures.OverridingItem
import dev.siluet.reflect.fixtures.Product
import dev.siluet.reflect.fixtures.Status
import dev.siluet.reflect.fixtures.anonymousLabeled
import dev.siluet.reflect.fixtures.hiddenLabeled
import dev.siluet.reflect.fixtures.unreachable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Property resolution of [CachedReflectionPropertyAccessor] (issue #7).
 *
 * Properties are resolved from public zero-argument getters, so computed and interface
 * properties work, while everything that worked before #7 (field-backed accessors) keeps working.
 *
 * Fixtures: `dev.siluet.reflect.fixtures` (Kotlin in `src/test/kotlin`, Java beans in `src/test/java`).
 * Security rules are covered separately in [CachedReflectionPropertyAccessorSecurityTest].
 */
class CachedReflectionPropertyAccessorTest {

    private val accessor = CachedReflectionPropertyAccessor()

    private fun read(target: Any, property: String): Any? = accessor.getProperty(target, property)

    // --- Kotlin ---------------------------------------------------------------------------------

    @Test
    fun `computed Boolean property with is prefix resolves`() {
        assertEquals(true, read(Product(price = 100, discount = 20), "isOnSale"))
        assertEquals(false, read(Product(price = 100, discount = 0), "isOnSale"))
    }

    @Test
    fun `computed property resolves`() {
        assertEquals(80, read(Product(price = 100, discount = 20), "finalPrice"))
    }

    @Test
    fun `stored constructor property still resolves`() {
        assertEquals(100, read(Product(price = 100, discount = 20), "price"))
    }

    @Test
    fun `Kotlin is prefixed property of non Boolean type keeps its name`() {
        assertEquals("featured", read(Product(100, 0), "isFeatured"))
    }

    @Test
    fun `lazy delegated property resolves`() {
        assertEquals("lazy description", read(Product(100, 0), "description"))
    }

    @Test
    fun `stored var with private setter resolves`() {
        assertEquals(0, read(Account("john", "s3cret"), "deleteCalls"))
    }

    // --- Interfaces -----------------------------------------------------------------------------

    @Test
    fun `property implemented from interface resolves`() {
        assertEquals("A-1", read(LabeledItem("A-1"), "code"))
    }

    @Test
    fun `interface default getter resolves`() {
        assertEquals("default label", read(LabeledItem("A-1"), "label"))
    }

    @Test
    fun `overridden interface property resolves to the override`() {
        assertEquals("overridden label", read(OverridingItem(), "label"))
    }

    @Test
    fun `private Kotlin class is read through its public interface`() {
        assertEquals("hidden", read(hiddenLabeled(), "code"))
        assertEquals("default label", read(hiddenLabeled(), "label"))
    }

    @Test
    fun `anonymous object is read through its public interface`() {
        assertEquals("anonymous", read(anonymousLabeled(), "code"))
    }

    @Test
    fun `private Java class is read through its public interface`() {
        assertEquals("hidden implementation", read(JavaInheritance.hiddenNamed(), "name"))
    }

    @Test
    fun `public getter inherited from package private Java class resolves`() {
        assertEquals("inherited value", read(JavaInheritance.PublicChild(), "inherited"))
    }

    // --- Java beans -----------------------------------------------------------------------------

    @Test
    fun `Java boolean is getter is exposed without the prefix`() {
        assertEquals(true, read(JavaBean(), "active"))
    }

    @Test
    fun `Java boxed Boolean is getter is exposed without the prefix`() {
        assertEquals(true, read(JavaBean(), "verified"))
    }

    @Test
    fun `Java getter without backing field resolves`() {
        assertEquals("computed value", read(JavaBean(), "computed"))
    }

    @Test
    fun `getter name with two leading capitals keeps them`() {
        assertEquals("https://siluet.dev", read(JavaBean(), "URL"))
        assertNull(read(JavaBean(), "uRL"))
    }

    @Test
    fun `Java is prefix on non boolean method is not a getter`() {
        assertNull(read(JavaBean(), "notBoolean"))
        assertNull(read(JavaBean(), "isNotBoolean"))
    }

    @Test
    fun `badly named Java bean keeps its pre-7 name`() {
        assertEquals(true, read(LegacyBean(), "isEnabled"))
    }

    @Test
    fun `fluent accessor backed by a same-named field still resolves`() {
        assertEquals("fluent title", read(FluentBean(), "title"))
    }

    // --- Records, enums, collections, maps ------------------------------------------------------

    @Test
    fun `record components resolve`() {
        val record = JavaRecord("Mug", 3)
        assertEquals("Mug", read(record, "title"))
        assertEquals(3, read(record, "quantity"))
    }

    @Test
    fun `enum name and ordinal still resolve`() {
        assertEquals("ARCHIVED", read(Status.ARCHIVED, "name"))
        assertEquals(1, read(Status.ARCHIVED, "ordinal"))
    }

    @Test
    fun `ArrayList size still resolves`() {
        assertEquals(3, read(arrayListOf("a", "b", "c"), "size"))
    }

    @Test
    fun `map keys are read directly`() {
        assertEquals("value", read(mapOf("key" to "value"), "key"))
        assertNull(read(mapOf("key" to "value"), "missing"))
    }

    // --- Missing properties ---------------------------------------------------------------------

    @Test
    fun `unknown property returns null`() {
        assertNull(read(Product(100, 0), "doesNotExist"))
    }

    @Test
    fun `private property without public getter returns null`() {
        assertNull(read(Account("john", "s3cret"), "secret"))
    }

    // --- Failures -------------------------------------------------------------------------------

    @Test
    fun `exception thrown by a getter is wrapped with property and class name`() {
        val exception = assertFailsWith<PropertyAccessException> {
            read(Product(100, 0), "broken")
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("'broken'"), "Message should name the property: $message")
        assertTrue(message.contains("'${Product::class.java.name}'"), "Message should name the class: $message")
        assertTrue(
            message.contains("IllegalStateException: price service unavailable"),
            "Message should contain the original failure: $message"
        )
        assertIs<IllegalStateException>(exception.cause)
    }

    @Test
    fun `Error thrown by a getter is not wrapped`() {
        assertFailsWith<FatalTestError> {
            read(Product(100, 0), "fatal")
        }
    }

    @Test
    fun `getter of private class without public supertype fails loudly instead of returning null`() {
        val exception = assertFailsWith<PropertyAccessException> {
            read(unreachable(), "value")
        }
        assertIs<IllegalAccessException>(exception.cause)
    }

    // --- Cache ----------------------------------------------------------------------------------

    @Test
    fun `repeated reads on different instances of the same class return their own values`() {
        assertEquals(80, read(Product(100, 20), "finalPrice"))
        assertEquals(45, read(Product(50, 5), "finalPrice"))
        assertEquals(true, read(Product(50, 5), "isOnSale"))
        assertEquals(false, read(Product(50, 0), "isOnSale"))
    }
}

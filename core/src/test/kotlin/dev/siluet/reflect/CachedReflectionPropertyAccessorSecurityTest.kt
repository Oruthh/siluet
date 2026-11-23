package dev.siluet.reflect

import dev.siluet.exception.SiluetSecurityException
import dev.siluet.reflect.fixtures.Account
import dev.siluet.reflect.fixtures.FluentBean
import dev.siluet.reflect.fixtures.JavaBean
import dev.siluet.reflect.fixtures.JavaRecord
import dev.siluet.reflect.fixtures.LegacyBean
import dev.siluet.reflect.fixtures.Product
import dev.siluet.reflect.fixtures.Status
import dev.siluet.reflect.fixtures.ThreadHolder
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

/**
 * Security guarantees of [CachedReflectionPropertyAccessor] after switching from field-based
 * to getter-based resolution (issue #7).
 *
 * Getter-based resolution exposes more members than the old field-based one, so these tests pin down
 * what must still never be reachable from a template:
 * - reflection / classloader members and dangerous receivers ([DefaultSecurityPolicy]),
 * - methods that are not getters (side effects), static methods, `Object` methods,
 * - any second spelling of a property that would bypass `blockedMembers`.
 */
class CachedReflectionPropertyAccessorSecurityTest {

    private val accessor = CachedReflectionPropertyAccessor()

    private fun read(target: Any, property: String): Any? =
        when (val lookup = accessor.getProperty(target, property)) {
            is PropertyLookup.Found -> lookup.value
            PropertyLookup.Missing -> fail("Property '$property' should be found on ${target::class.java.name}")
        }

    private fun assertMissing(target: Any, property: String) {
        assertEquals(PropertyLookup.Missing, accessor.getProperty(target, property))
    }

    private fun accessorBlocking(vararg members: String) =
        CachedReflectionPropertyAccessor(DefaultSecurityPolicy(blockedMembers = members.toSet()))

    // --- Forbidden members ----------------------------------------------------------------------

    @Test
    fun `class and getClass are forbidden`() {
        assertFailsWith<SiluetSecurityException> { read(Product(100, 0), "class") }
        assertFailsWith<SiluetSecurityException> { read(Product(100, 0), "getClass") }
    }

    @Test
    fun `classLoader is forbidden on any object`() {
        assertFailsWith<SiluetSecurityException> { read(Product(100, 0), "classLoader") }
        assertFailsWith<SiluetSecurityException> { read(Product(100, 0), "getClassLoader") }
    }

    @Test
    fun `enum declaringClass is forbidden`() {
        assertFailsWith<SiluetSecurityException> { read(Status.ACTIVE, "declaringClass") }
        assertFailsWith<SiluetSecurityException> { read(Status.ACTIVE, "getDeclaringClass") }
    }

    @Test
    fun `thread control methods are forbidden`() {
        assertFailsWith<SiluetSecurityException> { read(Product(100, 0), "wait") }
        assertFailsWith<SiluetSecurityException> { read(Product(100, 0), "notify") }
        assertFailsWith<SiluetSecurityException> { read(Product(100, 0), "notifyAll") }
    }

    // --- Forbidden receivers reached through getters --------------------------------------------

    @Test
    fun `members of a Class object are forbidden`() {
        assertFailsWith<SiluetSecurityException> { read(String::class.java, "name") }
    }

    @Test
    fun `members of a Thread returned by a getter are forbidden`() {
        val thread = read(ThreadHolder(), "thread")!!
        assertFailsWith<SiluetSecurityException> { read(thread, "name") }
        assertFailsWith<SiluetSecurityException> { read(thread, "contextClassLoader") }
    }

    @Test
    fun `members of a ThreadGroup are forbidden`() {
        assertFailsWith<SiluetSecurityException> { read(Thread.currentThread().threadGroup, "name") }
    }

    @Test
    fun `members of a ClassLoader returned by a getter are forbidden`() {
        val classLoader = read(ThreadHolder(), "classLoaderHolder")!!
        assertFailsWith<SiluetSecurityException> { read(classLoader, "parent") }
    }

    @Test
    fun `members of a Module returned by a getter are forbidden`() {
        val module = read(ThreadHolder(), "module")!!
        assertFailsWith<SiluetSecurityException> { read(module, "layer") }
        assertFailsWith<SiluetSecurityException> { read(module, "classLoader") }
    }

    @Test
    fun `members of Runtime are forbidden`() {
        assertFailsWith<SiluetSecurityException> { read(Runtime.getRuntime(), "availableProcessors") }
    }

    @Test
    fun `members of java io classes are forbidden`() {
        assertFailsWith<SiluetSecurityException> { read(File("."), "absolutePath") }
    }

    @Test
    fun `members of JDK internal sun classes are forbidden`() {
        // Charsets.UTF_8 is an instance of sun.nio.cs.UTF_8
        assertFailsWith<SiluetSecurityException> { read(Charsets.UTF_8, "name") }
    }

    // --- Only getters, never other methods ------------------------------------------------------

    @Test
    fun `Java non-getter method with side effects is never invoked`() {
        val bean = JavaBean()

        assertMissing(bean, "cancel")

        assertEquals(0, bean.cancelCalls(), "cancel() must not be invoked by property access")
    }

    @Test
    fun `Kotlin function with side effects is never invoked`() {
        val account = Account("john", "s3cret")

        assertMissing(account, "delete")

        assertEquals(0, account.deleteCalls, "delete() must not be invoked by property access")
    }

    @Test
    fun `fluent looking method without a same-named field is never invoked`() {
        val bean = FluentBean()

        assertMissing(bean, "reset")

        assertEquals(0, bean.resetCalls(), "reset() must not be invoked by property access")
    }

    @Test
    fun `Object methods are not exposed`() {
        assertMissing(Product(100, 0), "toString")
        assertMissing(Product(100, 0), "hashCode")
    }

    @Test
    fun `data class generated methods are not exposed`() {
        assertMissing(Product(100, 0), "component1")
        assertMissing(Product(100, 0), "copy")
    }

    @Test
    fun `record methods other than components are not exposed`() {
        assertMissing(JavaRecord("Mug", 3), "summary")
    }

    @Test
    fun `static getters are not exposed`() {
        assertMissing(JavaBean(), "instanceCount")
        assertMissing(Account("john", "s3cret"), "instanceCount")
    }

    // --- One name per property ------------------------------------------------------------------

    @Test
    fun `raw getter method names are not exposed`() {
        assertMissing(Product(100, 20), "getFinalPrice")
        assertMissing(JavaBean(), "getComputed")
        assertMissing(JavaBean(), "isActive")
    }

    @Test
    fun `Kotlin is property is not exposed without the prefix`() {
        assertMissing(Product(100, 20), "onSale")
        assertMissing(Account("john", "s3cret"), "admin")
    }

    @Test
    fun `badly named Java bean is not exposed under a second name`() {
        assertMissing(LegacyBean(), "enabled")
    }

    // --- blockedMembers cannot be bypassed ------------------------------------------------------

    @Test
    fun `blocked Kotlin property cannot be read through its getter name`() {
        val blocking = accessorBlocking("passwordHash")
        val account = Account("john", "s3cret")

        assertFailsWith<SiluetSecurityException> { blocking.getProperty(account, "passwordHash") }
        assertEquals(PropertyLookup.Missing, blocking.getProperty(account, "getPasswordHash"))
    }

    @Test
    fun `blocked Java property cannot be read through its getter name`() {
        val blocking = accessorBlocking("passwordHash")

        assertFailsWith<SiluetSecurityException> { blocking.getProperty(JavaBean(), "passwordHash") }
        assertEquals(PropertyLookup.Missing, blocking.getProperty(JavaBean(), "getPasswordHash"))
    }

    @Test
    fun `blocked Kotlin is property cannot be read without the prefix`() {
        val blocking = accessorBlocking("isAdmin")
        val account = Account("john", "s3cret")

        assertFailsWith<SiluetSecurityException> { blocking.getProperty(account, "isAdmin") }
        assertEquals(PropertyLookup.Missing, blocking.getProperty(account, "admin"))
    }

    @Test
    fun `blocked Java boolean property cannot be read with the is prefix`() {
        val blocking = accessorBlocking("active")

        assertFailsWith<SiluetSecurityException> { blocking.getProperty(JavaBean(), "active") }
        assertEquals(PropertyLookup.Missing, blocking.getProperty(JavaBean(), "isActive"))
    }

    @Test
    fun `blocked badly named Java property cannot be read under a second name`() {
        val blocking = accessorBlocking("isEnabled")

        assertFailsWith<SiluetSecurityException> { blocking.getProperty(LegacyBean(), "isEnabled") }
        assertEquals(PropertyLookup.Missing, blocking.getProperty(LegacyBean(), "enabled"))
    }

    @Test
    fun `blocking a getter method name blocks its property too`() {
        val blocking = accessorBlocking("getPasswordHash")

        assertFailsWith<SiluetSecurityException> { blocking.getProperty(JavaBean(), "passwordHash") }
    }
}

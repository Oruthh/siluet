package dev.siluet.reflect

import dev.siluet.exception.SiluetSecurityException
import dev.siluet.reflect.fixtures.Product
import dev.siluet.reflect.fixtures.WorkerThread
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertFailsWith

/**
 * Unit tests of [DefaultSecurityPolicy] in isolation.
 *
 * Getter-based property resolution (#7) makes the policy the main line of defense,
 * so its class and package rules are pinned down here, including the JDK internals added with #7.
 */
class DefaultSecurityPolicyTest {

    private val policy = DefaultSecurityPolicy()

    private fun assertForbidden(targetClass: Class<*>, member: String = "anything") {
        assertFailsWith<SiluetSecurityException>("Expected ${targetClass.name}.$member to be forbidden") {
            policy.checkAccess(targetClass, member)
        }
    }

    private fun assertAllowed(targetClass: Class<*>, member: String = "anything") {
        assertDoesNotThrow("Expected ${targetClass.name}.$member to be allowed") {
            policy.checkAccess(targetClass, member)
        }
    }

    // --- Allowed --------------------------------------------------------------------------------

    @Test
    fun `ordinary model classes are allowed`() {
        assertAllowed(Product::class.java, "finalPrice")
        assertAllowed(String::class.java, "length")
        assertAllowed(java.util.ArrayList::class.java, "size")
        assertAllowed(java.time.LocalDate::class.java, "year")
    }

    // --- Forbidden members ----------------------------------------------------------------------

    @Test
    fun `reflection members are forbidden on any class`() {
        listOf(
            "class", "getClass",
            "classLoader", "getClassLoader",
            "declaringClass", "getDeclaringClass",
            "protectionDomain", "getProtectionDomain",
            "wait", "notify", "notifyAll",
        ).forEach { assertForbidden(Product::class.java, it) }
    }

    // --- Forbidden classes ----------------------------------------------------------------------

    @Test
    fun `runtime and process classes are forbidden`() {
        assertForbidden(Class::class.java)
        assertForbidden(ClassLoader::class.java)
        assertForbidden(Runtime::class.java)
        assertForbidden(Process::class.java)
        assertForbidden(ProcessBuilder::class.java)
        assertForbidden(System::class.java)
    }

    @Test
    fun `thread and module classes are forbidden`() {
        assertForbidden(Thread::class.java)
        assertForbidden(ThreadGroup::class.java)
        assertForbidden(Module::class.java)
        assertForbidden(ModuleLayer::class.java)
    }

    @Test
    fun `subclasses of forbidden classes are forbidden`() {
        assertForbidden(WorkerThread::class.java)
        assertForbidden(ClassLoader.getSystemClassLoader()::class.java)
    }

    // --- Forbidden packages ---------------------------------------------------------------------

    @Test
    fun `reflection and io packages are forbidden`() {
        assertForbidden(java.lang.reflect.Method::class.java)
        assertForbidden(java.lang.invoke.MethodHandles::class.java)
        assertForbidden(java.io.File::class.java)
        assertForbidden(java.nio.file.Path::class.java)
        assertForbidden(java.net.URL::class.java)
        assertForbidden(java.security.KeyStore::class.java)
        assertForbidden(kotlin.reflect.KClass::class.java)
    }

    @Test
    fun `jdk internal packages are forbidden`() {
        assertForbidden(Class.forName("jdk.internal.misc.Unsafe"))
    }

    @Test
    fun `sun packages are forbidden`() {
        assertForbidden(Class.forName("sun.nio.cs.UTF_8"))
    }

    @Test
    fun `com sun packages are forbidden`() {
        assertForbidden(Class.forName("com.sun.net.httpserver.HttpServer"))
    }

    // --- Custom rules ---------------------------------------------------------------------------

    @Test
    fun `custom blocked members are forbidden`() {
        val custom = DefaultSecurityPolicy(blockedMembers = setOf("passwordHash"))

        assertFailsWith<SiluetSecurityException> { custom.checkAccess(Product::class.java, "passwordHash") }
        assertDoesNotThrow { custom.checkAccess(Product::class.java, "finalPrice") }
    }

    @Test
    fun `custom blocked packages are forbidden`() {
        val custom = DefaultSecurityPolicy(blockedPackages = setOf("dev.siluet.reflect.fixtures"))

        assertFailsWith<SiluetSecurityException> { custom.checkAccess(Product::class.java, "finalPrice") }
    }

    @Test
    fun `custom rules keep the default ones`() {
        val custom = DefaultSecurityPolicy(blockedMembers = setOf("passwordHash"))

        assertFailsWith<SiluetSecurityException> { custom.checkAccess(Product::class.java, "getClass") }
        assertFailsWith<SiluetSecurityException> { custom.checkAccess(Thread::class.java, "name") }
    }
}

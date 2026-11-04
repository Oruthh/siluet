package dev.siluet.reflect

import dev.siluet.exception.SiluetSecurityException

/**
 * Default implementation of [SecurityPolicy] providing sandbox guardrails against reflection exploits,
 * remote code execution (RCE), classloader traversal, and unauthorized system access in template expressions.
 *
 * Secures runtime property and method evaluations across three layers of defense:
 * 1. **Forbidden Members:** Blocks reflection metadata and thread-control methods (e.g., `class`, `getClass`, `classLoader`, `wait`).
 * 2. **Forbidden Classes:** Forbids target receivers assignable to dangerous runtime types (e.g., [System], [Runtime], [ProcessBuilder]).
 * 3. **Forbidden Packages:** Blocks receivers originating from system packages (e.g., `java.io`, `java.net`, `java.lang.reflect`, `kotlin.reflect`).
 *
 * ### Example:
 * ```kotlin
 * val policy = DefaultSecurityPolicy(
 *     blockedMembers = setOf("passwordHash", "apiKey"),
 *     blockedPackages = setOf("com.mycompany.app.internal")
 * )
 *
 * val siluet = Siluet {
 *     securityPolicy = policy
 * }
 * ```
 *
 * @param blockedMembers Additional property or method names to deny beyond standard internal members.
 * @param blockedPackages Additional package prefixes to deny beyond standard JVM/Kotlin reflection and I/O packages.
 */
class DefaultSecurityPolicy(
    blockedMembers: Set<String> = emptySet(),
    blockedPackages: Set<String> = emptySet(),
): SecurityPolicy {
    private val forbiddenMembers: Set<String> = setOf(
        "class",
        "getClass",
        "classLoader",
        "getClassLoader",
        "declaringClass",
        "getDeclaringClass",
        "protectionDomain",
        "getProtectionDomain",
        "wait",
        "notify",
        "notifyAll"
    ) + blockedMembers

    private val forbiddenPackagePrefixes: List<String> = (listOf(
        "java.lang.reflect",
        "java.lang.invoke",
        "java.lang.management",
        "java.io",
        "java.nio",
        "java.net",
        "java.security",
        "kotlin.reflect"
    ) + blockedPackages).distinct()

    private val forbiddenClasses: Set<Class<*>> = setOf(
        java.lang.Class::class.java,
        java.lang.ClassLoader::class.java,
        java.lang.Runtime::class.java,
        java.lang.Process::class.java,
        java.lang.ProcessBuilder::class.java,
        java.lang.System::class.java
    )

    /**
     * Validates whether reflection-based access to [memberName] on [targetClass] is permitted.
     *
     * Evaluates the request sequentially against:
     * 1. Member blacklist ([forbiddenMembers])
     * 2. Prohibited class types and assignable supertypes ([forbiddenClasses])
     * 3. Restricted package namespaces ([forbiddenPackagePrefixes])
     *
     * @param targetClass The receiver [Class] whose member is being accessed.
     * @param memberName The property name or method identifier requested by a template expression.
     * @throws dev.siluet.exception.SiluetSecurityException If the requested member, class, or package violates security constraints.
     */
    override fun checkAccess(targetClass: Class<*>, memberName: String) {
        if(memberName in forbiddenMembers) {
            throw SiluetSecurityException(
                "Access to member '$memberName' is forbidden by security policy."
            )
        }

        if(forbiddenClasses.any { it.isAssignableFrom(targetClass) }) {
            throw SiluetSecurityException(
                "Access to members on class '${targetClass.name}' is strictly forbidden."
            )
        }

        val packageName = targetClass.packageName
        if (forbiddenPackagePrefixes.any { packageName.startsWith(it) }) {
            throw SiluetSecurityException(
                "Access to class '${targetClass.name}' in package '$packageName' is forbidden."
            )
        }
    }
}
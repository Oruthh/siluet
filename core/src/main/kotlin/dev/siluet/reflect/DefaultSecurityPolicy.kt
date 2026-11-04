package dev.siluet.reflect

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
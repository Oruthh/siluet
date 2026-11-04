package dev.siluet.reflect

/**
 * Strategy contract defining security constraints for reflection-based property access and method invocation.
 *
 * During template expression evaluation, Siluet verifies that accessing memberName on an instance
 * of targetClass is permitted. Implementations can enforce whitelists, blacklists, or domain-specific
 * isolation to prevent arbitrary code execution (RCE) and unauthorized internal JVM inspection.
 *
 * @see DefaultSecurityPolicy
 */
interface SecurityPolicy {
    /**
     * Validates whether reading a property or invoking a method is permitted.
     *
     * Called by property accessors before inspecting or invoking any member on a target object.
     * Implementations should throw an exception if the operation is denied.
     *
     * @param targetClass The runtime [Class] of the receiver object.
     * @param memberName The identifier of the property or method being accessed.
     * @throws dev.siluet.exception.SiluetSecurityException If access to the requested member is forbidden.
     */
    fun checkAccess(targetClass: Class<*>, memberName: String)

    companion object {
        /**
         * Default security policy instance configured with standard sandbox rules.
         *
         * Backed by [DefaultSecurityPolicy] with default member, class, and package blacklists.
         */
        val Default: SecurityPolicy = DefaultSecurityPolicy()
    }
}
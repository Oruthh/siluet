package dev.siluet.exception

/**
 * Exception thrown when a template expression evaluation violates active security restrictions or sandbox rules.
 *
 * Typically thrown by implementations of [dev.siluet.reflect.SecurityPolicy] (such as [dev.siluet.reflect.DefaultSecurityPolicy]) when a template
 * attempts to access forbidden properties, inspect restricted packages, or invoke dangerous reflective methods
 * (e.g., classloader traversal, arbitrary process execution, or JVM system calls).
 *
 * @param message Diagnostic message explaining which member, class, or package triggered the security violation.
 * @see dev.siluet.reflect.SecurityPolicy
 * @see dev.siluet.reflect.DefaultSecurityPolicy
 */
class SiluetSecurityException(message: String) : RuntimeException(message) {
}
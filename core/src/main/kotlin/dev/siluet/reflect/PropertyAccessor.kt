package dev.siluet.reflect

/**
 * Strategy contract for resolving and reading properties from data model objects during expression evaluation.
 *
 * Siluet invokes this accessor whenever property navigation or dot-notation is encountered
 * in a template expression (e.g., `{{ user.name }}` or `{{ order.total }}`). Implementations decouple
 * property extraction mechanics—such as Java Bean getters, Kotlin properties, [Map] lookups, or
 * reflection-free code generation—from the core template runtime.
 *
 * @see CachedReflectionPropertyAccessor
 * @see SecurityPolicy
 */
interface PropertyAccessor {

    /**
     * Resolves and returns the value of [propertyName] on the given [target] object.
     *
     * Depending on the underlying implementation, this may inspect fields, invoke getter methods,
     * query map keys, or evaluate computed properties.
     *
     * Implementations must tell a property that does not exist apart from one that holds `null`:
     * return [PropertyLookup.Missing] when [target] has no such property (no matching getter, no such
     * key in a [Map]), and [PropertyLookup.Found] with a `null` value when the property exists but its
     * value is `null`.
     *
     * @param target The receiver object instance from which to read the property.
     * @param propertyName The identifier of the property, field, or key to resolve.
     * @return [PropertyLookup.Found] with the property value, or [PropertyLookup.Missing] if the property
     *         does not exist on [target].
     * @throws dev.siluet.exception.SiluetSecurityException If reading this property violates the active security policy.
     */
    fun getProperty(target: Any, propertyName: String): PropertyLookup
}
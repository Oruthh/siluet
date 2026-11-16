package dev.siluet.exception.internal

internal class PropertyAccessException(
    targetClass: Class<*>,
    propertyName: String,
    cause: Throwable,
) : RuntimeException(
    "Failed to read property '$propertyName' of '${targetClass.name}': " +
            (cause::class.java.simpleName + (cause.message?.let { ": $it" } ?: "")),
    cause
)
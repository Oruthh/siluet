package dev.siluet.exception

internal class ComponentViewNotFoundException(
    val componentName: String
): RuntimeException("Could not find view for component '$componentName'")

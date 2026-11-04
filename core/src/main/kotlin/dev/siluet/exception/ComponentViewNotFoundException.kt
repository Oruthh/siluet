package dev.siluet.exception

class ComponentViewNotFoundException(
    val componentName: String
): RuntimeException("Could not find view for component '$componentName'")

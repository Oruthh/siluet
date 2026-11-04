package dev.siluet.exception

internal class SectionsGroupNotFoundException(
    val sectionsGroup: String
): RuntimeException("Could not find sections group with name '$sectionsGroup'")

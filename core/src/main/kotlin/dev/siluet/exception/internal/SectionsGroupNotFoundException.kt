package dev.siluet.exception.internal

internal class SectionsGroupNotFoundException(
    val sectionsGroup: String
): RuntimeException("Could not find sections group with name '$sectionsGroup'")

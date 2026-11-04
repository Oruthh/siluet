package dev.siluet.exception

class SectionsGroupNotFoundException(
    val sectionsGroup: String
): RuntimeException("Could not find sections group with name '$sectionsGroup'")

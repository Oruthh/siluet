package dev.siluet.exception

internal class SectionViewNotFoundException(
    val sectionName: String
) : RuntimeException("Could not find view for section '$sectionName'") {
}
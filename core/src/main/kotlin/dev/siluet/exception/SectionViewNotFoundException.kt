package dev.siluet.exception

class SectionViewNotFoundException(
    val sectionName: String
) : RuntimeException("Could not find view for section '$sectionName'") {
}
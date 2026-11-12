package dev.siluet.parser

import dev.siluet.schema.SectionSchema

internal data class ParseResult(
    val ast: List<SiluetNode>,
    val sectionSchema: SectionSchema?
)

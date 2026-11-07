package dev.siluet.view

import dev.siluet.parser.SiluetNode
import dev.siluet.schema.SectionSchema

internal class SiluetSectionView(
    ast: List<SiluetNode>,
    val schema: SectionSchema? = null
) : SiluetView(ast)
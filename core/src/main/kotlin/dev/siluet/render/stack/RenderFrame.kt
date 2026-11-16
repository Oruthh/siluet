package dev.siluet.render.stack

import dev.siluet.SourceLocation

internal data class RenderFrame(
    val sourceName: String,
    val callSite: SourceLocation
) {
    fun isSameView(other: RenderFrame): Boolean =
        sourceName == other.sourceName
}
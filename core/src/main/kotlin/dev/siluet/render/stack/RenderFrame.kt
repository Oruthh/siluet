package dev.siluet.render.stack

import dev.siluet.SourceLocation

internal data class RenderFrame(
    val viewFilePath: String,
    val callSite: SourceLocation
) {
    fun isSameView(other: RenderFrame): Boolean =
        viewFilePath == other.viewFilePath
}
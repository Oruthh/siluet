package dev.siluet.exception.internal

import dev.siluet.render.stack.RenderFrame

internal class RenderDepthExceededException(
    val maxDepth: Int,
    val rejectedFrame: RenderFrame,
    val entryFrame: RenderFrame,
    val chain: List<RenderFrame>
) : RenderException("Maximum render depth ($maxDepth) exceeded while rendering '${rejectedFrame.sourceName}': ${buildChainMessage(rejectedFrame, entryFrame, chain)}", rejectedFrame.callSite) {
    private companion object {
        const val SEPARATOR = " → "

        fun buildChainMessage(rejectedFrame: RenderFrame, entryFrame: RenderFrame, chain: List<RenderFrame>): String {
            return buildString {
                append((chain + rejectedFrame).joinToString(SEPARATOR) { it.sourceName })
                append(" (entered from ")
                append(entryFrame.callSite)
                append(")")
            }
        }
    }
}
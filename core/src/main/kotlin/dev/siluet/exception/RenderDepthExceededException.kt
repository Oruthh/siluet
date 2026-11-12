package dev.siluet.exception

import dev.siluet.render.stack.RenderFrame

internal class RenderDepthExceededException(
    val maxDepth: Int,
    val rejectedFrame: RenderFrame,
    val entryFrame: RenderFrame,
    val chain: List<RenderFrame>
) : RenderException("Maximum render depth ($maxDepth) exceeded while rendering '${rejectedFrame.viewFilePath}': ${buildChainMessage(rejectedFrame, entryFrame, chain)}", rejectedFrame.callSite) {
    private companion object {
        const val SEPARATOR = " → "

        fun buildChainMessage(rejectedFrame: RenderFrame, entryFrame: RenderFrame, chain: List<RenderFrame>): String {
            return buildString {
                append((chain + rejectedFrame).joinToString(SEPARATOR) { it.viewFilePath })
                append(" (entered from ")
                append(entryFrame.callSite)
                append(")")
            }
        }
    }
}
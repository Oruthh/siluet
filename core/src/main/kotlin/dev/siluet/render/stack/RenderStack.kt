package dev.siluet.render.stack

import dev.siluet.SourceLocation
import dev.siluet.exception.RenderDepthExceededException

internal class RenderStack(
    private val maxDepth: Int,
) {
    private val stack = ArrayDeque<RenderFrame>()

    val depth: Int
        get() = stack.size

    init {
        require(maxDepth > 0) { "maxDepth must be greater than 0, but was $maxDepth" }
    }

    fun <T> withFrame(viewFilePath: String, callSite: SourceLocation, block: () -> T): T {
        val frame = RenderFrame(
            viewFilePath = viewFilePath,
            callSite = callSite
        )

        if(depth >= maxDepth) {
            val cycleStart = stack.indexOfLast { it.isSameView(frame) }
            val chain = if (cycleStart >= 0){
                stack.subList(cycleStart, stack.size).toList()
            } else {
                stack.toList()
            }

            val firstOccurrence = stack.indexOfFirst { it.isSameView(frame) }
            val entry = stack[maxOf(firstOccurrence, 0)]

            throw RenderDepthExceededException(maxDepth, frame, entry, chain)
        }

        stack.addLast(frame)

        try{
            return block()
        } finally {
            stack.removeLast()
        }
    }
}
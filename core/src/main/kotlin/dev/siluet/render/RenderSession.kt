package dev.siluet.render

import dev.siluet.render.stack.RenderStack
import dev.siluet.theme.SiluetTheme

internal data class RenderSession(
    val theme: SiluetTheme,
    val renderContext: RenderContext,
    val renderStack: RenderStack,
    val evaluationContext: EvaluationContext,
    val evaluator: ExpressionEvaluator,
    val buffer: Appendable,

)

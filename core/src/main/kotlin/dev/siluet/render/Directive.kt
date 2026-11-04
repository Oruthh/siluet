package dev.siluet.render

fun interface Directive {
    fun execute(
        args: List<Any?>,
        namedArgs: Map<String, Any?>,
        body: (() -> String)?,
        renderContext: RenderContext,
        out: Appendable)
}
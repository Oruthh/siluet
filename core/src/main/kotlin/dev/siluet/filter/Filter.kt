package dev.siluet.filter

import dev.siluet.render.RenderContext

fun interface Filter {
    fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any?
}
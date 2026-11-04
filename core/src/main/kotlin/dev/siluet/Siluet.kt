package dev.siluet

import dev.siluet.render.RenderContext

interface Siluet {
    fun render(templateName: String, model: Any = Unit, contextBuilder: RenderContext.Builder.() -> Unit = {}): String
    fun renderView(viewPath: String, model: Any = Unit, contextBuilder: RenderContext.Builder.() -> Unit = {}): String
    fun renderString(source: String, model: Any = Unit, contextBuilder: RenderContext.Builder.() -> Unit = {}): String

    companion object {
        operator fun invoke(block: SiluetBuilder.() -> Unit = {}): Siluet {
            val config = SiluetBuilder().apply(block)
            return DefaultSiluet(config)
        }
    }
}
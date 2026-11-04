package dev.siluet.plugin

import dev.siluet.SiluetBuilder

interface SiluetPlugin<TConfig: Any> {
    fun install(builder: SiluetBuilder, configure: TConfig.() -> Unit = {})
}
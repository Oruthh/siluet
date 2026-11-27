package dev.siluet.cache

internal fun interface CacheSizeListener {
    fun sizeChanged(delta: Int)
}
package dev.siluet.cache

import dev.siluet.parser.SiluetView

interface ViewCache {
    fun get(key: String): SiluetView?
    fun put(key: String, view: SiluetView)
    fun invalidate(key: String)
    fun invalidateAll()
}
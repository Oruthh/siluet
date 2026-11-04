package dev.siluet.render

interface PropertyAccessor {
    fun getProperty(target: Any, propertyName: String): Any?
}
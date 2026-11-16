package dev.siluet.exception.internal

internal class TemplateLoadingException(
    message: String,
    val sourceName: String
) : RuntimeException(message) {
}
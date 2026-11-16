package dev.siluet.exception.internal

internal class TemplateNotFoundException(val templateName: String): RuntimeException("Could not find template: $templateName") {
}
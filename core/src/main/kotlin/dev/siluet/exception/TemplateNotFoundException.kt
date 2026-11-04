package dev.siluet.exception

internal class TemplateNotFoundException(val templateName: String): RuntimeException("Could not find template: $templateName") {
}
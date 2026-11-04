package dev.siluet.exception

class TemplateNotFoundException(val templateName: String): RuntimeException("Could not find template: $templateName") {
}
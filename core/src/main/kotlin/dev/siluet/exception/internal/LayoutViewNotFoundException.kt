package dev.siluet.exception.internal

internal class LayoutViewNotFoundException(
    val layoutName: String
): RuntimeException("Could not find view for layout '$layoutName'")

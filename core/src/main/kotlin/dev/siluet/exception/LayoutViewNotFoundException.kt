package dev.siluet.exception

class LayoutViewNotFoundException(
    val layoutName: String
): RuntimeException("Could not find view for layout '$layoutName'")

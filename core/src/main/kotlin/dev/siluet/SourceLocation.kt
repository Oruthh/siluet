package dev.siluet

data class SourceLocation(
    val filePath: String,
    val line: Int,
    val column: Int,
) {
    override fun toString(): String = "$filePath:$line:$column"
}
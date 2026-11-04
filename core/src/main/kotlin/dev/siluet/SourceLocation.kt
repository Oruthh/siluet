package dev.siluet

/**
 * Represents a precise position within a template source file.
 *
 * Attached to AST nodes, tokens, and runtime evaluation errors to provide
 * actionable diagnostic messages and accurate stack traces pointing back
 * to the original template file.
 *
 * @property filePath Identifier or relative path of the source file (e.g., `"sections/header.html"`).
 * @property line The 1-based line number where the element originates.
 * @property column The 1-based character column position within the line.
 */
data class SourceLocation(
    val filePath: String,
    val line: Int,
    val column: Int,
) {
    /**
     * Formats the source position into a standard diagnostic string (`filePath:line:column`).
     *
     * Example output: `"layouts/main.html:14:8"`.
     *
     * @return Formatted location string compatible with IDE log consoles and compiler diagnostics.
     */
    override fun toString(): String = "$filePath:$line:$column"
}
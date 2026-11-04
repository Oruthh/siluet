package dev.siluet.parser

/**
 * Represents a pre-compiled, in-memory template view ready for rendering.
 *
 * A [SiluetView] encapsulates the compiled Abstract Syntax Tree ([ast]) generated
 * after lexical analysis and parsing. Instances of this class are stored within
 * [dev.siluet.cache.ViewCache] to bypass subsequent compilation passes when rendering
 * the same view or layout repeatedly.
 *
 * @property ast The ordered sequence of root-level [SiluetNode] elements composing the template AST.
 */
data class SiluetView(
    val ast: List<SiluetNode>,
)
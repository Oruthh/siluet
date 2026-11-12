package dev.siluet.view

import dev.siluet.parser.SiluetNode

/**
 * Represents a pre-compiled, in-memory template view ready for rendering.
 *
 * A [SiluetView] encapsulates the compiled Abstract Syntax Tree ([ast]) generated
 * after lexical analysis and parsing. Instances of this class are stored in a
 * [dev.siluet.cache.SiluetCache] to bypass subsequent compilation passes when rendering
 * the same view or layout repeatedly.
 *
 * @property ast The ordered sequence of root-level [dev.siluet.parser.SiluetNode] elements composing the template AST.
 */
open class SiluetView(
    val ast: List<SiluetNode>,
)
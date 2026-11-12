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
 * @property filePath Path of the source file this view was compiled from, relative to the theme root
 *   (e.g. `components/productCard.html`). Used in error messages and source locations.
 */
open class SiluetView(
    val ast: List<SiluetNode>,
    val filePath: String
)
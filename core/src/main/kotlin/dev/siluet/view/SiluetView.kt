package dev.siluet.view

import dev.siluet.parser.SiluetNode

/**
 * Represents a pre-compiled, in-memory template view ready for rendering.
 *
 * A [SiluetView] encapsulates the compiled Abstract Syntax Tree ([ast]) generated
 * after lexical analysis and parsing. Instances of this class are stored in a
 * [dev.siluet.cache.ThemeCache] to bypass subsequent compilation passes when rendering
 * the same view or layout repeatedly.
 *
 * @property ast The ordered sequence of root-level [dev.siluet.parser.SiluetNode] elements composing the template AST.
 * @property sourceName Source name of the asset this view was compiled from, as reported by the
 *   [dev.siluet.loader.SiluetAssetLoader] (e.g. `/theme/components/productCard.html`). Used in error messages,
 *   source locations and to tell views apart when detecting infinite recursion.
 */
internal open class SiluetView(
    val ast: List<SiluetNode>,
    val sourceName: String
)
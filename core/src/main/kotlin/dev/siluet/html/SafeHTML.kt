package dev.siluet.html

import java.util.stream.IntStream

/**
 * Trusted HTML that Siluet prints without escaping.
 *
 * Every value printed with `{{ }}` is HTML-escaped, except `SafeHTML`. A custom filter can return
 * it to output markup, e.g. a rendered Markdown text or an inline SVG icon. Built-in text filters
 * such as `uppercase` or `truncate` do not accept it, because changing HTML as plain text would break it.
 *
 * Create it with [asSafeHTML].
 */
class SafeHTML internal constructor(private val content: String) : CharSequence by content {
    /** Returns the HTML exactly as it was wrapped. */
    override fun toString(): String = content

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SafeHTML) return false
        return content == other.content
    }

    override fun hashCode(): Int = content.hashCode()
    override fun chars(): IntStream {
        return content.chars()
    }

    override fun codePoints(): IntStream {
        return content.codePoints()
    }
}

/**
 * Marks this text as trusted HTML that is printed without escaping.
 *
 * **Only wrap content you fully trust.** Siluet never escapes [SafeHTML], so wrapping user input
 * or any other untrusted value opens the page to cross-site scripting (XSS). Escape untrusted
 * parts with [HTMLEscape.escape] before combining them into the result.
 *
 * Returns the same instance when called on [SafeHTML].
 *
 * ### Example:
 * ```kotlin
 * filter("bold") { value, _, _ ->
 *     "<b>${HTMLEscape.escape(value)}</b>".asSafeHTML()
 * }
 * ```
 */
fun CharSequence.asSafeHTML(): SafeHTML = this as? SafeHTML ?: SafeHTML(this.toString())
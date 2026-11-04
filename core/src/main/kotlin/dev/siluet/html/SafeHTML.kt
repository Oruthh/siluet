package dev.siluet.html

import java.util.stream.IntStream

class SafeHTML(private val content: String) : CharSequence by content {
    override fun toString(): String = content

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SafeHTML) return false
        return content == other.content
    }

    override fun hashCode(): Int = content.hashCode()
    override fun chars(): IntStream? {
        return content.chars()
    }

    override fun codePoints(): IntStream? {
        return content.codePoints()
    }
}

fun CharSequence.asSafeHTML(): SafeHTML = this as? SafeHTML ?: SafeHTML(this.toString())
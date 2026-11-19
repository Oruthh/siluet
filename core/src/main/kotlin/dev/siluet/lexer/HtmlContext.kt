package dev.siluet.lexer

internal enum class HtmlContext(
    val directives: Boolean,
    val prints: Boolean
){
    CONTENT(true, true),
    COMMENT(true, true),
    TAG(true, true),
    ATTR_VALUE(false, true),
    RAW_TEXT(false, true)
}

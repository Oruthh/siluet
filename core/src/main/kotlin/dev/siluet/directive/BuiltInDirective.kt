package dev.siluet.directive

internal enum class BuiltInDirective(val keyword: String) {
    IF("if"), ELSE("else"), FOR("for"), COMPONENT("component"),
    SECTIONS("sections"), PAGINATE("paginate"), SCHEMA("schema");

    companion object {
        private val byKeyword = entries.associateBy { it.keyword }
        fun of(keyword: String): BuiltInDirective? = byKeyword[keyword]
        val keywords: Set<String> = byKeyword.keys
    }
}
package dev.siluet.lexer

internal class HtmlContextTracker(
    private val source: String
) {
    var context = HtmlContext.CONTENT
        private set
    var contextSwitchedAt: Int = -1
        private set

    private var resumeAt = -1
    private var lookaheadContext = HtmlContext.CONTENT

    private var tagStartIndex: Int = -1
    private var tagName: String = ""
    private var rawTextTagName: String = ""
    private var isEndTag: Boolean = false
    private var afterEquals: Boolean = false
    private var quote: Char? = null

    fun advance(currentIndex: Int): HtmlContext {
        if(resumeAt > currentIndex) {
            return lookaheadContext
        }

        val c = source[currentIndex]

        return when(context) {
            HtmlContext.CONTENT -> advanceContent(currentIndex, c)
            HtmlContext.COMMENT -> advanceComment(currentIndex, c)
            HtmlContext.TAG -> advanceTag(currentIndex, c)
            HtmlContext.ATTR_VALUE -> advanceAttributeValue(c)
            HtmlContext.RAW_TEXT -> advanceRawTextTag(currentIndex, c)
        }
    }

    fun unclosed(): UnclosedHtml? = when (context) {
        HtmlContext.CONTENT -> null
        HtmlContext.RAW_TEXT -> UnclosedHtml(
            "Unclosed <$rawTextTagName> element: missing </$rawTextTagName>", contextSwitchedAt
        )
        HtmlContext.ATTR_VALUE -> quote
            ?.let { UnclosedHtml("Unclosed attribute value: missing closing $it", contextSwitchedAt) }
            ?: UnclosedHtml("Unclosed tag: missing '>'", tagStartIndex)  // wartość bez cudzysłowu = niedomknięty tag
        HtmlContext.TAG -> UnclosedHtml("Unclosed tag: missing '>'", contextSwitchedAt)
        HtmlContext.COMMENT -> UnclosedHtml("Unclosed comment: missing '-->'", contextSwitchedAt)
    }

    private fun advanceContent(index: Int, c: Char): HtmlContext {
        if(c != '<'){
            return HtmlContext.CONTENT
        }

        if(source.startsWith("<!--", index)) {
            switchTo(HtmlContext.COMMENT, index)
            skipTo(HtmlContext.COMMENT, index + 4)
            return HtmlContext.COMMENT
        }

        val nextC = peek(index + 1)
        when {
            nextC.isLetter() -> openTag(index, index + 1, false)
            nextC == '/' -> openTag(index, index + 2, true)
            nextC == '!' -> openTag(index, index + 2, false) // <!DOCTYPE>
            else -> return HtmlContext.CONTENT
        }

        return HtmlContext.TAG
    }

    private fun advanceTag(index: Int, c: Char): HtmlContext {
        if(afterEquals && !c.isWhitespace()) {
            afterEquals = false

            when(c) {
                '"', '\'' -> {
                    quote = c
                    switchTo(HtmlContext.ATTR_VALUE, index)
                    return HtmlContext.ATTR_VALUE
                }
                '>' -> {

                }
                else -> {
                    quote = null
                    switchTo(HtmlContext.ATTR_VALUE, index)
                    return HtmlContext.ATTR_VALUE
                }
            }
        }
        when (c) {
            '=' -> afterEquals = true
            '>' -> closeTag()
        }
        return HtmlContext.TAG
    }

    private fun advanceAttributeValue(c: Char): HtmlContext {
        val quote = quote
        if(quote != null){
            if(quote == c){
                switchTo(HtmlContext.TAG, tagStartIndex)
            }
            return HtmlContext.ATTR_VALUE
        }

        return when {
            c.isWhitespace() -> {
                switchTo(HtmlContext.TAG, tagStartIndex)
                HtmlContext.TAG
            }
            c == '>' -> {
                closeTag()
                HtmlContext.TAG
            }
            else -> HtmlContext.ATTR_VALUE
        }
    }

    private fun advanceRawTextTag(index: Int, c: Char): HtmlContext {
        if (c == '<' && peek(index + 1) == '/' && isRawTextEndTagAt(index + 2)) {
            openTag(index, index + 2, isEndTag = true)
            return HtmlContext.TAG
        }
        return HtmlContext.RAW_TEXT
    }

    private fun advanceComment(index: Int, c: Char): HtmlContext {
        if (source.startsWith("-->", index)) {
            switchTo(HtmlContext.CONTENT, -1)
            skipTo(HtmlContext.COMMENT, index + 3)
        }
        return HtmlContext.COMMENT
    }

    private fun openTag(index: Int, nameStartIndex: Int, isEndTag: Boolean) {
        var nameEndIndex = nameStartIndex
        while(nameEndIndex < source.length && isTagNameChar(source[nameEndIndex])){
            nameEndIndex++
        }

        tagStartIndex = index
        tagName = source.substring(nameStartIndex, nameEndIndex)
        this.isEndTag = isEndTag

        skipTo(HtmlContext.TAG, nameEndIndex)
        switchTo(HtmlContext.TAG, index)
    }
    private fun closeTag() {
        if(!isEndTag && tagName.lowercase() in RAW_TEXT_ELEMENTS){
            rawTextTagName = tagName
            switchTo(HtmlContext.RAW_TEXT, tagStartIndex)
            return
        }
        switchTo(HtmlContext.CONTENT, -1)
    }

    private fun isRawTextEndTagAt(nameStart: Int): Boolean {
        val length = rawTextTagName.length
        if (!source.regionMatches(nameStart, rawTextTagName, 0, length, ignoreCase = true)) {
            return false
        }
        val after = peek(nameStart + length)
        return after == '>' || after == '/' || after.isWhitespace()
    }

    private fun skipTo(htmlContext: HtmlContext, until: Int){
        resumeAt = until
        lookaheadContext = htmlContext
    }

    private fun switchTo(htmlContext: HtmlContext, switchedAt: Int){
        context = htmlContext
        contextSwitchedAt = switchedAt
    }

    private fun peek(index: Int): Char = if (index < source.length) source[index] else '\u0000'

    private fun isTagNameChar(c: Char): Boolean = c.isLetterOrDigit() || c == '-' || c == ':' || c == '!'

    private companion object {
        val RAW_TEXT_ELEMENTS = setOf("style", "script")
    }
}
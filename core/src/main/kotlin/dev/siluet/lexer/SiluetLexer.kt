package dev.siluet.lexer

import dev.siluet.SourceLocation
import dev.siluet.directive.BuiltInDirective
import dev.siluet.exception.internal.LexerException

internal class SiluetLexer(
    private val source: String,
    private val filePath: String = "<inline>"
) {
    private val htmlContextTracker = HtmlContextTracker(source)

    private var cursor: Int = 0
    private var line: Int = 1
    private var column: Int = 1

    private enum class Mode {
        TEXT,
        EXPRESSION,
        DIRECTIVE
    }
    private var mode: Mode = Mode.TEXT
    private var braceDepth: Int = 0
    private var bracketDepth: Int = 0
    private var isExpressionOpen: Boolean = false

    private var directiveStartLine: Int = -1;
    private var directiveStartColumn: Int = -1;

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()

        while (!isAtEnd()) {
            when (mode) {
                Mode.TEXT -> scanText(tokens)
                Mode.EXPRESSION -> scanExpression(tokens)
                Mode.DIRECTIVE -> scanDirective(tokens)
            }
        }

        if(mode == Mode.DIRECTIVE) {
            val lastToken = tokens.last()
            throw LexerException(
                "'@${lastToken.value}' must be followed by '(' or '{'. Use '\\@' to print a literal '@'.",
                sourceLocation = SourceLocation(
                    filePath = filePath,
                    line = directiveStartLine,
                    column = directiveStartColumn + 1
                )
            )
        }

        val unclosedHtml = htmlContextTracker.unclosed()
        if(unclosedHtml != null){
            throw LexerException(
                unclosedHtml.message,
                sourceLocation = calculateSourceLocation(unclosedHtml.index)
            )
        }

        tokens.add(
            Token(
                type = TokenType.EOF,
                value = "",
                sourceLocation = SourceLocation(
                    filePath = filePath,
                    line = line,
                    column = column
                ),
            )
        )
        return tokens
    }

    private fun scanText(tokens: MutableList<Token>) {
        val startLine = line
        val startColumn = column
        val buffer = StringBuilder()

        while (!isAtEnd()) {
            val c = peek()

            val htmlContext = htmlContextTracker.advance(cursor)

            if(c == '\\' && htmlContext.directives) {
                val next = peek(1)
                if(next == '{' || next == '@' || next == '\\' || next == '}') {
                    advance()
                    buffer.append(advance())
                    continue
                }
            }

            if(c == '{' && peek(1) == '{' && htmlContext.prints) {
                flushTextToken(buffer, startLine, startColumn, tokens)
                val exprLine = line
                val exprColumn = column
                advance()
                advance()
                tokens.add(
                    Token(
                        type = TokenType.OPEN_EXPR,
                        value = "{{",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = exprLine,
                            column = exprColumn
                        )
                    )
                )
                mode = Mode.EXPRESSION
                isExpressionOpen = true
                return
            }

            if (c == '}' && peek(1) == '}' && isExpressionOpen && htmlContext.prints) {
                val braceLine = line
                val braceColumn = column
                advance()
                advance()
                tokens.add(
                    Token(
                        type = TokenType.CLOSE_EXPR,
                        value = "}}",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = braceLine,
                            column = braceColumn
                        )
                    )
                )
                isExpressionOpen = false
                return
            }

            if (c == '}' && braceDepth > 0 && htmlContext.directives) {
                flushTextToken(buffer, startLine, startColumn, tokens)
                val braceLine = line
                val braceColumn = column
                advance()
                braceDepth--
                tokens.add(
                    Token(
                        type = TokenType.RBRACE,
                        value = "}",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = braceLine,
                            column = braceColumn
                        )
                    )
                )
                return
            }

            if(c == '@' && peek(1).isLetter() && htmlContext.directives) {
                flushTextToken(buffer, startLine, startColumn, tokens)
                val atLine = line
                val atColumn = column
                advance()
                tokens.add(
                    Token(
                        type = TokenType.AT,
                        value = "@",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = atLine,
                            column = atColumn
                        )
                    )
                )
                mode = Mode.DIRECTIVE
                directiveStartLine = atLine
                directiveStartColumn = atColumn
                return
            }

            buffer.append(advance())
        }

        flushTextToken(buffer, startLine, startColumn, tokens)
    }

    private fun flushTextToken(
        buffer: StringBuilder,
        startLine: Int,
        startColumn: Int,
        tokens: MutableList<Token>
    ) {
        if(buffer.isNotEmpty()) {
            tokens.add(
                Token(
                    type = TokenType.TEXT,
                    value = buffer.toString(),
                    sourceLocation = SourceLocation(
                        filePath = filePath,
                        line = startLine,
                        column = startColumn
                    )
                )
            )
            buffer.setLength(0)
        }
    }

    private fun scanExpression(tokens: MutableList<Token>) {
        skipWhitespaces()
        if(isAtEnd()) {
            return
        }

        val tokenLine = line
        val tokenColumn = column
        val c = advance()

        when(c) {
            '}' -> {
                if(peek() == '}'){
                    advance()
                    tokens.add(
                        Token(
                            type = TokenType.CLOSE_EXPR,
                            value = "}}",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                    mode = Mode.TEXT
                    isExpressionOpen = false
                } else {
                    tokens.add(
                        Token(
                            type = TokenType.RBRACE,
                            value = "}",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                }
            }
            '{' -> {
                braceDepth++
                tokens.add(
                    Token(
                        type = TokenType.LBRACE,
                        value = "{",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                )
                mode = Mode.TEXT
            }
            '(' -> {
                bracketDepth++
                tokens.add(
                    Token(
                        type = TokenType.LPAREN,
                        value = "(",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                )
            }
            ')' -> {
                bracketDepth--
                tokens.add(
                    Token(
                        type = TokenType.RPAREN,
                        value = ")",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                )

                if(!isExpressionOpen && bracketDepth <= 0){
                    peekNonWhitespace()?.let { (nextChar, nextCharOffset) ->
                        if(nextChar != '{' || peek(nextCharOffset + 1) == '{') {
                            mode = Mode.TEXT
                        }
                    } ?: run {
                        mode = Mode.TEXT
                    }
                }
            }
            '|' -> {
                if(peek() == '|') {
                    advance()
                    tokens.add(
                        Token(
                            type = TokenType.OR,
                            value = "||",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                } else {
                    tokens.add(
                        Token(
                            type = TokenType.PIPE,
                            value = "|",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                }
            }
            '.' -> {
                tokens.add(
                    Token(
                        type = TokenType.DOT,
                        value = ".",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                )
            }
            ',' -> {
                tokens.add(
                    Token(
                        type = TokenType.COMMA,
                        value = ",",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                )
            }
            '=' -> {
                if(match('=')){
                    tokens.add(
                        Token(
                            type = TokenType.EQUALS,
                            value = "==",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                } else {
                    tokens.add(
                        Token(
                            type = TokenType.ASSIGN,
                            value = "=",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                }
            }
            '!' -> {
                if(match('=')){
                    tokens.add(
                        Token(
                            type = TokenType.NOT_EQUALS,
                            value = "!=",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                } else {
                    tokens.add(
                        Token(
                            type = TokenType.NOT,
                            value = "!",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                }
            }
            '<' -> {
                if(match('=')){
                    tokens.add(
                        Token(
                            type = TokenType.LESS_OR_EQUAL,
                            value = "<=",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                } else {
                    tokens.add(
                        Token(
                            type = TokenType.LESS_THAN,
                            value = "<",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                }
            }
            '>' -> {
                if(match('=')){
                    tokens.add(
                        Token(
                            type = TokenType.GREATER_OR_EQUAL,
                            value = ">=",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                } else {
                    tokens.add(
                        Token(
                            type = TokenType.GREATER_THAN,
                            value = ">",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                }
            }
            '&' -> {
                if(match('&')){
                    tokens.add(
                        Token(
                            type = TokenType.AND,
                            value = "&&",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    )
                } else {
                    throw LexerException(
                        "Single '&'. Did you mean '&&'?",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                }
            }
            '"', '\'' -> {
                scanStringLiteral(c, tokenLine, tokenColumn, tokens)
            }
            else -> {
                when {
                    c.isDigit() -> {
                        scanNumberLiteral(c, tokenLine, tokenColumn, tokens)
                    }
                    c.isLetter() -> {
                        scanIdentifierOrKeyword(c, tokenLine, tokenColumn, tokens)
                    }
                    else -> {
                        throw LexerException(
                            "Unexpected character '$c'",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        )
                    }
                }
            }
        }
    }

    private fun scanDirective(tokens: MutableList<Token>) {
        skipWhitespaces()
        if(isAtEnd()) {
            return
        }

        val tokenLine = line
        val tokenColumn = column
        val currentToken = tokens.last()
        val c = advance()

        when(c) {
            '{' -> {
                if(peek() == '{'){
                    throw LexerException(
                        "'@${currentToken.value}' must be followed by '(' or '{'. Use '\\@' to print a literal '@'.",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = directiveStartLine,
                            column = directiveStartColumn + 1
                        )
                    )
                }
                braceDepth++
                tokens.add(
                    Token(
                        type = TokenType.LBRACE,
                        value = "{",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                )
                mode = Mode.TEXT
            }
            '(' -> {
                bracketDepth++
                tokens.add(
                    Token(
                        type = TokenType.LPAREN,
                        value = "(",
                        sourceLocation = SourceLocation(
                            filePath = filePath,
                            line = tokenLine,
                            column = tokenColumn
                        )
                    )
                )
                mode = Mode.EXPRESSION
            }
            else -> {
                when {
                    c.isDigit() && currentToken.type == TokenType.AT -> {
                        scanNumberLiteral(c, tokenLine, tokenColumn, tokens)
                    }
                    c.isLetter() && currentToken.type == TokenType.AT -> {
                        scanIdentifierOrKeyword(c, tokenLine, tokenColumn, tokens)
                        if(tokens.last().value == BuiltInDirective.SCHEMA.keyword){
                            scanSchema(tokens)
                            mode = Mode.TEXT
                        }
                    }
                    else -> {
                        throw LexerException(
                            "'@${currentToken.value}' must be followed by '(' or '{'. Use '\\@' to print a literal '@'.",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = directiveStartLine,
                                column = directiveStartColumn + 1
                            )
                        )
                    }
                }
            }
        }
    }

    private fun scanSchema(tokens: MutableList<Token>) {
        skipWhitespaces()
        if(isAtEnd()) {
            return
        }

        var depth = 0

        val startLine = line
        val startColumn = column

        val buffer = StringBuilder()

        var inQuotes = false

        while(!isAtEnd()) {
            val tokenLine = line
            val tokenColumn = column

            val c = advance()

            when(c) {
                '{' -> {
                    if(inQuotes) {
                        buffer.append(c)
                        continue
                    }
                    if(depth == 0) {
                        flushTextToken(buffer, startLine, startColumn, tokens)
                        tokens.add(Token(
                            type = TokenType.LBRACE,
                            value = "{",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        ))
                    } else {
                        buffer.append(c)
                    }
                    depth++
                }
                '}' -> {
                    if(inQuotes) {
                        buffer.append(c)
                        continue
                    }
                    depth--
                    if(depth == 0) {
                        flushTextToken(buffer, startLine, startColumn, tokens)
                        tokens.add(Token(
                            type = TokenType.RBRACE,
                            value = "}",
                            sourceLocation = SourceLocation(
                                filePath = filePath,
                                line = tokenLine,
                                column = tokenColumn
                            )
                        ))
                        break
                    } else {
                        buffer.append(c)
                    }
                }
                '\\' -> {
                    buffer.append(c)
                    if(!isAtEnd()) {
                        val nextChar = advance()
                        buffer.append(nextChar)
                    }
                }
                '"' -> {
                    inQuotes = !inQuotes
                    buffer.append(c)
                }
                else -> {
                    buffer.append(c)
                }
            }
        }

        flushTextToken(buffer, startLine, startColumn, tokens)
    }

    private fun scanStringLiteral(quote: Char, tokenLine: Int, tokenColumn: Int, tokens: MutableList<Token>){
        val buffer = StringBuilder()
        while(!isAtEnd() && peek() != quote) {
            val c = advance()
            if(c == '\\' && !isAtEnd()) {
                when(val escaped = advance()) {
                    'n' -> buffer.append('\n')
                    't' -> buffer.append('\t')
                    'r' -> buffer.append('\r')
                    '\\' -> buffer.append('\\')
                    else -> buffer.append(escaped)
                }
            } else {
                buffer.append(c)
            }
        }

        if(isAtEnd()){
            throw LexerException(
                "Unterminated string literal",
                sourceLocation = SourceLocation(
                    filePath = filePath,
                    line = tokenLine,
                    column = tokenColumn
                )
            )
        }

        advance()
        tokens.add(
            Token(
                type = TokenType.STRING_LITERAL,
                value = buffer.toString(),
                sourceLocation = SourceLocation(
                    filePath = filePath,
                    line = tokenLine,
                    column = tokenColumn
                )
            )
        )
    }

    private fun scanNumberLiteral(firstChar: Char, tokenLine: Int, tokenColumn: Int, tokens: MutableList<Token>){
        val buffer = StringBuilder().append(firstChar)
        while(!isAtEnd() && (peek().isDigit() || peek() == '.')) {
            if(peek() == '.' && peek(1) == '.') {
                break
            }
            buffer.append(advance())
        }
        tokens.add(
            Token(
                type = TokenType.NUMBER_LITERAL,
                value = buffer.toString(),
                sourceLocation = SourceLocation(
                    filePath = filePath,
                    line = tokenLine,
                    column = tokenColumn
                )
            )
        )
    }

    private fun scanIdentifierOrKeyword(firstChar: Char, tokenLine: Int, tokenColumn: Int, tokens: MutableList<Token>){
        val buffer = StringBuilder().append(firstChar)
        while(!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            buffer.append(advance())
        }

        val text = buffer.toString()
        val tokenType = when(text) {
            "in" -> TokenType.KEYWORD_IN
            "by" -> TokenType.KEYWORD_BY
            "as" -> TokenType.KEYWORD_AS
            "true", "false" -> TokenType.BOOLEAN_LITERAL
            "null" -> TokenType.NULL_LITERAL
            else -> TokenType.IDENTIFIER
        }

        tokens.add(
            Token(
                type = tokenType,
                value = text,
                sourceLocation = SourceLocation(
                    filePath = filePath,
                    line = tokenLine,
                    column = tokenColumn
                )
            )
        )
    }

    private fun peek(offset: Int = 0): Char {
        val target = cursor + offset
        return if (target >= source.length) {
            '\u0000'
        } else {
            source[target]
        }
    }

    private fun peekNonWhitespace(offset: Int = 0): Pair<Char, Int>? {
        var offset = offset
        while (cursor + offset < source.length) {
            val char = source[cursor + offset]
            if(!char.isWhitespace()) {
                return char to offset
            }
            offset++
        }
        return null
    }

    private fun match(char: Char): Boolean {
        if(isAtEnd() || source[cursor] != char) {
            return false
        }
        advance()
        return true
    }

    private fun advance(): Char {
        val char = source[cursor++]
        if(char == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return char
    }

    private fun skipWhitespaces() {
        while (!isAtEnd() && peek().isWhitespace()) {
            advance()
        }
    }

    private fun isAtEnd(): Boolean {
        return cursor >= source.length
    }

    private fun calculateSourceLocation(index: Int): SourceLocation {
        val before = source.substring(0, index)
        val line = before.count { it == '\n' } + 1
        val column = index - before.lastIndexOf('\n')

        return SourceLocation(
            filePath = filePath,
            line = line,
            column = column
        )
    }
}
package dev.siluet.lexer

import dev.siluet.SourceLocation
import dev.siluet.exception.LexerException

internal class SiluetLexer(
    private val source: String,
    private val filePath: String = "<inline>"
) {
    private var cursor: Int = 0
    private var line: Int = 1
    private var column: Int = 1

    private enum class Mode { TEXT, EXPRESSION }
    private var mode: Mode = Mode.TEXT
    private var braceDepth: Int = 0
    private var isExpressionOpen: Boolean = false

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()

        while (!isAtEnd()) {
            when (mode) {
                Mode.TEXT -> scanText(tokens)
                Mode.EXPRESSION -> scanExpression(tokens)
            }
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
        val buffer = StringBuffer()

        while (!isAtEnd()) {
            val c = peek()

            if(c == '\\') {
                val next = peek(1)
                if(next == '{' || next == '@' || next == '\\' || next == '}') {
                    advance()
                    buffer.append(advance())
                    continue
                }
            }

            if(c == '{' && peek(1) == '{') {
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

            if(c == '@' && peek(1).isLetter()) {
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
                mode = Mode.EXPRESSION
                return
            }

            if (c == '}' && peek(1) == '}' && isExpressionOpen) {
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

            if (c == '}' && braceDepth > 0) {
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

            buffer.append(advance())
        }

        flushTextToken(buffer, startLine, startColumn, tokens)
    }

    private fun flushTextToken(
        buffer: StringBuffer,
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

                peekNonWhitespace()?.let { nextChar ->
                    if(nextChar != '{') {
                        mode = Mode.TEXT
                    }
                } ?: run {
                    mode = Mode.TEXT
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
                            type = TokenType.GREATER_THAN,
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
            '&' -> {
                if(match('&')){
                    tokens.add(
                        Token(
                            type = TokenType.AND,
                            value = "<=",
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
    private fun peekNonWhitespace(offset: Int = 0): Char? {
        var offset = 0
        while (cursor + offset < source.length) {
            val char = source[cursor + offset]
            if(!char.isWhitespace()) {
                return char
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
}
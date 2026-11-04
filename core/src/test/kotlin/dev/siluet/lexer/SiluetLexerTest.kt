package dev.siluet.lexer

import dev.siluet.SourceLocation
import dev.siluet.exception.LexerException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SiluetLexerTest {
    private fun tokenize(source: String): List<Token> = SiluetLexer(source).tokenize()

    @Test
    fun `should tokenize plain text without expressions`() {
        val input = "Hello, world! This is static text."
        val tokens = tokenize(input)

        val expected = listOf(
            Token(TokenType.TEXT, "Hello, world! This is static text.", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 1
            )),
            Token(TokenType.EOF, "", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 35
            ))
        )
        assertTokensMatch(expected, tokens)
    }

    @Test
    fun `should tokenize print expression with identifier and property access`() {
        val input = "Hello {{ user.name }}!"
        val tokens = tokenize(input)

        val expected = listOf(
            Token(TokenType.TEXT, "Hello ", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 1
            )),
            Token(TokenType.OPEN_EXPR, "{{", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 7
            )),
            Token(TokenType.IDENTIFIER, "user", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 9
            )),
            Token(TokenType.DOT, ".", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 13
            )),
            Token(TokenType.IDENTIFIER, "name", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 14
            )),
            Token(TokenType.CLOSE_EXPR, "}}", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 19
            )),
            Token(TokenType.TEXT, "!", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 21
            )),
            Token(TokenType.EOF, "", sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 22
            ))
        )
        assertTokensMatch(expected, tokens)
    }

    @Test
    fun `should tokenize pipe expression with filter`() {
        val input = "{{ amount | money }}"
        val tokens = tokenize(input)

        val expectedTypes = listOf(
            TokenType.OPEN_EXPR,
            TokenType.IDENTIFIER,
            TokenType.PIPE,
            TokenType.IDENTIFIER,
            TokenType.CLOSE_EXPR,
            TokenType.EOF
        )
        assertEquals(expectedTypes, tokens.map { it.type })
        assertEquals("amount", tokens[1].value)
        assertEquals("money", tokens[3].value)
    }

    @Test
    fun `should tokenize if directive with comparison and body`() {
        val input = "@if(isActive == true) { <span>Active</span> }"
        val tokens = tokenize(input)

        val expectedTypes = listOf(
            TokenType.AT,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.IDENTIFIER,
            TokenType.EQUALS,
            TokenType.BOOLEAN_LITERAL,
            TokenType.RPAREN,
            TokenType.LBRACE,
            TokenType.TEXT,
            TokenType.RBRACE,
            TokenType.EOF
        )
        assertEquals(expectedTypes, tokens.map { it.type })
        assertEquals("isActive", tokens[3].value)
        assertEquals("true", tokens[5].value)
        assertEquals(" <span>Active</span> ", tokens[8].value)
    }

    @Test
    fun `should tokenize for loop directive with in keyword and nested print`() {
        val input = "@for(item in items) { ID: {{ item.id }} }"
        val tokens = tokenize(input)

        print(tokens)

        val expectedTypes = listOf(
            TokenType.AT,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.IDENTIFIER,
            TokenType.KEYWORD_IN,
            TokenType.IDENTIFIER,
            TokenType.RPAREN,
            TokenType.LBRACE,
            TokenType.TEXT,
            TokenType.OPEN_EXPR,
            TokenType.IDENTIFIER,
            TokenType.DOT,
            TokenType.IDENTIFIER,
            TokenType.CLOSE_EXPR,
            TokenType.TEXT,
            TokenType.RBRACE,
            TokenType.EOF
        )
        assertEquals(expectedTypes, tokens.map { it.type })
        assertEquals("item", tokens[3].value)
        assertEquals("in", tokens[4].value)
        assertEquals("items", tokens[5].value)
        assertEquals(" ID: ", tokens[8].value)
        assertEquals(" ", tokens[14].value)
    }

    @Test
    fun `should tokenize standalone inline directive without body`() {
        val input = "Before @custom(payload) After"
        val tokens = tokenize(input)

        val expectedTypes = listOf(
            TokenType.TEXT,
            TokenType.AT,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.IDENTIFIER,
            TokenType.RPAREN,
            TokenType.TEXT,
            TokenType.EOF
        )
        assertEquals(expectedTypes, tokens.map { it.type })
        assertEquals("Before ", tokens[0].value)
        assertEquals("custom", tokens[2].value)
        assertEquals("payload", tokens[4].value)
        assertEquals(" After", tokens[6].value)
    }

    @Test
    fun `should preserve escaped braces and at-signs as raw text`() {
        val input = "Escaped \\{{ test }} and \\@directive and \\\\"
        val tokens = tokenize(input)

        // Escaped markers should not trigger expression mode
        assertEquals(2, tokens.size)
        assertEquals(TokenType.TEXT, tokens[0].type)
        assertEquals("Escaped {{ test }} and @directive and \\", tokens[0].value)
        assertEquals(TokenType.EOF, tokens[1].type)
    }

    @Test
    fun `should tokenize string and number literals inside expressions`() {
        val input = "{{ calculate(42.5, \"test\\nvalue\") }}"
        val tokens = tokenize(input)

        val expectedTypes = listOf(
            TokenType.OPEN_EXPR,
            TokenType.IDENTIFIER,
            TokenType.LPAREN,
            TokenType.NUMBER_LITERAL,
            TokenType.COMMA,
            TokenType.STRING_LITERAL,
            TokenType.RPAREN,
            TokenType.CLOSE_EXPR,
            TokenType.EOF
        )
        assertEquals(expectedTypes, tokens.map { it.type })
        assertEquals("42.5", tokens[3].value)
        assertEquals("test\nvalue", tokens[5].value)
    }

    @Test
    fun `should throw LexerException on unterminated string literal`() {
        val input = "{{ \"unclosed string }}"

        assertThrows(LexerException::class.java) {
            tokenize(input)
        }
    }

    private fun assertTokensMatch(expected: List<Token>, actual: List<Token>) {
        assertEquals(expected.size, actual.size, "Token list size mismatch")
        for (i in expected.indices) {
            val exp = expected[i]
            val act = actual[i]
            assertEquals(
                exp.type,
                act.type,
                "Type mismatch at index $i (expected: ${exp.type}, actual: ${act.type})"
            )
            assertEquals(
                exp.value,
                act.value,
                "Value mismatch at index $i (expected: '${exp.value}', actual: '${act.value}')"
            )
        }
    }
}
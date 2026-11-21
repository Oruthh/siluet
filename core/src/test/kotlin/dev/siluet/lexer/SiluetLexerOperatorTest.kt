package dev.siluet.lexer

import dev.siluet.exception.internal.ParseException
import dev.siluet.parser.SiluetParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Token values of expression operators (issue #28).
 *
 * Token values end up in parser error messages (`Found '…'`), so every operator token
 * must carry exactly the text that appears in the template source.
 */
class SiluetLexerOperatorTest {

    private fun lex(source: String): List<Token> = SiluetLexer(source, "views/test.html").tokenize()

    private val operators = listOf(
        TokenType.EQUALS to "==",
        TokenType.NOT_EQUALS to "!=",
        TokenType.LESS_THAN to "<",
        TokenType.LESS_OR_EQUAL to "<=",
        TokenType.GREATER_THAN to ">",
        TokenType.GREATER_OR_EQUAL to ">=",
        TokenType.AND to "&&",
        TokenType.OR to "||",
    )

    @Test
    fun `every binary operator token carries its source text`() {
        operators.forEach { (type, text) ->
            val tokens = lex("{{ a $text b }}")

            assertEquals(
                listOf(
                    TokenType.OPEN_EXPR to "{{",
                    TokenType.IDENTIFIER to "a",
                    type to text,
                    TokenType.IDENTIFIER to "b",
                    TokenType.CLOSE_EXPR to "}}",
                    TokenType.EOF to "",
                ),
                tokens.map { it.type to it.value },
                "Token stream mismatch for operator '$text'"
            )
        }
    }

    @Test
    fun `operators are tokenized the same way without surrounding whitespace`() {
        operators.forEach { (type, text) ->
            val operator = lex("{{a${text}b}}").single { it.type == type }

            assertEquals(text, operator.value, "Wrong value for $type")
        }
    }

    @Test
    fun `not operator carries its source text`() {
        val not = lex("{{ !a }}").single { it.type == TokenType.NOT }

        assertEquals("!", not.value)
    }

    @Test
    fun `operator token location points at the operator`() {
        val operator = lex("{{ a >= b }}").single { it.type == TokenType.GREATER_OR_EQUAL }

        assertEquals(1, operator.sourceLocation.line)
        assertEquals(6, operator.sourceLocation.column)
    }

    @Test
    fun `parser error message shows the operator as written in the template`() {
        listOf(">", ">=", "&&").forEach { text ->
            val tokens = lex("@for(item $text items) { x }")

            val exception = assertFailsWith<ParseException>("No error for operator '$text'") {
                SiluetParser(tokens).parse()
            }

            assertTrue(
                exception.message!!.contains("Found '$text'."),
                "Expected the message to quote '$text', but was: ${exception.message}"
            )
        }
    }
}

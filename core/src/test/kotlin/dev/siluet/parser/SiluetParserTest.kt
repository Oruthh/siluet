package dev.siluet.parser

import dev.siluet.SourceLocation
import dev.siluet.exception.ParseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import dev.siluet.lexer.SiluetLexer

class SiluetParserTest {

    private fun parse(source: String): List<SiluetNode> {
        val tokens = SiluetLexer(source).tokenize()
        return SiluetParser(tokens).parse()
    }

    @Test
    fun `should parse plain static text`() {
        val ast = parse("Hello World")

        assertEquals(1, ast.size)
        assertEquals(TextNode(
            "Hello World",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 1
            )
        ), ast[0])
    }

    @Test
    fun `should parse print node with nested property access`() {
        val ast = parse("{{ user.address.street }}")

        assertEquals(1, ast.size)
        val printNode = assertInstanceOf(PrintNode::class.java, ast[0])

        // user.address.street -> PropertyAccess(PropertyAccess(user, "address"), "street")
        val streetAccess = assertInstanceOf(PropertyAccessNode::class.java, printNode.expression)
        assertEquals("street", streetAccess.propertyName)

        val addressAccess = assertInstanceOf(PropertyAccessNode::class.java, streetAccess.receiver)
        assertEquals("address", addressAccess.propertyName)

        val userVar = assertInstanceOf(VariableNode::class.java, addressAccess.receiver)
        assertEquals("user", userVar.name)
    }

    @Test
    fun `should parse print node with chained and parameterized filters`() {
        val ast = parse("{{ price | round | currency(\"USD\", true) }}")

        assertEquals(1, ast.size)
        val printNode = assertInstanceOf(PrintNode::class.java, ast[0])

        assertEquals(VariableNode(
            "price",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 4
            )
        ), printNode.expression)
        assertEquals(2, printNode.filters.size)

        // First filter: round
        val filter1 = printNode.filters[0]
        assertEquals("round", filter1.name)
        assertEquals(0, filter1.arguments.size)

        // Second filter: currency("USD", true)
        val filter2 = printNode.filters[1]
        assertEquals("currency", filter2.name)
        assertEquals(2, filter2.arguments.size)
        assertEquals(LiteralNode(
            "USD",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 29
            )
        ), filter2.arguments[0])
        assertEquals(LiteralNode(
            true,
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 36
            )
        ), filter2.arguments[1])
    }

    @Test
    fun `should parse if directive with comparison and else block`() {
        val ast = parse("@if(count > 0) { Positive } @else { Zero }")
        println(ast)

        assertEquals(1, ast.size)
        val ifNode = assertInstanceOf(IfNode::class.java, ast[0])

        // Verify condition: count > 0
        val condition = assertInstanceOf(BinaryOpNode::class.java, ifNode.condition)
        assertEquals(VariableNode(
            "count",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 5
            )
        ), condition.left)
        assertEquals(BinaryOperator.GREATER_THAN, condition.operator)
        assertEquals(LiteralNode(
            0.0,
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 13
            )
        ), condition.right)

        // Verify then and else bodies
        assertEquals(listOf(TextNode(
            " Positive ",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 17
            )
        )), ifNode.thenBody)
        assertEquals(listOf(TextNode(
            " Zero ",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 36
            )
        )), ifNode.elseBody)
    }

    @Test
    fun `should parse for loop with inner print statement`() {
        val ast = parse("@for(item in items) { ID: {{ item.id }} }")

        assertEquals(1, ast.size)
        val forNode = assertInstanceOf(ForNode::class.java, ast[0])

        assertEquals("item", forNode.variableName)
        assertEquals(VariableNode(
            "items",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 14
            )
        ), forNode.collectionExpression)

        assertEquals(3, forNode.body.size)
        assertEquals(TextNode(
            " ID: ",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 22
            )
        ), forNode.body[0])

        val innerPrint = assertInstanceOf(PrintNode::class.java, forNode.body[1])
        val propAccess = assertInstanceOf(PropertyAccessNode::class.java, innerPrint.expression)
        assertEquals(VariableNode(
            "item",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 30
            )
        ), propAccess.receiver)
        assertEquals("id", propAccess.propertyName)

        assertEquals(TextNode(
            " ",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 40
            )
        ), forNode.body[2])
    }

    @Test
    fun `should parse custom inline and block directives`() {
        val ast = parse("@inline(123) and @block(item) { Body: {{ item }} }")

        assertEquals(3, ast.size)

        // 1. Inline directive
        val inlineDirective = assertInstanceOf(DirectiveNode::class.java, ast[0])
        assertEquals("inline", inlineDirective.name)
        assertEquals(listOf(LiteralNode(
            123.0,
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 9
            )
        )), inlineDirective.arguments)
        assertNull(inlineDirective.body)

        // 2. Middle text
        assertEquals(TextNode(
            " and ",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 13
            )
        ), ast[1])

        // 3. Block directive
        val blockDirective = assertInstanceOf(DirectiveNode::class.java, ast[2])

        assertEquals("block", blockDirective.name)
        assertEquals(listOf(VariableNode(
            "item",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 25
            )
        )), blockDirective.arguments)
        val body = blockDirective.body
        assertEquals(3, body?.size)
        assertEquals(TextNode(
            " Body: ",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 32
            )
        ), body?.get(0))
        assertInstanceOf(PrintNode::class.java, body?.get(1))
        assertEquals(TextNode(
            " ",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 49
            )
        ), body?.get(2))
    }

    @Test
    fun `should enforce operator precedence in logical expressions`() {
        // Condition: a == b && c > 10 || !d
        val ast = parse("@if(a == b && c > 10 || !d) {}")

        val ifNode = assertInstanceOf(IfNode::class.java, ast[0])

        // Root operator must be || (lowest precedence)
        val rootOr = assertInstanceOf(BinaryOpNode::class.java, ifNode.condition)
        assertEquals(BinaryOperator.OR, rootOr.operator)

        // Right side of || should be !d
        val unaryNot = assertInstanceOf(UnaryOpNode::class.java, rootOr.right)
        assertEquals(UnaryOperator.NOT, unaryNot.operator)
        assertEquals(VariableNode(
            "d",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 26
            )
        ), unaryNot.operand)

        // Left side of || should be &&
        val andNode = assertInstanceOf(BinaryOpNode::class.java, rootOr.left)
        assertEquals(BinaryOperator.AND, andNode.operator)

        // Left of &&: a == b
        val equalsNode = assertInstanceOf(BinaryOpNode::class.java, andNode.left)
        assertEquals(BinaryOperator.EQUALS, equalsNode.operator)
        assertEquals(VariableNode(
            "a",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 5
            )
        ), equalsNode.left)
        assertEquals(VariableNode(
            "b",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 10
            )
        ), equalsNode.right)

        // Right of &&: c > 10
        val gtNode = assertInstanceOf(BinaryOpNode::class.java, andNode.right)
        assertEquals(BinaryOperator.GREATER_THAN, gtNode.operator)
        assertEquals(VariableNode(
            "c",
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 15
            )
        ), gtNode.left)
        assertEquals(LiteralNode(
            10.0,
            sourceLocation = SourceLocation(
                filePath = "<inline>",
                line = 1,
                column = 19
            )
        ), gtNode.right)
    }

    @Test
    fun `should throw ParseException on unclosed block`() {
        val input = "@if(isValid) { Never closed"

        assertThrows(ParseException::class.java) {
            parse(input)
        }
    }

    @Test
    fun `should throw ParseException on missing print expression closing delimiter`() {
        val input = "{{ user.name"

        assertThrows(ParseException::class.java) {
            parse(input)
        }
    }

    @Test
    fun `should throw ParseException on invalid for-loop syntax`() {
        val input = "@for(item items) {}"

        assertThrows(ParseException::class.java) {
            parse(input)
        }
    }
}
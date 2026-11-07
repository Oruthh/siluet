package dev.siluet.parser

import dev.siluet.SourceLocation
import dev.siluet.exception.ParseException
import dev.siluet.lexer.Token
import dev.siluet.lexer.TokenType
import dev.siluet.schema.SectionSchema
import dev.siluet.view.SiluetSectionView
import dev.siluet.view.SiluetView
import kotlinx.serialization.json.Json

internal class SiluetParser(
    private val tokens: List<Token>,
) {
    private var sectionSchema: SectionSchema? = null

    private var current = 0

    fun parseAsView(): SiluetView {
        return SiluetView(
            ast = parseAsNodeList()
        )
    }

    fun parseAsSectionView(): SiluetSectionView {
        return SiluetSectionView(
            ast = parseAsNodeList(),
            schema = sectionSchema
        )
    }

    fun parseAsNodeList(): List<SiluetNode> {
        current = 0;

        val nodes = mutableListOf<SiluetNode>()
        while(!isAtEnd()) {
            parseNode()?.let {
                nodes.add(it)
            }
        }

        return nodes
    }

    private fun parseNode(): SiluetNode? {
        return when {
            match(TokenType.TEXT) -> {
                val previous = previous()
                TextNode(
                    text = previous.value,
                    sourceLocation = previous.sourceLocation,
                )
            }
            match(TokenType.OPEN_EXPR) -> parsePrintNode()
            match(TokenType.AT) -> parseDirective()
            else -> {
                if(!isAtEnd()) {
                    advance()
                }
                null
            }
        }
    }

    private fun parsePrintNode(): PrintNode {
        val expr = parseExpression()
        val filters = mutableListOf<FilterCallNode>()

        while(match(TokenType.PIPE)) {
            val filterNameIdentifier = consume(TokenType.IDENTIFIER, "Expected filter name after '|'")
            val args = mutableListOf<ExpressionNode>()

            if(match(TokenType.LPAREN)) {
                if(!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression())
                    } while(match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Expected ')' after filter arguments")
            }
            filters.add(FilterCallNode(
                name = filterNameIdentifier.value,
                arguments = args,
                sourceLocation = filterNameIdentifier.sourceLocation
            ))
        }

        consume(TokenType.CLOSE_EXPR, "Expected '}}' to close print expression")
        return PrintNode(
            expression = expr,
            filters = filters,
            sourceLocation = expr.sourceLocation
        )
    }

    private fun parseDirective(): SiluetNode? {
        val nameToken = consume(TokenType.IDENTIFIER, "Expected directive keyword after '@'")
        val sourceLocation = nameToken.sourceLocation

        return when (nameToken.value) {
            "if" -> parseIfDirective(sourceLocation)
            "for" -> parseForDirective(sourceLocation)
            "component" -> parseComponentDirective(sourceLocation)
            "sections" -> parseSectionsComponent(sourceLocation)
            "paginate" -> parsePaginateDirective(sourceLocation)
            "t", "translate" -> parseTranslationDirective(nameToken.value, sourceLocation)
            "schema" -> {
                parseSchemaDirective(sourceLocation)
                null
            }
            else -> parseCustomDirective(nameToken.value, sourceLocation)
        }
    }

    private fun parseIfDirective(
        sourceLocation: SourceLocation,
    ): IfNode {
        consume(TokenType.LPAREN, "Expected '(' after '@if'")
        val condition = parseExpression()
        consume(TokenType.RPAREN, "Expected ')' after condition")

        consume(TokenType.LBRACE, "Expected '{' to start '@if' block")
        val thenBody = parseBlock()

        var elseBody: List<SiluetNode>? = null
        if(checkDirective("else")) {
            advance()
            advance()
            consume(TokenType.LBRACE, "Expected '{' after '@else'")
            elseBody = parseBlock()
        }

        return IfNode(
            condition = condition,
            thenBody = thenBody,
            elseBody = elseBody,
            sourceLocation = sourceLocation
        )
    }

    private fun parseForDirective(
        sourceLocation: SourceLocation
    ): ForNode {
        consume(TokenType.LPAREN, "Expected '(' after '@for'")
        val loopVar = consume(TokenType.IDENTIFIER, "Expected loop variable name").value
        consume(TokenType.KEYWORD_IN, "Expected 'in' keyword after variable name")
        val collection = parseExpression()
        consume(TokenType.RPAREN, "Expected ')' after loop definition")

        consume(TokenType.LBRACE, "Expected '{' to start '@for' body")
        val body = parseBlock()

        return ForNode(
            variableName = loopVar,
            collectionExpression = collection,
            body = body,
            sourceLocation = sourceLocation
        )
    }

    private fun parseComponentDirective(
        sourceLocation: SourceLocation
    ): ComponentNode {
        consume(TokenType.LPAREN, "Expected '(' after '@component'")
        val nameExpression = parseExpression()

        val props = mutableMapOf<String, ExpressionNode>()

        while(match(TokenType.COMMA)){
            if (check(TokenType.IDENTIFIER) && peek(1).type == TokenType.ASSIGN) {
                val keyToken = advance()
                advance() // Consume ASSIGN
                val valueExpr = parseExpression()
                props[keyToken.value] = valueExpr
            } else {
                throw ParseException(
                    "Positional argument are not allowed in @component",
                    sourceLocation = sourceLocation,
                )
            }
        }

        consume(TokenType.RPAREN, "Expected ')' after @component arguments")

        return ComponentNode(
            componentName = nameExpression,
            properties = props,
            sourceLocation = sourceLocation
        )
    }

    private fun parseSectionsComponent(
        sourceLocation: SourceLocation
    ): SectionsNode {
        consume(TokenType.LPAREN, "Expected '(' after '@sections'")

        val nameExpression = try {
            parseExpression()
        } catch (_: ParseException) {
            null
        }

        consume(TokenType.RPAREN, "Expected ')' after @component arguments")
        
        return SectionsNode(
            sectionsGroupName = nameExpression,
            sourceLocation = sourceLocation
        )
    }

    private fun parsePaginateDirective(
        sourceLocation: SourceLocation
    ): PaginateNode {
        consume(TokenType.LPAREN, "Expected '(' after '@paginate'")
        val collectionExpression = parseExpression()
        consume(TokenType.KEYWORD_AS, "Expected 'as' after collection in '@paginate'")
        val variableName = consume(TokenType.IDENTIFIER, "Expected variable name").value
        consume(TokenType.KEYWORD_BY, "Expected 'by' after variable name in '@paginate'")
        val pageSizeExpression = parseExpression()

        var pageNumberExpression: ExpressionNode? = null

        while(match(TokenType.COMMA)){
            if (check(TokenType.IDENTIFIER) && peek(1).type == TokenType.ASSIGN) {
                val keyToken = advance()
                if(keyToken.value != "page"){
                    throw ParseException(
                        "Unknown '${keyToken.value}' argument in '@paginate'",
                        sourceLocation = sourceLocation
                    )
                }
                advance() // Consume ASSIGN
                pageNumberExpression = parseExpression()
            } else {
                val currentToken = peek()
                throw ParseException(
                    "Positional argument are not allowed in '@paginate'",
                    sourceLocation = sourceLocation
                )
            }
        }

        consume(TokenType.RPAREN, "Expected ')' after arguments in '@paginate'")
        consume(TokenType.LBRACE, "Expected '{' to start '@paginate' body")
        val block = parseBlock()

        return PaginateNode(
            collectionExpression = collectionExpression,
            variableName = variableName,
            pageSizeExpression = pageSizeExpression,
            pageNumberExpression = pageNumberExpression,
            body = block,
            sourceLocation = sourceLocation
        )
    }

    private fun parseTranslationDirective(
        name: String,
        sourceLocation: SourceLocation
    ): TranslationNode {
        consume(TokenType.LPAREN, "Expected '(' after '@$name'")
        val translationKeyExpression = parseExpression()
        consume(TokenType.RPAREN, "Expected ')' after translation key")
        
        return TranslationNode(
            translationKeyExpression = translationKeyExpression,
            sourceLocation = sourceLocation
        )
    }

    private fun parseSchemaDirective(
        sourceLocation: SourceLocation
    ) {
        consume(TokenType.LBRACE, "Expected '{' after '@schema'")
        var bracesDepth = 1
        val builder = StringBuilder()
        builder.append('{')

        while(true){
            val currentToken = advance()
            when(currentToken.type) {
                TokenType.LBRACE -> bracesDepth++
                TokenType.RBRACE -> bracesDepth--
                TokenType.EOF -> throw ParseException(
                    "Expected '}' before EOF to close '@schema'",
                    sourceLocation = currentToken.sourceLocation
                )
                else -> {}
            }

            builder.append(currentToken.value)

            if(bracesDepth == 0){
                break
            }
        }

        sectionSchema = Json.decodeFromString<SectionSchema>(builder.toString()) //TODO proper errors
    }

    private fun parseCustomDirective(
        name: String,
        sourceLocation: SourceLocation,
    ): DirectiveNode {
        val args = mutableListOf<ExpressionNode>()
        val namedArgs = mutableMapOf<String, ExpressionNode>()
        if(match(TokenType.LPAREN)) {
            if(!check(TokenType.RPAREN)) {
                do {
                    if(check(TokenType.IDENTIFIER) && peek(1).type == TokenType.EQUALS) {
                        val nameToken = advance()
                        advance() // consume EQUALS
                        val valueExpression = parseExpression()
                        namedArgs[nameToken.value] = valueExpression
                    } else {
                        if (namedArgs.isNotEmpty()) {
                            val currentToken = peek()
                            throw ParseException(
                                "Positional arguments cannot follow named arguments in directive call.",
                                sourceLocation = currentToken.sourceLocation
                            )
                        }
                        args.add(parseExpression())
                    }
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RPAREN, "Expected ')' after directive arguments")
        }

        val body = if(match(TokenType.LBRACE)) {
            parseBlock()
        } else {
            null
        }

        return DirectiveNode(
            name = name,
            arguments = args,
            namedArguments = namedArgs,
            body = body,
            sourceLocation = sourceLocation
        )
    }

    private fun parseBlock(): List<SiluetNode> {
        val body = mutableListOf<SiluetNode>()
        while(!check(TokenType.RBRACE) && !isAtEnd()) {
            parseNode()?.let {
                body.add(it)
            }
        }
        consume(TokenType.RBRACE, "Expected ')' to close block")
        return body
    }

    // --- EXPRESSION LEVEL PARSING ---

    private fun parseExpression(): ExpressionNode = parseLogicalOr()

    private fun parseLogicalOr(): ExpressionNode {
        var expr = parseLogicalAnd()
        while(match(TokenType.OR)) {
            val previous = previous()
            val right = parseLogicalAnd()
            expr = BinaryOpNode(
                left = expr,
                operator = BinaryOperator.OR,
                right = right,
                previous.sourceLocation
            )
        }
        return expr
    }

    private fun parseLogicalAnd(): ExpressionNode {
        var expr = parseEquality()
        while(match(TokenType.AND)) {
            val previous = previous()
            val right = parseEquality()
            expr = BinaryOpNode(
                left = expr,
                operator = BinaryOperator.AND,
                right = right,
                previous.sourceLocation
            )
        }
        return expr
    }

    private fun parseEquality(): ExpressionNode {
        var expr = parseComparison()
        while(match(TokenType.EQUALS, TokenType.NOT_EQUALS)) {
            val previous = previous()
            val op = if (previous.type == TokenType.EQUALS) {
                BinaryOperator.EQUALS
            } else {
                BinaryOperator.NOT_EQUALS
            }
            val right = parseComparison()
            expr = BinaryOpNode(
                left = expr,
                operator = op,
                right = right,
                previous.sourceLocation
            )
        }
        return expr
    }

    private fun parseComparison(): ExpressionNode {
        var expr = parseUnary()
        while (match(TokenType.LESS_THAN, TokenType.LESS_OR_EQUAL, TokenType.GREATER_THAN, TokenType.GREATER_OR_EQUAL)){
            val previous = previous()
            val op = when (previous.type) {
                TokenType.LESS_THAN -> BinaryOperator.LESS_THEN
                TokenType.LESS_OR_EQUAL -> BinaryOperator.LESS_OR_EQUAL
                TokenType.GREATER_THAN -> BinaryOperator.GREATER_THAN
                else -> BinaryOperator.GREATER_OR_EQUAL
            }
            val right = parseUnary()
            expr = BinaryOpNode(
                left = expr,
                operator = op,
                right = right,
                previous.sourceLocation
            )
        }
        return expr
    }

    private fun parseUnary(): ExpressionNode {
        if(match(TokenType.NOT)){
            val previous = previous()
            val right = parseUnary()
            return UnaryOpNode(
                operator = UnaryOperator.NOT,
                operand = right,
                previous.sourceLocation
            )
        }
        return parsePostfix()
    }

    private fun parsePostfix(): ExpressionNode {
        var expr = parsePrimary()

        while(match(TokenType.DOT)) {
            val propToken = consume(TokenType.IDENTIFIER, "Expected property name after '.'")
            expr = PropertyAccessNode(
                receiver = expr,
                propertyName = propToken.value,
                sourceLocation = propToken.sourceLocation
            )
        }

        return expr
    }

    private fun parsePrimary(): ExpressionNode {
        val token = peek()

        return when {
            match(TokenType.NULL_LITERAL) -> LiteralNode(
                value = null,
                sourceLocation = token.sourceLocation
            )
            match(TokenType.BOOLEAN_LITERAL) -> LiteralNode(
                value = token.value.toBoolean(),
                sourceLocation = token.sourceLocation
            )
            match(TokenType.NUMBER_LITERAL) -> {
                val text = token.value
                val num = text.toDoubleOrNull() ?: text.toLong()
                LiteralNode(
                    value = num,
                    sourceLocation = token.sourceLocation
                )
            }
            match(TokenType.STRING_LITERAL) -> LiteralNode(
                token.value,
                sourceLocation = token.sourceLocation
            )
            match(TokenType.IDENTIFIER) -> VariableNode(
                name = token.value,
                sourceLocation = token.sourceLocation
            )
            match(TokenType.LPAREN) -> {
                val expr = parseExpression()
                consume(TokenType.RPAREN, "Expected ')' after grouped expression")
                expr
            }
            else -> throw ParseException(
                "Unexpected token '${peek().value}'",
                sourceLocation = token.sourceLocation
            )
        }
    }

    // --- CURSOR HELPERS ---

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if(isAtEnd()) {
            return false
        }
        return peek().type == type
    }

    private fun checkDirective(name: String): Boolean {
        val saveIndex = current

        while (!isAtEnd() && peek().type == TokenType.TEXT && peek().value.isBlank()) {
            advance()
        }

        if(peek().type == TokenType.AT && current + 1 < tokens.size) {
            val next = peek(1)
            return next.type == TokenType.IDENTIFIER && next.value == name
        }

        current = saveIndex
        return false
    }

    private fun advance(): Token {
        if(!isAtEnd()){
            current++
        }
        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    private fun peek(offset: Int = 0): Token {
        return tokens[current + offset]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun consume(type: TokenType, errorMessage: String): Token {
        if(check(type)){
            return advance()
        }
        val currentToken = peek()
        throw ParseException(
            "$errorMessage. Found '${currentToken.value}'.",
            sourceLocation = currentToken.sourceLocation
        )
    }
}
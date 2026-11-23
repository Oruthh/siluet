package dev.siluet.parser

import dev.siluet.directive.BuiltInDirective
import dev.siluet.SourceLocation
import dev.siluet.exception.internal.ParseException
import dev.siluet.lexer.Token
import dev.siluet.lexer.TokenType
import dev.siluet.schema.SectionSchema
import kotlinx.serialization.json.Json

internal class SiluetParser(
    private val tokens: List<Token>,
) {
    private var sectionSchema: SectionSchema? = null

    private var current = 0

    fun parse(): ParseResult {
        return ParseResult(
            ast = parseAsNodeList(),
            sectionSchema = sectionSchema
        )
    }

    fun parseAsNodeList(): List<SiluetNode> {
        current = 0
        sectionSchema = null

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
            val filterNameIdentifier = consume(TokenType.IDENTIFIER, "Expected a filter name after '|'.")
            val args = mutableListOf<ExpressionNode>()

            if(match(TokenType.LPAREN)) {
                if(!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression())
                    } while(match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Expected ')' to close the arguments of filter '${filterNameIdentifier.value}'.")
            }
            filters.add(FilterCallNode(
                name = filterNameIdentifier.value,
                arguments = args,
                sourceLocation = filterNameIdentifier.sourceLocation
            ))
        }

        consume(TokenType.CLOSE_EXPR, "Expected '}}' to close the print expression.")
        return PrintNode(
            expression = expr,
            filters = filters,
            sourceLocation = expr.sourceLocation
        )
    }

    private fun parseDirective(): SiluetNode? {
        val nameToken = consume(TokenType.IDENTIFIER, "Expected a directive name after '@'.")
        val sourceLocation = nameToken.sourceLocation

        return when (BuiltInDirective.of(nameToken.value)) {
            BuiltInDirective.IF -> parseIfDirective(sourceLocation)
            BuiltInDirective.FOR -> parseForDirective(sourceLocation)
            BuiltInDirective.COMPONENT -> parseComponentDirective(sourceLocation)
            BuiltInDirective.SECTIONS -> parseSectionsComponent(sourceLocation)
            BuiltInDirective.PAGINATE -> parsePaginateDirective(sourceLocation)
            BuiltInDirective.SCHEMA -> {
                parseSchemaDirective(sourceLocation)
                null
            }
            BuiltInDirective.ELSE -> throw ParseException(
                "'@else' must directly follow the block of an '@if', e.g. '@if(condition) { ... } @else { ... }'.",
                sourceLocation = sourceLocation
            )
            else -> parseCustomDirective(nameToken.value, sourceLocation)
        }
    }

    private fun parseIfDirective(
        sourceLocation: SourceLocation,
    ): IfNode {
        consume(TokenType.LPAREN, "Directive '@if' requires parentheses, e.g. '@if(...)'.", sourceLocation)
        val condition = parseExpression()
        consume(TokenType.RPAREN, "Expected ')' to close the '@if' condition.")

        consume(TokenType.LBRACE, "Directive '@if' requires a block, e.g. '@if(condition) { ... }'.", sourceLocation)
        val thenBody = parseBlock("if", sourceLocation)

        var elseBody: List<SiluetNode>? = null
        if(checkDirective("else")) {
            advance()
            val elseName = advance()
            consume(TokenType.LBRACE, "Directive '@else' requires a block, e.g. '@else { ... }'.", elseName.sourceLocation)
            elseBody = parseBlock("else", elseName.sourceLocation)
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
        consume(TokenType.LPAREN, "Directive '@for' requires parentheses, e.g. '@for(...)'.", sourceLocation)
        val loopVar = consume(TokenType.IDENTIFIER, "Expected a loop variable name in '@for', e.g. '@for(item in items)'.").value
        consume(TokenType.KEYWORD_IN, "Expected 'in' after the loop variable in '@for', e.g. '@for(item in items)'.")
        val collection = parseExpression()
        consume(TokenType.RPAREN, "Expected ')' to close the '@for' loop definition.")

        consume(TokenType.LBRACE, "Directive '@for' requires a block, e.g. '@for(item in items) { ... }'.", sourceLocation)
        val body = parseBlock("for", sourceLocation)

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
        consume(TokenType.LPAREN, "Directive '@component' requires parentheses, e.g. '@component(...)'.", sourceLocation)
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
                    "'@component' takes the component name followed by named arguments only, e.g. '@component(\"card\", title = product.name)'.",
                    sourceLocation = peek().sourceLocation,
                )
            }
        }

        consume(TokenType.RPAREN, "Expected ')' to close the '@component' arguments.")

        return ComponentNode(
            componentName = nameExpression,
            properties = props,
            sourceLocation = sourceLocation
        )
    }

    private fun parseSectionsComponent(
        sourceLocation: SourceLocation
    ): SectionsNode {
        consume(TokenType.LPAREN, "Directive '@sections' requires parentheses, e.g. '@sections(...)'.", sourceLocation)

        val nameExpression = try {
            parseExpression()
        } catch (_: ParseException) {
            null
        }

        consume(TokenType.RPAREN, "Expected ')' to close the '@sections' arguments.")
        
        return SectionsNode(
            sectionsGroupName = nameExpression,
            sourceLocation = sourceLocation
        )
    }

    private fun parsePaginateDirective(
        sourceLocation: SourceLocation
    ): PaginateNode {
        consume(TokenType.LPAREN, "Directive '@paginate' requires parentheses, e.g. '@paginate(...)'.", sourceLocation)
        val collectionExpression = parseExpression()
        consume(TokenType.KEYWORD_AS, "Expected 'as' after the collection in '@paginate', e.g. '@paginate(products as page by 10)'.")
        val variableName = consume(TokenType.IDENTIFIER, "Expected a variable name after 'as' in '@paginate'.").value
        consume(TokenType.KEYWORD_BY, "Expected 'by' after the variable name in '@paginate', e.g. '@paginate(products as page by 10)'.")
        val pageSizeExpression = parseExpression()

        var pageNumberExpression: ExpressionNode? = null

        while(match(TokenType.COMMA)){
            if (check(TokenType.IDENTIFIER) && peek(1).type == TokenType.ASSIGN) {
                val keyToken = advance()
                if(keyToken.value != "page"){
                    throw ParseException(
                        "Unknown argument '${keyToken.value}' in '@paginate'. The only named argument is 'page'.",
                        sourceLocation = keyToken.sourceLocation
                    )
                }
                advance() // Consume ASSIGN
                pageNumberExpression = parseExpression()
            } else {
                val currentToken = peek()
                throw ParseException(
                    "'@paginate' accepts only the named argument 'page' after 'by', e.g. '@paginate(products as page by 10, page = currentPage)'.",
                    sourceLocation = currentToken.sourceLocation
                )
            }
        }

        consume(TokenType.RPAREN, "Expected ')' to close the '@paginate' arguments.")
        consume(TokenType.LBRACE, "Directive '@paginate' requires a block, e.g. '@paginate(products as page by 10) { ... }'.", sourceLocation)
        val block = parseBlock("paginate", sourceLocation)

        return PaginateNode(
            collectionExpression = collectionExpression,
            variableName = variableName,
            pageSizeExpression = pageSizeExpression,
            pageNumberExpression = pageNumberExpression,
            body = block,
            sourceLocation = sourceLocation
        )
    }

    private fun parseSchemaDirective(
        sourceLocation: SourceLocation
    ) {
        consume(TokenType.LBRACE, "Directive '@schema' requires a block, e.g. '@schema { ... }'.", sourceLocation)
        var bracesDepth = 1
        val builder = StringBuilder()
        builder.append('{')

        while(!isAtEnd()){
            val currentToken = advance()
            when(currentToken.type) {
                TokenType.LBRACE -> bracesDepth++
                TokenType.RBRACE -> bracesDepth--
                TokenType.EOF -> break
                else -> {}
            }

            builder.append(currentToken.value)

            if(bracesDepth == 0){
                break
            }
        }

        if(bracesDepth > 0) {
            throw ParseException(
                "Unclosed block of '@schema': missing '}'.",
                sourceLocation = sourceLocation
            )
        }

        sectionSchema = Json.decodeFromString<SectionSchema>(builder.toString()) //TODO proper errors
    }

    private fun parseCustomDirective(
        name: String,
        sourceLocation: SourceLocation,
    ): DirectiveNode {
        val args = mutableListOf<ExpressionNode>()
        val namedArgs = mutableMapOf<String, ExpressionNode>()

        consume(TokenType.LPAREN, "Directive '@$name' requires parentheses, e.g. '@$name(...)'.", sourceLocation)

        if(!check(TokenType.RPAREN)) {
            do {
                if(check(TokenType.IDENTIFIER) && peek(1).type == TokenType.ASSIGN) {
                    val nameToken = advance()
                    advance() // consume ASSIGN
                    val valueExpression = parseExpression()
                    namedArgs[nameToken.value] = valueExpression
                } else {
                    if (namedArgs.isNotEmpty()) {
                        val currentToken = peek()
                        throw ParseException(
                            "Positional arguments must come before named arguments in '@$name'.",
                            sourceLocation = currentToken.sourceLocation
                        )
                    }
                    args.add(parseExpression())
                }
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "Expected ')' to close the arguments of '@$name'.")

        val body = if(match(TokenType.LBRACE)) {
            parseBlock(name, sourceLocation)
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

    private fun parseBlock(
        directiveName: String,
        directiveLocation: SourceLocation
    ): List<SiluetNode> {
        val body = mutableListOf<SiluetNode>()
        while(!check(TokenType.RBRACE) && !isAtEnd()) {
            parseNode()?.let {
                body.add(it)
            }
        }
        consume(TokenType.RBRACE, "Unclosed block of '@$directiveName': missing '}'.", directiveLocation)
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
            val propToken = consume(TokenType.IDENTIFIER, "Expected a property name after '.'.")
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

                val num: Number = if(text.contains(".")) {
                    text.toDoubleOrNull()
                } else {
                    text.toIntOrNull() ?: text.toLongOrNull()
                } ?: throw ParseException(
                    "Invalid number literal '$text'.",
                    sourceLocation = token.sourceLocation,
                )

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
                consume(TokenType.RPAREN, "Expected ')' to close the grouped expression.")
                expr
            }
            else -> throw ParseException(
                "Unexpected ${describe(peek())} in expression.",
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

    private fun describe(token: Token): String = when (token.type) {
        TokenType.EOF -> "end of file"
        TokenType.TEXT -> "text '${token.value.trim().take(20)}'"
        else -> "'${token.value}'"
    }

    private fun consume(type: TokenType, errorMessage: String, sourceLocation: SourceLocation? = null): Token {
        if(check(type)){
            return advance()
        }
        val currentToken = peek()
        throw ParseException(
            "$errorMessage Found ${describe(currentToken)}.",
            sourceLocation = sourceLocation ?: currentToken.sourceLocation
        )
    }
}
package dev.siluet.parser

import dev.siluet.SourceLocation

/**
 * @suppress
 */
sealed interface SiluetNode{
    val sourceLocation: SourceLocation
}

internal data class TextNode(
    val text: String,
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal data class PrintNode(
    val expression: ExpressionNode,
    val filters: List<FilterCallNode> = emptyList(),
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal data class IfNode(
    val condition: ExpressionNode,
    val thenBody: List<SiluetNode>,
    val elseBody: List<SiluetNode>? = null,
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal data class ForNode(
    val variableName: String,
    val collectionExpression: ExpressionNode,
    val body: List<SiluetNode>,
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal data class ComponentNode(
    val componentName: ExpressionNode,
    val properties: Map<String, ExpressionNode>,
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal data class SectionsNode(
    val sectionsGroupName: ExpressionNode?,
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal data class PaginateNode(
    val collectionExpression: ExpressionNode,
    val variableName: String,
    val pageSizeExpression: ExpressionNode,
    val pageNumberExpression: ExpressionNode?,
    val body: List<SiluetNode>,
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal data class TranslationNode(
    val translationKeyExpression: ExpressionNode,
    override val sourceLocation: SourceLocation
) : SiluetNode

internal data class DirectiveNode(
    val name: String,
    val arguments: List<ExpressionNode>,
    val namedArguments: Map<String, ExpressionNode>,
    val body: List<SiluetNode>? = null,
    override val sourceLocation: SourceLocation,
) : SiluetNode

internal sealed interface ExpressionNode{
    val sourceLocation: SourceLocation
}

internal data class LiteralNode(
    val value: Any?,
    override val sourceLocation: SourceLocation,
) : ExpressionNode

internal data class VariableNode(
    val name: String,
    override val sourceLocation: SourceLocation,
) : ExpressionNode

internal data class PropertyAccessNode(
    val receiver: ExpressionNode,
    val propertyName: String,
    override val sourceLocation: SourceLocation,
) : ExpressionNode

internal data class BinaryOpNode(
    val left: ExpressionNode,
    val operator: BinaryOperator,
    val right: ExpressionNode,
    override val sourceLocation: SourceLocation,
) : ExpressionNode

internal data class UnaryOpNode(
    val operator: UnaryOperator,
    val operand: ExpressionNode,
    override val sourceLocation: SourceLocation,
) : ExpressionNode

internal data class FilterCallNode(
    val name: String,
    val arguments: List<ExpressionNode> = emptyList(),
    val sourceLocation: SourceLocation,
)

internal enum class BinaryOperator {
    EQUALS,
    NOT_EQUALS,
    LESS_THEN,
    LESS_OR_EQUAL,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    AND,
    OR
}

internal enum class UnaryOperator {
    NOT
}
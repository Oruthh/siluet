package dev.siluet.render

import dev.siluet.parser.BinaryOpNode
import dev.siluet.parser.BinaryOperator
import dev.siluet.parser.ExpressionNode
import dev.siluet.parser.FilterCallNode
import dev.siluet.parser.LiteralNode
import dev.siluet.parser.PropertyAccessNode
import dev.siluet.parser.UnaryOpNode
import dev.siluet.parser.UnaryOperator
import dev.siluet.parser.VariableNode

class ExpressionEvaluator(
    private val context: EvaluationContext
) {
    fun evaluate(node: ExpressionNode): Any? {
        return when (node) {
            is LiteralNode -> node.value
            is VariableNode -> context.resolveVariable(node.name)
            is PropertyAccessNode -> evaluatePropertyAccess(node)
            is BinaryOpNode -> evaluateBinary(node)
            is UnaryOpNode -> evaluateUnary(node)
        }
    }

    private fun evaluatePropertyAccess(node: PropertyAccessNode): Any? {
        val receiver = evaluate(node.receiver) ?: return null
        return context.resolveProperty(receiver, node.propertyName)
    }

    private fun evaluateBinary(node: BinaryOpNode): Any? {
        if(node.operator == BinaryOperator.AND) {
            val left = evaluate(node.left)
            return if (!isTruthy(left)) {
                false
            } else {
                val right = evaluate(node.right)
                isTruthy(right)
            }
        }

        if(node.operator == BinaryOperator.OR) {
            val left = evaluate(node.left)
            return if (isTruthy(left)) {
                true
            } else {
                val right = evaluate(node.right)
                isTruthy(right)
            }
        }

        val left = evaluate(node.left)
        val right = evaluate(node.right)

        try {
            return when(node.operator) {
                BinaryOperator.EQUALS -> compareEquality(left, right)
                BinaryOperator.NOT_EQUALS -> !compareEquality(left, right)
                BinaryOperator.LESS_THEN -> compareOrder(left, right) < 0
                BinaryOperator.LESS_OR_EQUAL -> compareOrder(left, right) <= 0
                BinaryOperator.GREATER_THAN -> compareOrder(left, right) > 0
                BinaryOperator.GREATER_OR_EQUAL -> compareOrder(left, right) >= 0
            }
        } catch (e: IllegalArgumentException) {
            throw RenderException(
                e.message!!,
                sourceLocation = node.sourceLocation,
            )
        }
    }

    fun evaluateUnary(node: UnaryOpNode): Boolean {
        val value = evaluate(node.operand)
        return when(node.operator) {
            UnaryOperator.NOT -> !isTruthy(value)
        }
    }

    fun applyFilters(initialValue: Any?, filters: List<FilterCallNode>, renderContext: RenderContext): Any? {
        var currentValue = initialValue
        for (filterNode in filters) {
            val filter = context.filters[filterNode.name]
                ?: throw RenderException(
                    "Filter '${filterNode.name}' is not registered",
                    sourceLocation = filterNode.sourceLocation
                )

            val evaluatedArgs = filterNode.arguments.map {
                evaluate(it)
            }
            try {
                currentValue = filter.apply(currentValue, evaluatedArgs, renderContext)
            } catch (e: Exception) { //TODO maybe we should create FilterExecutionException instead of catching all Exceptions?
                throw RenderException(
                    e.message!!,
                    sourceLocation = filterNode.sourceLocation,
                )
            }

        }

        return currentValue
    }

    fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            is CharSequence -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            else -> true
        }
    }

    private fun compareEquality(left: Any?, right: Any?): Boolean {
        if (left == right) {
            return true
        }
        if (left == null || right == null) {
            return false
        }

        if(left is Number && right is Number) {
            return left.toDouble() == right.toDouble()
        }

        return left == right
    }

    @Suppress("UNCHECKED_CAST")
    private fun compareOrder(left: Any?, right: Any?): Int {
        if (left == null || right == null) {
            throw IllegalArgumentException("Cannot compare nullable values: left=$left, right=$right")
        }

        if (left is Number && right is Number) {
            return left.toDouble().compareTo(right.toDouble())
        }

        if (left is Comparable<*> && left::class.java.isInstance(right)) {
            return (left as Comparable<Any>).compareTo(right)
        }

        throw IllegalArgumentException("Types cannot be compared: ${left::class.simpleName} and ${right::class.simpleName}")
    }
}
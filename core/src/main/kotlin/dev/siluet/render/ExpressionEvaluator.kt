package dev.siluet.render

import dev.siluet.exception.internal.MissingTranslationException
import dev.siluet.exception.internal.PropertyAccessException
import dev.siluet.exception.internal.RenderException
import dev.siluet.parser.BinaryOpNode
import dev.siluet.parser.BinaryOperator
import dev.siluet.parser.ExpressionNode
import dev.siluet.parser.FilterCallNode
import dev.siluet.parser.LiteralNode
import dev.siluet.parser.PropertyAccessNode
import dev.siluet.parser.UnaryOpNode
import dev.siluet.parser.UnaryOperator
import dev.siluet.parser.VariableNode
import dev.siluet.parser.accessPath
import dev.siluet.reflect.PropertyLookup

internal class ExpressionEvaluator(
    private val context: EvaluationContext,
    private val strictMode: Boolean,
) {
    fun evaluate(node: ExpressionNode): Any? {
        return when (node) {
            is LiteralNode -> node.value
            is VariableNode -> evaluateVariable(node)
            is PropertyAccessNode -> evaluatePropertyAccess(node)
            is BinaryOpNode -> evaluateBinary(node)
            is UnaryOpNode -> evaluateUnary(node)
        }
    }

    private fun evaluateVariable(node: VariableNode): Any? {
        val lookupResult = withPropertyErrorLocation(node) { context.resolveVariable(node.name) }
        return when (lookupResult) {
            is PropertyLookup.Found -> {
                lookupResult.value
            }
            PropertyLookup.Missing -> {
                if(strictMode) {
                    throw RenderException(
                        "Undefined variable '${node.name}'",
                        sourceLocation = node.sourceLocation
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun evaluatePropertyAccess(node: PropertyAccessNode): Any? {
        val receiver = evaluate(node.receiver) ?: if (strictMode) {
            val path = node.receiver.accessPath

            throw RenderException(
                "Cannot read property '${node.propertyName}' of null" + (path?.let { " '$it'" } ?: ""),
                sourceLocation = node.sourceLocation
            )
        } else {
            return null
        }
        val lookupResult = withPropertyErrorLocation(node) { context.resolveProperty(receiver, node.propertyName) }
        return when (lookupResult) {
            is PropertyLookup.Found -> {
                lookupResult.value
            }
            PropertyLookup.Missing -> {
                if(strictMode) {
                    val path = node.receiver.accessPath
                    throw RenderException(
                        "Undefined property '${node.propertyName}'" + (path?.let { " of '$it'" } ?: ""),
                        sourceLocation = node.sourceLocation
                    )
                } else {
                    null
                }
            }
        }
    }

    private inline fun <T> withPropertyErrorLocation(node: ExpressionNode, block: () -> T): T {
        return try {
            block()
        } catch (e: PropertyAccessException) {
            throw RenderException(
                e.message!!,
                sourceLocation = node.sourceLocation
            )
        } catch (e: MissingTranslationException) {
            throw RenderException(
                e.message!!,
                sourceLocation = node.sourceLocation
            )
        }
    }

    private fun evaluateBinary(node: BinaryOpNode): Any {
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
            } catch (e: Exception) {
                throw RenderException(
                    "Filter '${filterNode.name}' failed: ${e.message ?: e::class.simpleName}",
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
            is Array<*> -> value.isNotEmpty()
            is IntArray -> value.isNotEmpty()
            is LongArray -> value.isNotEmpty()
            is ShortArray -> value.isNotEmpty()
            is ByteArray -> value.isNotEmpty()
            is DoubleArray -> value.isNotEmpty()
            is FloatArray -> value.isNotEmpty()
            is BooleanArray -> value.isNotEmpty()
            is CharArray -> value.isNotEmpty()
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
package dev.siluet.render

import dev.siluet.blueprint.Template
import dev.siluet.filter.Filter

class EvaluationContext(
    val root: Any?,
    private val accessor: PropertyAccessor,
    val filters: Map<String, Filter> = emptyMap(),
    val directives: Map<String, Directive> = emptyMap(),
    val template: Template
) {
    private val scopeStack = ArrayDeque<MutableMap<String, Any?>>()

    fun pushScope() {
        scopeStack.addLast(mutableMapOf())
    }

    fun popScope() {
        scopeStack.removeLastOrNull()
    }

    fun setLocal(name: String, value: Any?) {
        val currentScope = scopeStack.lastOrNull()
            ?: throw IllegalStateException("No active local scope available.")
        currentScope[name] = value
    }

    fun resolveVariable(name: String): Any? {
        for(scope in scopeStack.reversed()) {
            if(scope.containsKey(name)){
                return scope[name]
            }
        }

        val rootObject = root ?: return null
        return accessor.getProperty(rootObject, name)
    }

    fun resolveProperty(target: Any, propertyName: String): Any? {
        return accessor.getProperty(target, propertyName)
    }
}
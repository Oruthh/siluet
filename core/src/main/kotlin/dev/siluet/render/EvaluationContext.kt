package dev.siluet.render

import dev.siluet.blueprint.Template
import dev.siluet.blueprint.property.PropertyValue
import dev.siluet.blueprint.property.TranslationPropertyValue
import dev.siluet.directive.Directive
import dev.siluet.filter.Filter
import dev.siluet.locale.LocaleRegistry
import dev.siluet.reflect.PropertyAccessor
import dev.siluet.reflect.PropertyLookup
import java.util.Locale

internal class EvaluationContext(
    val root: Any?,
    private val accessor: PropertyAccessor,
    val filters: Map<String, Filter> = emptyMap(),
    val directives: Map<String, Directive> = emptyMap(),
    val template: Template,
    val localeRegistry: LocaleRegistry, //TODO: I'm not sure if I should leave this here, but I don't have any other ideas right now.
    val locale: Locale //TODO: I'm not sure if I should leave this here, but I don't have any other ideas right now.
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

    fun resolveVariable(name: String): PropertyLookup {
        for(scope in scopeStack.reversed()) {
            if(scope.containsKey(name)){
                return PropertyLookup.Found(scope[name])
            }
        }

        val rootObject = root ?: return PropertyLookup.Missing
        return resolveProperty(rootObject, name)
    }

    fun resolveProperty(target: Any, propertyName: String): PropertyLookup {
        val lookup = accessor.getProperty(target, propertyName)
        if(lookup !is PropertyLookup.Found){
            return lookup
        }

        return when (val value = lookup.value){
            is TranslationPropertyValue -> {
                PropertyLookup.Found(localeRegistry.getTranslation(locale, value.value))
            }
            is PropertyValue<*> -> PropertyLookup.Found(value.value)
            else -> lookup
        }
    }

    fun variableOrNull(name: String): Any? {
        return when(val lookup = resolveVariable(name)){
            is PropertyLookup.Found -> lookup.value
            PropertyLookup.Missing -> null
        }

    }
}
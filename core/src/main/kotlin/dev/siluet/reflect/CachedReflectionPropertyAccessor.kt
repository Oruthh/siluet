package dev.siluet.reflect

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

internal class CachedReflectionPropertyAccessor(
    private val securityPolicy: SecurityPolicy = SecurityPolicy.Default
) : PropertyAccessor {
    private val getterCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()

    override fun getProperty(target: Any, propertyName: String): Any? {
        if(target is Map<*, *>) {
            return target[propertyName]
        }

        val targetClass = target::class.java

        securityPolicy.checkAccess(targetClass, propertyName)

        val getters = getterCache.computeIfAbsent(targetClass) { clazz ->
            inspectGetters(clazz)
        }

        val getter = getters[propertyName] ?: return null

        securityPolicy.checkAccess(targetClass, getter.name)

        return getter.invoke(target)
    }

    private fun inspectGetters(clazz: Class<*>): Map<String, Method> {
        val publicMethods = clazz.methods
            .filter { it.parameterCount == 0 && it.returnType != Void.TYPE }
            .associateBy { it.name }

        val propertyMap = mutableMapOf<String, Method>()

        var currentClass: Class<*>? = clazz
        while(currentClass != null && currentClass != Any::class.java && currentClass != Object::class.java) {
            for(field in currentClass.declaredFields) {
                if(Modifier.isStatic(field.modifiers) || field.name.startsWith("$")) {
                    continue
                }

                val propertyName = if(field.type == Lazy::class.java && field.name.endsWith("\$delegate")) {
                    field.name.substring(0, field.name.length - 9)
                } else {
                    field.name
                }
                val capitalized = propertyName.replaceFirstChar { it.uppercase() }

                val getter = publicMethods[propertyName]
                    ?: publicMethods["get$capitalized"]
                    ?: publicMethods["is$capitalized"]

                if (getter != null) {
                    propertyMap[propertyName] = getter
                }
            }
            currentClass = currentClass.superclass
        }

        return propertyMap
    }
}
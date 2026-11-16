package dev.siluet.reflect

import dev.siluet.exception.internal.PropertyAccessException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

internal class CachedReflectionPropertyAccessor(
    private val securityPolicy: SecurityPolicy = SecurityPolicy.Default
) : PropertyAccessor {
    private val propertyCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()

    override fun getProperty(target: Any, propertyName: String): Any? {
        if(target is Map<*, *>) {
            return target[propertyName]
        }

        val targetClass = target::class.java

        securityPolicy.checkAccess(targetClass, propertyName)

        val getter = propertyCache.computeIfAbsent(targetClass, ::inspectProperties)[propertyName]
            ?: return null

        securityPolicy.checkAccess(targetClass, getter.name)

        return invokeGetter(getter, target, propertyName)
    }

    private fun inspectProperties(clazz: Class<*>): Map<String, Method> {
        val properties = HashMap<String, Getter>()

        val publicMethodsByName = clazz.methods
            .filter { it.isGetterCandidate() }
            .groupBy { it.name }
        for(fieldName in instanceFieldNames(clazz)) {
            val method = publicMethodsByName[fieldName]?.preferNonBridge() ?: continue
            properties.offer(fieldName, Getter(method, GetterKind.FIELD_ACCESSOR))
        }

        for(method in clazz.methods) {
            if(!method.isGetterCandidate()) continue
            val (name, kind) = method.toPropertyName() ?: continue
            properties.offer(name, Getter(method, kind))
        }

        val seen = HashSet<Method>()
        val result = HashMap<String, Method>()
        for((name, getter) in properties.entries.sortedBy { it.value.kind.ordinal }) {
            if(seen.add(getter.method)) {
                result[name] = findInvokableVersion(getter.method, clazz)
            }
        }
        return result
    }

    private fun MutableMap<String, Getter>.offer(name: String, candidate: Getter) {
        val existing = this[name]
        val replace = when {
            existing == null -> true
            candidate.kind.ordinal != existing.kind.ordinal -> candidate.kind.ordinal < existing.kind.ordinal
            else -> existing.method.isBridge && !candidate.method.isBridge
        }
        if(replace) {
            this[name] = candidate
        }
    }

    private fun Method.isGetterCandidate(): Boolean =
        parameterCount == 0 &&
            returnType != Void.TYPE &&
            !Modifier.isStatic(modifiers) &&
            declaringClass != Any::class.java &&
            (!isSynthetic || isBridge)

    private fun Method.toPropertyName(): Pair<String, GetterKind>? {
        val name = name
        return when {
            name.hasAccessorPrefix("get") -> decapitalize(name.substring(3)) to GetterKind.GET

            name.hasAccessorPrefix("is") -> when {
                declaringClass.isKotlinClass() -> name to GetterKind.IS
                returnType == java.lang.Boolean.TYPE || returnType == java.lang.Boolean::class.java ->
                    decapitalize(name.substring(2)) to GetterKind.IS
                else -> null
            }

            else -> null
        }
    }

    private fun findInvokableVersion(method: Method, clazz: Class<*>): Method {
        if(method.declaringClass.isPubliclyAccessible()) {
            return method
        }

        for(type in supertypesOf(clazz)) {
            if(!type.isPubliclyAccessible()) continue
            val candidate = try {
                type.getDeclaredMethod(method.name)
            } catch(_: NoSuchMethodException) {
                continue
            }
            if(Modifier.isPublic(candidate.modifiers) && !Modifier.isStatic(candidate.modifiers)) {
                return candidate
            }
        }

        return method
    }

    private fun invokeGetter(getter: Method, target: Any, propertyName: String): Any? {
        return try {
            getter.invoke(target)
        } catch(e: InvocationTargetException) {
            val cause = e.targetException ?: e
            if(cause is Error) {
                throw cause
            }
            throw PropertyAccessException(target::class.java, propertyName, cause)
        } catch(e: IllegalAccessException) {
            throw PropertyAccessException(target::class.java, propertyName, e)
        }
    }

    private fun instanceFieldNames(clazz: Class<*>): Set<String> {
        val names = LinkedHashSet<String>()
        var current: Class<*>? = clazz
        while(current != null && current != Any::class.java) {
            for(field in current.declaredFields) {
                if(!Modifier.isStatic(field.modifiers) && !field.name.contains('$')) {
                    names += field.name
                }
            }
            current = current.superclass
        }
        return names
    }

    private fun supertypesOf(clazz: Class<*>): List<Class<*>> {
        val result = LinkedHashSet<Class<*>>()
        val queue = ArrayDeque<Class<*>>()
        queue += clazz
        while(queue.isNotEmpty()) {
            val type = queue.removeFirst()
            type.superclass?.let { if(result.add(it)) queue += it }
            for(itf in type.interfaces) {
                if(result.add(itf)) queue += itf
            }
        }
        return result.toList()
    }

    private fun Class<*>.isPubliclyAccessible(): Boolean =
        Modifier.isPublic(modifiers) && module.isExported(packageName, ownModule)

    private fun Class<*>.isKotlinClass(): Boolean =
        isAnnotationPresent(Metadata::class.java)

    private fun decapitalize(name: String): String =
        if(name.length > 1 && name[0].isUpperCase() && name[1].isUpperCase()) {
            name
        } else {
            name.replaceFirstChar { it.lowercaseChar() }
        }

    private fun String.hasAccessorPrefix(prefix: String): Boolean =
        length > prefix.length && startsWith(prefix) && this[prefix.length].isUpperCase()

    private fun List<Method>.preferNonBridge(): Method =
        firstOrNull { !it.isBridge } ?: first()

    private enum class GetterKind { FIELD_ACCESSOR, IS, GET }

    private class Getter(val method: Method, val kind: GetterKind)

    private companion object {
        val ownModule: Module = CachedReflectionPropertyAccessor::class.java.module
    }
}

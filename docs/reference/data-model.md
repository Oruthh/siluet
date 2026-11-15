# Data Model & Security

The model you pass to `siluet.render(...)` is what templates see. This page explains how expressions such as `{{ product.finalPrice }}` are resolved against it, and how to keep templates from reaching data they should not.

---

## Recommended: Pass a Dedicated View Model

Build a small class (DTO / view model) for each page and put in it **only** what the templates need:

```kotlin
data class ProductView(
    val name: String,
    val price: BigDecimal,
    val discount: BigDecimal,
) {
    val isOnSale: Boolean get() = discount > BigDecimal.ZERO
    val finalPrice: BigDecimal get() = price - discount
}

val model = mapOf("product" to ProductView(entity.name, entity.price, entity.discount))
siluet.render("product", model)
```

Templates can read **every public getter** of the objects in the model. Passing a view model instead of your domain objects means:

- templates see only fields you chose to expose, not password hashes, internal flags or API keys added to an entity later,
- framework objects never reach the template. JPA/Hibernate entities, for example, are often runtime proxies whose public getters expose ORM internals,
- getters never trigger hidden work such as lazy loading or database queries during rendering.

This matters most when templates are edited by people other than the application developers (e.g. merchants in a theme editor).

---

## How Properties Are Resolved

An expression like `product.finalPrice` reads the property `finalPrice` of the object `product`. Siluet looks for a **public, non-static method without parameters that returns a value**, named according to the conventions below:

| Declared as | Template name |
| :--- | :--- |
| Kotlin `val price: Int` / `val finalPrice get() = ...` | `price` / `finalPrice` |
| Kotlin `val isOnSale: Boolean` | `isOnSale` (Kotlin keeps the `is` prefix) |
| Java `getPrice()` | `price` |
| Java `boolean isActive()` / `Boolean isActive()` | `active` |
| Java `getURL()` | `URL` (two leading capitals are kept) |
| Java record component `title()` | `title` |
| Method `name()` backed by a field `name` (enums, fluent accessors) | `name` |

This covers:

- stored and **computed** Kotlin properties (`val total get() = ...`),
- `by lazy` properties,
- properties declared in **interfaces**, including default getters,
- Java beans and records,
- enum `name` and `ordinal`.

`Map` models are read by key: `{{ settings.theme }}` returns `settings["theme"]`.

Each property is reachable under **exactly one name**. `product.getFinalPrice`, `product.onSale` (for Kotlin `isOnSale`) or `bean.isActive` (for Java `isActive()`) are not aliases. They resolve to nothing.

::: info
A Kotlin function named like a getter, e.g. `fun getTotal(): Int`, cannot be told apart from a property at runtime and is exposed as `total`.
:::

### What Is Never Called

Only getters are invoked. Templates cannot call:

- methods that do not follow the getter naming, e.g. `cancel()`, `delete()`, `clear()`, `toString()`, `component1()`. `{{ order.cancel }}` renders nothing and `cancel()` is **not** invoked,
- methods with parameters,
- static methods,
- non-public methods. Siluet never uses `setAccessible`.

### Missing Values and Failures

- An unknown property, or a property whose value is `null`, renders as an empty string and is falsy in `@if`.
- If a getter throws, rendering fails with a `SiluetException` pointing at the property in the template:

```text
Failed to read property 'finalPrice' of 'com.example.ProductView': ArithmeticException: / by zero at sections/product.html:12:19
```

- A public getter of a non-public class is read through a public interface or superclass that declares it. If there is none, rendering fails with the same kind of error instead of silently rendering nothing.

---

## Security Policy

Every property access is checked by a `SecurityPolicy` before the getter is invoked. Both the name used in the template and the name of the JVM method are checked.

The default policy (`DefaultSecurityPolicy`) blocks:

| Rule | Blocked |
| :--- | :--- |
| Members | `class`, `classLoader`, `declaringClass`, `protectionDomain` (and their `get...` methods), `wait`, `notify`, `notifyAll` |
| Classes (and subclasses) | `Class`, `ClassLoader`, `Runtime`, `Process`, `ProcessBuilder`, `System`, `Thread`, `ThreadGroup`, `Module`, `ModuleLayer` |
| Packages | `java.lang.reflect`, `java.lang.invoke`, `java.lang.management`, `java.io`, `java.nio`, `java.net`, `java.security`, `kotlin.reflect`, `jdk.internal`, `sun.`, `com.sun.` |

A violation fails the render with `SiluetSecurityException`.

### Blocking Your Own Members and Packages

```kotlin
val siluet = Siluet {
    assetLoader = ResourceSiluetAssetLoader("theme")
    securityPolicy = DefaultSecurityPolicy(
        blockedMembers = setOf("passwordHash", "apiKey"),
        blockedPackages = setOf("com.example.internal"),
    )
}
```

Custom rules are added on top of the defaults. Use the **template name** of a property (`passwordHash`, Kotlin `isAdmin`) or its getter name (`getPasswordHash`). Because every property has a single template name, a blocked property cannot be reached under another spelling.

::: warning
The security policy applies to objects, not to `Map` entries. Keys of a `Map` in the model are always readable, so do not put data that templates must not see into maps passed to the model.
:::

### Custom Policy

For an allow-list approach, implement `SecurityPolicy` yourself:

```kotlin
class AllowListPolicy(private val allowedPackages: Set<String>) : SecurityPolicy {
    private val defaults = SecurityPolicy.Default

    override fun checkAccess(targetClass: Class<*>, memberName: String) {
        defaults.checkAccess(targetClass, memberName)

        val allowed = targetClass.isPrimitive ||
            targetClass.packageName.startsWith("java.lang") ||
            targetClass.packageName.startsWith("java.time") ||
            targetClass.packageName.startsWith("java.math") ||
            allowedPackages.any { targetClass.packageName.startsWith(it) }

        if (!allowed) {
            throw SiluetSecurityException("Class '${targetClass.name}' is not allowed in templates.")
        }
    }
}
```

Security policies are not a replacement for view models. They limit damage, but the most reliable way to control what templates can see is to pass only what they need.

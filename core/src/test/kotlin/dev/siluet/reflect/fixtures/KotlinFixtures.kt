package dev.siluet.reflect.fixtures

// Fixtures live in their own package on purpose: the accessor (dev.siluet.reflect) must not get
// package-private access to them, otherwise visibility problems would be hidden in tests.

/** Model from issue #7. */
data class Product(val price: Int, val discount: Int) {
    val isOnSale: Boolean get() = discount > 0
    val finalPrice: Int get() = price - discount

    /** Kotlin keeps the `is` prefix for any type, not only Boolean. */
    val isFeatured: String get() = "featured"

    val description: String by lazy { "lazy description" }

    val broken: Int get() = throw IllegalStateException("price service unavailable")
    val fatal: Int get() = throw FatalTestError()
}

class FatalTestError : Error("fatal test error")

interface Labeled {
    val code: String
    val label: String get() = "default label"
}

class LabeledItem(override val code: String) : Labeled

class OverridingItem : Labeled {
    override val code: String = "O-1"
    override val label: String = "overridden label"
}

private class HiddenLabeled : Labeled {
    override val code: String = "hidden"
}

/** Instance of a private class, reachable only through the public [Labeled] interface. */
fun hiddenLabeled(): Labeled = HiddenLabeled()

fun anonymousLabeled(): Labeled = object : Labeled {
    override val code: String = "anonymous"
}

private class Unreachable {
    val value: String get() = "must not be readable"
}

/** Private class with no public supertype declaring its getter. */
fun unreachable(): Any = Unreachable()

enum class Status { ACTIVE, ARCHIVED }

class Profile(val nickname: String?)

class Account(val login: String, private val secret: String) {
    val isAdmin: Boolean get() = true
    val passwordHash: String get() = "secret-hash"

    var deleteCalls: Int = 0
        private set

    /** Not a property: must never be invoked from a template. */
    fun delete(): Boolean {
        deleteCalls++
        return true
    }

    companion object {
        @JvmStatic
        val instanceCount: Int get() = 42
    }
}

class ThreadHolder {
    val thread: Thread get() = Thread.currentThread()
    val classLoaderHolder: ClassLoader get() = ThreadHolder::class.java.classLoader
    val module: Module get() = ThreadHolder::class.java.module
}

class WorkerThread : Thread()

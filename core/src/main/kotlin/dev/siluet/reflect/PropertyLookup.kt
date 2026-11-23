package dev.siluet.reflect

/**
 * Result of reading a property with [PropertyAccessor.getProperty].
 *
 * Distinguishes a property that does not exist ([Missing]) from one that exists but holds
 * `null` (`Found(null)`).
 */
sealed class PropertyLookup {
    /**
     * The property exists on the target object.
     *
     * @property value The property value, which may be `null`.
     */
    data class Found(val value: Any?) : PropertyLookup()

    /**
     * The target object has no property with the requested name, for example no matching getter
     * or no such key in a [Map].
     */
    data object Missing : PropertyLookup()
}

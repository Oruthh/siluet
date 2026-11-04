package dev.siluet.filter

import dev.siluet.render.RenderContext

/**
 * Functional interface representing an extensible data transformation filter in Siluet templates.
 *
 * Filters are invoked within template expressions using the pipe operator:
 * `{{ expression | filterName(arg1, arg2) }}`.
 *
 * Because this is a [fun interface], custom implementations can be declared either
 * by implementing the interface directly or using a lambda:
 *
 * ```kotlin
 * val uppercaseFilter = Filter { value, _, _ ->
 *     value?.toString()?.uppercase()
 * }
 * ```
 *
 * Filters have access to the active [RenderContext], allowing them to respect contextual
 * state such as the current locale, timezone, or environment variables.
 */
fun interface Filter {
    /**
     * Applies the transformation logic to the piped [value].
     *
     * @param value The value passed from the left side of the pipe (`|`) operator. Can be `null`.
     * @param args Positional arguments passed to the filter within parentheses (e.g., `filterName(arg1, arg2)`).
     * @param renderContext The active rendering context providing access to environment settings, locale, and scope.
     * @return The transformed output, which will either be printed to the template output stream or passed
     *         to the next filter in the pipeline.
     */
    fun apply(value: Any?, args: List<Any?>, renderContext: RenderContext): Any?
}
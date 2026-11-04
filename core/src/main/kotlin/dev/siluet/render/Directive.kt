package dev.siluet.render

/**
 * Functional interface representing a custom block or statement directive in Siluet templates.
 *
 * Directives provide extensible control flow, structural transformations, or custom markup generation.
 * They are declared in template syntax using the `@` prefix and support positional arguments,
 * named arguments, and optional enclosed bodies:
 *
 * ```html
 * @cache(key = "nav", ttl = 300) {
 *     <nav>...</nav>
 * }
 *
 * @alert("warning", dismissible = true)
 * ```
 *
 * ### Example Implementation:
 * ```kotlin
 * val alertDirective = Directive { args, namedArgs, body, _, out ->
 *     val level = args.firstOrNull() ?: "info"
 *     val content = body?.invoke() ?: ""
 *     out.append("""<div class="alert alert-$level">$content</div>""")
 * }
 * ```
 */
fun interface Directive {
    /**
     * Executes the directive logic and writes the output directly to [out].
     *
     * @param args Positional arguments passed to the directive (e.g., `@name("arg1", 42)`).
     * @param namedArgs Named arguments passed to the directive (e.g., `@name(title = "Home", active = true)`).
     * @param body Optional supplier evaluating and rendering the inner block content enclosed by the directive.
     *             Returns `null` if the directive was invoked as a self-closing/standalone statement without a block body.
     * @param renderContext The active runtime execution context, exposing variables, locale, and evaluation scope.
     * @param out The target buffer or output stream where the directive appends rendered markup or text.
     */
    fun execute(
        args: List<Any?>,
        namedArgs: Map<String, Any?>,
        body: (() -> String)?,
        renderContext: RenderContext,
        out: Appendable)
}
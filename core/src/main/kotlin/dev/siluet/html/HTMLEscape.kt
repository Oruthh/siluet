package dev.siluet.html

/**
 * HTML escaping used by Siluet when printing values, available to custom filters and directives.
 *
 * Directives write to their output as it is, so any value that comes from arguments or from the
 * model must be escaped with [escape] before it is written. Filters can use it to escape the
 * untrusted part of a value before marking the result as [SafeHTML].
 */
object HTMLEscape {
    /**
     * Returns [input] as text that is safe to put into HTML content and attribute values.
     *
     * - `null` gives an empty string.
     * - [SafeHTML] is returned as it is, without escaping, the same way it is printed in a template.
     * - Any other value is converted with `toString()`, and `&`, `<`, `>`, `"` and `'` are replaced
     *   with `&amp;`, `&lt;`, `&gt;`, `&quot;` and `&#39;`.
     *
     * Text that is already escaped is escaped again, so escape a value exactly once.
     *
     * ### Example:
     * ```kotlin
     * directive("badge") { args, _, _, _, out ->
     *     out.append("<span class=\"badge\">")
     *     out.append(HTMLEscape.escape(args.firstOrNull()))
     *     out.append("</span>")
     * }
     * ```
     */
    fun escape(input: Any?): String {
        if(input == null){
            return ""
        }
        if(input is SafeHTML) {
            return input.toString()
        }

        val text = input.toString()
        if (text.isEmpty()){
            return ""
        }

        var hasSpecialChar = false
        for (i in text.indices) {
            val c = text[i]
            if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'') {
                hasSpecialChar = true
                break
            }
        }

        if (!hasSpecialChar) {
            return text
        }

        val buffer = StringBuilder(text.length + 16)
        for (i in text.indices) {
            when (val c = text[i]) {
                '&' -> buffer.append("&amp;")
                '<' -> buffer.append("&lt;")
                '>' -> buffer.append("&gt;")
                '"' -> buffer.append("&quot;")
                '\'' -> buffer.append("&#39;")
                else -> buffer.append(c)
            }
        }
        return buffer.toString()
    }
}
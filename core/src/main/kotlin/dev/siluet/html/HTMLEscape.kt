package dev.siluet.html

object HTMLEscape {
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
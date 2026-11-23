package dev.siluet.render

import dev.siluet.directive.Directive
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Custom directives receive evaluated positional arguments, named arguments and the rendered body (issue #19).
 */
class SiluetCustomDirectiveTest {

    data class Page(val minutesLeft: Int)

    private val alert = Directive { args, namedArgs, body, _, out ->
        val level = args.firstOrNull() ?: "info"
        val dismissible = namedArgs["dismissible"] == true
        out.append("""<div class="alert alert-$level" data-dismissible="$dismissible">""")
        out.append(body?.invoke() ?: "")
        out.append("</div>")
    }

    private fun render(template: String, model: Any? = Unit) =
        renderInline(template, model, directives = mapOf("alert" to alert))

    @Test
    fun `named argument is passed to the directive`() {
        assertEquals(
            """<div class="alert alert-warning" data-dismissible="true"></div>""",
            render("""@alert("warning", dismissible = true)""")
        )
    }

    @Test
    fun `named argument and rendered body are passed to the directive`() {
        assertEquals(
            """<div class="alert alert-info" data-dismissible="false"><p>5 minutes left</p></div>""",
            render("""@alert(dismissible = false) {<p>{{ minutesLeft }} minutes left</p>}""", Page(minutesLeft = 5))
        )
    }
}

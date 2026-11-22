package dev.siluet.parser

import dev.siluet.exception.internal.LexerException
import dev.siluet.exception.internal.ParseException
import dev.siluet.lexer.SiluetLexer
import dev.siluet.schema.BlockSchema
import dev.siluet.schema.PropertyOption
import dev.siluet.schema.PropertySchema
import dev.siluet.schema.PropertyType
import dev.siluet.schema.SectionSchema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The body of `@schema { ... }` is a JSON object read as a whole: nested objects and arrays
 * do not end it, and the content of JSON strings (braces, `@`, `{{ }}`, escaped quotes) is
 * taken literally. The block ends at the brace that matches its opening brace.
 */
class SiluetSchemaDirectiveTest {

    private fun parse(source: String): ParseResult =
        SiluetParser(SiluetLexer(source, "sections/hero.html").tokenize()).parse()

    private fun schemaOf(source: String): SectionSchema = assertNotNull(parse(source).sectionSchema, "No schema in:\n$source")

    /** Either the lexer or the parser may detect the error, so both are accepted. */
    private fun assertSyntaxError(source: String, messagePart: String, line: Int, column: Int) {
        val (message, location) = try {
            parse(source)
            fail("No error for:\n$source")
        } catch (e: LexerException) {
            e.message to e.sourceLocation
        } catch (e: ParseException) {
            e.message to e.sourceLocation
        }

        assertTrue(message.orEmpty().contains(messagePart), "Unexpected message: $message")
        assertEquals(line to column, location.line to location.column, "Location for:\n$source")
    }

    // --- nested JSON -----------------------------------------------------------------------------

    @Test
    fun `flat schema is read`() {
        assertEquals(SectionSchema(name = "Hero"), schemaOf("""@schema { "name": "Hero" }"""))
    }

    @Test
    fun `nested objects in properties do not end the block`() {
        val schema = schemaOf(
            """
            @schema {
              "name": "Hero",
              "properties": {
                "title": { "type": "text", "label": "Title", "default": "Welcome" },
                "subtitle": { "type": "textarea", "label": "Subtitle" }
              }
            }
            """.trimIndent()
        )

        assertEquals(
            SectionSchema(
                name = "Hero",
                properties = mapOf(
                    "title" to PropertySchema(type = PropertyType.TEXT, label = "Title", default = "Welcome"),
                    "subtitle" to PropertySchema(type = PropertyType.TEXTAREA, label = "Subtitle"),
                )
            ),
            schema
        )
    }

    @Test
    fun `arrays of objects do not end the block`() {
        val schema = schemaOf(
            """
            @schema {
              "name": "Hero",
              "properties": {
                "align": {
                  "type": "select",
                  "label": "Alignment",
                  "options": [
                    { "value": "left", "label": "Left" },
                    { "value": "center", "label": "Center" }
                  ]
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf(PropertyOption("left", "Left"), PropertyOption("center", "Center")),
            schema.properties!!.getValue("align").options
        )
    }

    @Test
    fun `deeply nested blocks with properties are read`() {
        val schema = schemaOf(
            """
            @schema {
              "name": "Slider",
              "blocks": [
                {
                  "name": "slide",
                  "properties": {
                    "image": { "type": "text", "label": "Image" },
                    "size": { "type": "select", "label": "Size", "options": [ { "value": "s", "label": "S" } ] }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val slide = schema.blocks!!.single()
        assertEquals("slide", slide.name)
        assertEquals(setOf("image", "size"), slide.properties!!.keys)
        assertEquals(listOf(PropertyOption("s", "S")), slide.properties!!.getValue("size").options)
    }

    // --- content of JSON strings is literal -----------------------------------------------------------

    @Test
    fun `braces in strings do not affect the block`() {
        val schema = schemaOf("""@schema { "name": "Price } net", "description": "Use {code} or { or }" }""")

        assertEquals("Price } net", schema.name)
        assertEquals("Use {code} or { or }", schema.description)
    }

    @Test
    fun `at sign in strings is literal`() {
        val schema = schemaOf("""@schema { "name": "Contact", "description": "Write to hello@shop.com or @siluet" }""")

        assertEquals("Write to hello@shop.com or @siluet", schema.description)
    }

    @Test
    fun `print expression syntax in strings is literal`() {
        val schema = schemaOf("""@schema { "name": "Greeting", "description": "Shows {{ user.name }}" }""")

        assertEquals("Shows {{ user.name }}", schema.description)
    }

    @Test
    fun `escaped quotes in strings do not end the string`() {
        val schema = schemaOf("""@schema { "name": "Say \"hi }\"", "description": "a \\ b" }""")

        assertEquals("Say \"hi }\"", schema.name)
        assertEquals("a \\ b", schema.description)
    }

    @Test
    fun `apostrophes in strings are plain characters`() {
        val result = parse("""@schema { "name": "Don't } stop", "description": "It's {fine}" }<p>after</p>""")

        assertEquals(SectionSchema(name = "Don't } stop", description = "It's {fine}"), result.sectionSchema)
        assertEquals("<p>after</p>", result.ast.filterIsInstance<TextNode>().joinToString("") { it.text })
    }

    @Test
    fun `escaped backslash before a closing quote ends the string`() {
        val result = parse("""@schema { "name": "C:\\", "description": "x" }<p>after</p>""")

        assertEquals(SectionSchema(name = "C:\\", description = "x"), result.sectionSchema)
        assertEquals("<p>after</p>", result.ast.filterIsInstance<TextNode>().joinToString("") { it.text })
    }

    @Test
    fun `escaped backslash followed by an escaped quote`() {
        val schema = schemaOf("""@schema { "name": "a\\\"b }", "description": "x" }""")

        assertEquals("a\\\"b }", schema.name)
    }

    @Test
    fun `unicode escapes of braces do not affect the block`() {
        val schema = schemaOf("""@schema { "name": "\u007B\u007D" }""")

        assertEquals("{}", schema.name)
    }

    // --- the rest of the view ---------------------------------------------------------------------

    @Test
    fun `content after the schema is parsed as usual`() {
        val result = parse(
            """
            @schema {
              "name": "Hero",
              "blocks": [ { "name": "slide" } ]
            }
            <section>{{ section.properties.title }}</section>
            """.trimIndent()
        )

        assertEquals(SectionSchema(name = "Hero", blocks = listOf(BlockSchema(name = "slide"))), result.sectionSchema)
        assertTrue(result.ast.any { it is PrintNode }, "Expected the print expression after the schema in ${result.ast}")
        assertTrue(
            result.ast.filterIsInstance<TextNode>().joinToString("") { it.text }.contains("<section>"),
            "Expected the HTML after the schema in ${result.ast}"
        )
    }

    @Test
    fun `schema between other directives`() {
        val result = parse(
            """
            @if(a) {x}
            @schema { "name": "Hero", "properties": { "t": { "type": "text", "label": "T" } } }
            @for(i in xs) { {{ i }} }
            """.trimIndent()
        )

        assertEquals("Hero", result.sectionSchema?.name)
        assertIs<IfNode>(result.ast.first { it !is TextNode })
        assertIs<ForNode>(result.ast.last { it !is TextNode })
    }

    // --- unclosed schema ------------------------------------------------------------------------

    @Test
    fun `unclosed schema is reported at the directive`() {
        assertSyntaxError(
            "<div>\n  @schema {\n    \"name\": \"Hero\"\n",
            "Unclosed block of '@schema': missing '}'.",
            line = 2, column = 4
        )
    }

    @Test
    fun `unclosed schema with a closed nested object is reported at the directive`() {
        assertSyntaxError(
            """@schema { "name": "Hero", "properties": { "t": { "type": "text", "label": "T" } }""",
            "Unclosed block of '@schema': missing '}'.",
            line = 1, column = 2
        )
    }

    @Test
    fun `brace in a string does not close the schema`() {
        assertSyntaxError(
            """@schema { "name": "}" """,
            "Unclosed block of '@schema': missing '}'.",
            line = 1, column = 2
        )
    }

    // --- only a block may follow the name ------------------------------------------------------------

    @Test
    fun `parentheses after schema are reported as a missing block`() {
        assertSyntaxError(
            """@schema("x") { "name": "Hero" }""",
            "Directive '@schema' requires a block",
            line = 1, column = 2
        )
    }

    @Test
    fun `text after schema is not swallowed into the body`() {
        assertSyntaxError(
            """@schema hello <p>a</p> @if(x) { y }""",
            "Directive '@schema' requires a block, e.g. '@schema { ... }'. Found text 'hello",
            line = 1, column = 2
        )
    }

    @Test
    fun `guard - block on the next line is the schema body`() {
        assertEquals(SectionSchema(name = "Hero"), schemaOf("@schema\n{ \"name\": \"Hero\" }"))
    }

    // --- guards ----------------------------------------------------------------------------------

    @Test
    fun `guard - view without a schema has none`() {
        assertEquals(null, parse("<p>{{ x }}</p>").sectionSchema)
    }

    @Test
    fun `guard - blocks of other directives keep their rules`() {
        val result = parse("""@schema { "name": "Hero" } @if(a) { <p>\}</p> }""")

        assertEquals("Hero", result.sectionSchema?.name)
        assertIs<IfNode>(result.ast.single { it !is TextNode })
    }

}

package dev.siluet

import dev.siluet.exception.SiluetException
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.pagination.PaginationContext
import dev.siluet.pagination.PaginationSource
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SiluetTest {
    @Test
    fun `rendering layout with layout only`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test1")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with main sections`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test2")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Simple Section</h1>\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with multiple main section`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test3")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Simple Section</h1><h1>Simple Section2</h1>\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with multiple main section with ordering`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test4")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Simple Section2</h1><h1>Simple Section</h1>\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with multiple main section and sections grouping`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test5")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Simple Section</h1><h1>Simple Section2</h1>\n" +
                "    <div>\n" +
                "        <h1>Group Section</h1><h1>Group Section2</h1>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with component`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test6")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div>TestComponent</div>\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with component with arguments`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test7")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div>TestComponent, arg: Test Arg</div>\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with for loop`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test8")
        }

        val list = listOf("Test 1", "Test 2", "Test 3")
        val model = object {
            val list = list
        }

        val rendered = siluet.render("main", model)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "        Test 1\n" +
                "    \n" +
                "        Test 2\n" +
                "    \n" +
                "        Test 3\n" +
                "    \n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with if`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test9")
        }

        val model = object {
            val arg1 = true
            val arg2 = false
            val arg3 = 5
            val arg4 = "arg4"
        }

        val rendered = siluet.render("main", model)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "        <div>arg1 is true</div>\n" +
                "    \n" +
                "        <div>arg2 is false</div>\n" +
                "    \n" +
                "\n" +
                "    \n" +
                "        <div>arg2 is not true</div>\n" +
                "    \n" +
                "        <div>arg3 is greater than 3</div>\n" +
                "    \n" +
                "        <div>arg3 is smaller than 5</div>\n" +
                "    \n" +
                "        <div>arg3 is greater or equals 5</div>\n" +
                "    \n" +
                "        <div>arg3 is smaller or equals 5</div>\n" +
                "    \n" +
                "        <div>arg3 is 5</div>\n" +
                "    \n" +
                "        <div>arg4 is \"arg4\"</div>\n" +
                "    \n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout using lazy property`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test10")
        }

        val model = object {
            val list by lazy {
                listOf("Lazy1", "Lazy2", "Lazy3")
            }
        }

        val rendered = siluet.render("main", model)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "        Lazy1\n" +
                "    \n" +
                "        Lazy2\n" +
                "    \n" +
                "        Lazy3\n" +
                "    \n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `rendering layout with list pagination`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test11")
        }

        val model = object {
            val list = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14")
            val pageNumber = 1
        }

        val rendered = siluet.render("main", model)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "        \n" +
                "            1\n" +
                "        \n" +
                "            2\n" +
                "        \n" +
                "            3\n" +
                "        \n" +
                "            4\n" +
                "        \n" +
                "            5\n" +
                "        \n" +
                "            6\n" +
                "        \n" +
                "            7\n" +
                "        \n" +
                "            8\n" +
                "        \n" +
                "            9\n" +
                "        \n" +
                "            10\n" +
                "        \n" +
                "    \n" +
                "</body>\n" +
                "</html>", rendered)

        val model2 = object {
            val list = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14")
            val pageNumber = 2
        }
        val rendered2 = siluet.render("main", model2)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "        \n" +
                "            11\n" +
                "        \n" +
                "            12\n" +
                "        \n" +
                "            13\n" +
                "        \n" +
                "            14\n" +
                "        \n" +
                "    \n" +
                "</body>\n" +
                "</html>", rendered2)
    }

    @Test
    fun `layout rendering with paginationSource pagination`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test11")
        }

        val paginationSource = object : PaginationSource<String> {
            override fun fetchPage(
                page: Int,
                pageSize: Int
            ): PaginationContext<String> {
                val items = if(page == 1) {
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
                } else if(page == 2) {
                    listOf("11", "12", "13", "14")
                } else {
                    listOf()
                }

                return PaginationContext.of(items, page, pageSize)
            }
        }

        val model = object {
            val list = paginationSource
            val pageNumber = 1
        }
        val rendered = siluet.render("main", model)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "        \n" +
                "            1\n" +
                "        \n" +
                "            2\n" +
                "        \n" +
                "            3\n" +
                "        \n" +
                "            4\n" +
                "        \n" +
                "            5\n" +
                "        \n" +
                "            6\n" +
                "        \n" +
                "            7\n" +
                "        \n" +
                "            8\n" +
                "        \n" +
                "            9\n" +
                "        \n" +
                "            10\n" +
                "        \n" +
                "    \n" +
                "</body>\n" +
                "</html>", rendered)

        val model2 = object {
            val list = paginationSource
            val pageNumber = 2
        }
        val rendered2 = siluet.render("main", model2)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    \n" +
                "        \n" +
                "            11\n" +
                "        \n" +
                "            12\n" +
                "        \n" +
                "            13\n" +
                "        \n" +
                "            14\n" +
                "        \n" +
                "    \n" +
                "</body>\n" +
                "</html>", rendered2)
    }

    @Test
    fun `layout rendering with translation`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test12")
        }

        val rendered = siluet.render("main")
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    Test tłumaczenia\n" +
                "</body>\n" +
                "</html>", rendered)

        val rendered2 = siluet.render("main") {
            locale = Locale.ENGLISH
        }
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    Locales Test\n" +
                "</body>\n" +
                "</html>", rendered2)
    }

    @Test
    fun `layout rendering with date`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test13")
        }

        val now = LocalDateTime.of(2026, 11, 3, 18,48, 10)

        val model = object {
            val testDate = now
        }

        val rendered = siluet.render("main", model)
        assertEquals("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>LayoutTest</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    2026-11-03 18:48\n" +
                "    3 listopada 2026 18:48:10\n" +
                "</body>\n" +
                "</html>", rendered)
    }

    @Test
    fun `layout rendering with wrong date`() {
        val siluet = Siluet.invoke {
            assetLoader = ResourceSiluetAssetLoader("test13")
        }

        val now = emptyMap<String, String>()

        val model = object {
            val testDate = now
        }

        val exception = assertFailsWith<SiluetException> {
            siluet.render("main", model)
        }
        assertEquals("Cannot parse '{}' as a valid date/time at layouts/main.html:8:19", exception.message)
    }

    @Test
    fun `layout rendering with block and properties`() {

    }

}
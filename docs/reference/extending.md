# Extending Siluet

Siluet can be extended from your Kotlin code with custom filters, custom directives, reusable plugins and asset loaders that read themes from anywhere. Everything is registered once, in the `Siluet { }` block.

---

## Custom Filters

A filter receives the value on the left of `|`, the arguments in parentheses and the current `RenderContext` (locale and time zone). Register it with a lambda:

```kotlin
val siluet = Siluet {
    assetLoader = ResourceSiluetAssetLoader("theme")

    filter("initials") { value, _, _ ->
        value?.toString()
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.joinToString("") { it.first().uppercase() }
    }
}
```

```html
<span class="avatar">{{ user.fullName | initials }}</span>
<!-- If fullName is "Jane Doe", renders: <span class="avatar">JD</span> -->
```

Arguments are evaluated before the filter is called, so they can be literals or any expression. Filters that depend on the reader's language should use the locale from the render context:

```kotlin
filter("money") { value, args, context ->
    val amount = value as? Number ?: return@filter value
    val currency = args.firstOrNull()?.toString() ?: "EUR"
    NumberFormat.getCurrencyInstance(context.locale)
        .apply { this.currency = Currency.getInstance(currency) }
        .format(amount)
}
```

```html
<p class="price">{{ product.price | money("PLN") }}</p>
```

For larger filters, implement the `Filter` interface and register an instance with `filter("name", MyFilter())`.

A few rules apply to every filter:

- **The result is escaped when printed**, like any other value. To print a filter's result as HTML, chain [`rawHTML`](/reference/filters#rawhtml) after it in the template.
- **Exceptions become template errors.** If a filter throws, the render fails with a `SiluetViewException` whose message is the exception's message and whose location points at the filter in the template. Start the message with the filter name, e.g. `Filter 'money' expects a number`, so the error is easy to trace.
- **Built-in names are reserved.** A custom filter registered under the name of a built-in filter (`date`, `t`, `default`...) is ignored.
- **Filters must be thread-safe.** One `Siluet` instance renders many pages in parallel and shares its filters.

---

## Custom Directives

A directive receives its positional arguments, named arguments, an optional body and the render context, and writes its output to `out`:

```kotlin
val siluet = Siluet {
    assetLoader = ResourceSiluetAssetLoader("theme")

    directive("alert") { args, namedArgs, body, _, out ->
        val level = args.firstOrNull()?.toString() ?: "info"
        val dismissible = namedArgs["dismissible"] == true

        out.append("""<div class="alert alert-${escapeHtml(level)}">""")
        out.append(body?.invoke() ?: "")
        if (dismissible) {
            out.append("""<button class="alert-close" aria-label="Close">×</button>""")
        }
        out.append("</div>")
    }
}
```

```html
@alert("warning", dismissible = true) {
    <p>Your session expires in {{ minutesLeft }} minutes.</p>
}
```

How a directive is called in a template:

| Template | `args` | `namedArgs` | `body` |
| :--- | :--- | :--- | :--- |
| `@alert` | `[]` | `{}` | `null` |
| `@alert("warning")` | `["warning"]` | `{}` | `null` |
| `@alert("warning", dismissible = true)` | `["warning"]` | `{dismissible=true}` | `null` |
| `@alert("warning") { ... }` | `["warning"]` | `{}` | renders the block |

Positional arguments must come before named ones. All arguments are evaluated before the directive runs. Number literals arrive as `Int` (`Long` when too large for `Int`) or `Double` when written with a decimal point, so `@stars(5)` passes `5` as an `Int`.

`body` is a function: call it to render the block, skip it to leave the block out, or call it several times to repeat it. It returns the rendered HTML of the block, with the block's own expressions already escaped.

::: warning Directive output is not escaped
Everything a directive writes to `out` goes to the page as it is. Escape any value that comes from arguments or from the model before writing it, as `escapeHtml(level)` does in the example above. Only the result of `body()` is safe to write directly.
:::

A minimal escaping helper:

```kotlin
fun escapeHtml(value: String): String = buildString(value.length) {
    for (c in value) when (c) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        '"' -> append("&quot;")
        '\'' -> append("&#39;")
        else -> append(c)
    }
}
```

Directives follow the same [syntax rules](/reference/syntax#where-template-syntax-is-active) as the built-in ones: they work in HTML content, inside tags and in comments, but not inside attribute values, `<style>` or `<script>`. The names of built-in directives (`if`, `else`, `for`, `component`, `sections`, `paginate`, `schema`) are reserved. As with filters, directives must be thread-safe.

---

## Plugins

A plugin bundles filters and directives into a reusable module with its own configuration:

```kotlin
class MoneyConfig {
    var defaultCurrency: String = "EUR"
}

object MoneyPlugin : SiluetPlugin<MoneyConfig> {
    override fun install(builder: SiluetConfig, configure: MoneyConfig.() -> Unit) {
        val config = MoneyConfig().apply(configure)

        builder.filter("money") { value, args, context ->
            val amount = value as? Number ?: return@filter value
            val currency = args.firstOrNull()?.toString() ?: config.defaultCurrency
            NumberFormat.getCurrencyInstance(context.locale)
                .apply { this.currency = Currency.getInstance(currency) }
                .format(amount)
        }
    }
}
```

```kotlin
val siluet = Siluet {
    assetLoader = ResourceSiluetAssetLoader("theme")
    install(MoneyPlugin) {
        defaultCurrency = "PLN"
    }
}
```

---

## Custom Asset Loaders

Siluet reads every theme file through a `SiluetAssetLoader`. The built-in `ResourceSiluetAssetLoader` reads from the classpath, but themes can live anywhere: in a database, in S3 or behind an API. This is what makes a theme editor possible, where merchants change sections and properties without redeploying the application.

Implement one method per kind of asset and return `null` when an asset does not exist:

```kotlin
class DatabaseAssetLoader(private val themes: ThemeRepository, private val themeId: Long) : SiluetAssetLoader {

    override fun loadTemplate(name: String) = asset("templates", name)
    override fun loadSectionsGroup(name: String) = asset("groups", name)
    override fun loadLayout(name: String) = asset("layouts", name)
    override fun loadSection(name: String) = asset("sections", name)
    override fun loadComponent(name: String) = asset("components", name)
    override fun loadLocale(locale: Locale) = asset("locales", locale.language)

    private fun asset(kind: String, name: String): SiluetAsset? =
        themes.find(themeId, kind, name)?.let { row ->
            SiluetAsset(sourceName = "db:theme_files#${row.id}", content = row.content)
        }
}
```

| Method | Returns |
| :--- | :--- |
| `loadTemplate` | JSON template, e.g. `home` |
| `loadSectionsGroup` | JSON section group, e.g. `footer` |
| `loadLayout`, `loadSection`, `loadComponent` | HTML views |
| `loadLocale` | JSON translations for a `Locale` |

The `sourceName` appears in error messages and must be unique per asset. See [Source Names](/reference/errors#source-names).

Compiled views and parsed templates are cached by name for the lifetime of the `Siluet` instance (see `cache { }` in [Getting Started](/guide/getting-started#step-4-optional-tune-the-engine)). If your loader returns content that changes at runtime, create a new `Siluet` instance to pick up the changes.

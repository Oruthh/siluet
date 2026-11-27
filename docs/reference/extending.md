# Extending Siluet

Siluet can be extended from your Kotlin code with custom filters, custom directives, reusable plugins and asset loaders that read themes from anywhere. Filters, directives and plugins are registered once, in the `Siluet { }` block, and are available in every theme. Asset loaders are set per theme, in `siluet.theme { }`.

---

## Custom Filters

A filter receives the value on the left of `|`, the arguments in parentheses and the current `RenderContext` (locale and time zone). Register it with a lambda:

```kotlin
val siluet = Siluet {
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

### Returning HTML

A filter that produces markup, such as rendered Markdown or an inline SVG icon, returns it as `SafeHTML` with `asSafeHTML()` from `dev.siluet.html`. `SafeHTML` is printed without escaping, so escape every untrusted part with `HTMLEscape.escape(...)` before combining it into the result:

```kotlin
import dev.siluet.html.HTMLEscape
import dev.siluet.html.asSafeHTML

filter("icon") { value, _, _ ->
    """<svg class="icon"><use href="#icon-${HTMLEscape.escape(value)}"/></svg>""".asSafeHTML()
}
```

```html
<button>{{ "cart" | icon }} Add to cart</button>
```

::: danger Only wrap trusted content
`asSafeHTML()` turns off escaping for the whole value. Never call it on user input or other untrusted text that has not been escaped, or the page becomes open to cross-site scripting (XSS).
:::

`SafeHTML` passes through filters that do not change it, such as `default`, but built-in text filters (`uppercase`, `truncate`...) reject it with `expects a String value, but received SafeHTML`, because changing HTML as plain text would break it. Put such filters before the one that returns HTML.

A few rules apply to every filter:

- **The result is escaped when printed**, like any other value. To output markup, return it as `SafeHTML` (see [Returning HTML](#returning-html) below).
- **Exceptions become template errors.** If a filter throws, the render fails with a `SiluetViewException` whose location points at the filter in the template. Siluet adds the name the filter was called by, so the message reads `Filter '<name>' failed: <your message>`. Write the message as the rest of that sentence, without the filter name, e.g. `expects a number, but received String`. An exception without a message is reported by its type, e.g. `Filter 'money' failed: IllegalStateException`.
- **Names are unique.** Registering a filter under the name of a built-in filter (`date`, `t`, `default`...) throws an `IllegalArgumentException`, and so does registering the same name twice, whether by the application or by a [plugin](#plugins). The error is raised by the `filter(...)` call itself and names whoever registered the name first.
- **Filters must be thread-safe.** One `Siluet` instance renders many pages in parallel and shares its filters.

---

## Custom Directives

A directive receives its positional arguments, named arguments, an optional body and the render context, and writes its output to `out`:

```kotlin
val siluet = Siluet {
    directive("alert") { args, namedArgs, body, _, out ->
        val level = args.firstOrNull()?.toString() ?: "info"
        val dismissible = namedArgs["dismissible"] == true

        out.append("""<div class="alert alert-${HTMLEscape.escape(level)}">""")
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
| `@alert()` | `[]` | `{}` | `null` |
| `@alert() { ... }` | `[]` | `{}` | renders the block |
| `@alert("warning")` | `["warning"]` | `{}` | `null` |
| `@alert("warning", dismissible = true)` | `["warning"]` | `{dismissible=true}` | `null` |
| `@alert("warning") { ... }` | `["warning"]` | `{}` | renders the block |

A directive is always called with parentheses, even without arguments. Positional arguments must come before named ones. All arguments are evaluated before the directive runs. Number literals arrive as `Int` (`Long` when too large for `Int`) or `Double` when written with a decimal point, so `@stars(5)` passes `5` as an `Int`.

`body` is a function: call it to render the block, skip it to leave the block out, or call it several times to repeat it. It returns the rendered HTML of the block, with the block's own expressions already escaped.

::: warning Directive output is not escaped
Everything a directive writes to `out` goes to the page as it is. Escape any value that comes from arguments or from the model with `HTMLEscape.escape(...)` from `dev.siluet.html` before writing it, as the example above does with `level`. Only the result of `body()` is safe to write directly. `HTMLEscape.escape` returns an empty string for `null` and writes `SafeHTML` as it is.
:::

Directives follow the same [syntax rules](/reference/syntax#where-template-syntax-is-active) as the built-in ones: they work in HTML content, inside tags and in comments, but not inside attribute values, `<style>` or `<script>`. The names of built-in directives (`if`, `else`, `for`, `component`, `sections`, `paginate`, `schema`) are reserved, and as with filters, registering a reserved name or the same name twice throws an `IllegalArgumentException`. A filter and a directive may share a name. As with filters, directives must be thread-safe.

If a directive throws, the render fails with a `SiluetViewException` that points at the directive in the template, with the message `Directive '<name>' failed: <your message>` (or the exception type when there is no message). As with filters, write the message without the directive name. Errors raised while rendering the block you get from `body`, such as a failing filter or another directive inside it, are reported as they are, with their own message and location, not as an error of your directive.

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
    install(MoneyPlugin) {
        defaultCurrency = "PLN"
    }
}
```

A plugin can be installed only once, and the names it registers must not clash with built-in names, with the application's own filters and directives or with other plugins. A clash fails when the configuration is built, with a message that names both sides, e.g. `Filter 'money' is already registered by MoneyPlugin.`

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

Give each theme its own loader instance and pass it in `siluet.theme { }`:

```kotlin
val theme = siluet.theme {
    assetLoader = DatabaseAssetLoader(repository, themeId)
}
```

Compiled views, parsed templates and translations are cached per theme. If your loader returns content that changes at runtime, call `theme.invalidate(...)` for the changed asset or `theme.clearCache()` to pick up the changes; see [Picking Up Changes](/guide/themes#picking-up-changes).

# Error Handling

When a page cannot be rendered, `theme.render(...)` throws an exception and returns no partial HTML. This page lists the exceptions, what each one tells you, and how asset loaders decide the file names shown in error messages.

---

## Exception Hierarchy

All rendering errors share the base class `SiluetException`, so a single `catch` handles every failure. Each subclass carries the most precise location Siluet has for that kind of error:

```text
SiluetException
├── SiluetViewException          sourceLocation   HTML views: layouts, sections, components
├── SiluetBlueprintException     sourceName       JSON blueprints: templates, section groups
└── SiluetAssetNotFoundException assetType, name  an asset requested by your application does not exist
```

| Exception | Thrown when | Location |
| :--- | :--- | :--- |
| `SiluetViewException` | Syntax errors in an HTML view, including an unclosed `<style>` / `<script>`, attribute value, tag or comment; filter and expression errors, a missing component or section view, undefined variables, properties and translation keys in [strict mode](/guide/getting-started#strict-mode), an unknown section group name in `@sections("...")`, loop and render depth limits | `sourceLocation`: file, line and column in the HTML view |
| `SiluetBlueprintException` | An invalid template or section group, e.g. a typo or duplicate in `sectionsOrder` / `blocksOrder` | `sourceName`: the JSON file. JSON errors have no line and column. |
| `SiluetAssetNotFoundException` | The template passed to `theme.render(...)` does not exist | `assetType` and `name`, e.g. `template` / `homee` |

```kotlin
try {
    theme.render("product", model)
} catch (e: SiluetViewException) {
    log.error("Template error at ${e.sourceLocation}: ${e.message}")
} catch (e: SiluetBlueprintException) {
    log.error("Invalid page configuration in ${e.sourceName}: ${e.message}")
} catch (e: SiluetException) {
    log.error("Rendering failed: ${e.message}")
}
```

Example messages:

```text
Filter 'date' failed: cannot read 'soon' (String) as a date or time; expected a java.time value, epoch seconds/millis or an ISO-8601 string at /theme/layouts/main.html:8:19
Unclosed <style> element: missing </style> at /theme/sections/hero.html:4:5
Undefined variable 'prodcut' at /theme/sections/hero.html:3:12
'heroo' listed in sectionsOrder is not defined. Defined: 'hero', 'promo'. in /theme/templates/home.json
Could not find template 'homee'
```

Errors raised by a filter, built-in or custom, always start with `Filter '<name>' failed:`, where `<name>` is the name used in the template, followed by the reason:

```text
Filter 'uppercase' failed: expects a String value, but received Int at /theme/sections/hero.html:3:22
Filter 'truncate' failed: requires a 'length' argument, e.g. truncate(20) at /theme/sections/hero.html:5:18
Filter 'date' failed: invalid date pattern 'jj-yy': Unknown pattern letter: j at /theme/layouts/main.html:8:19
```

Errors raised by a custom directive start with `Directive '<name>' failed:` in the same way. An error inside the directive's block is reported where it happened, not as an error of the directive:

```text
Directive 'alert' failed: theme has no alert styles at /theme/sections/hero.html:12:4
```

::: warning Not yet wrapped
A few failures are currently thrown without being wrapped in a `SiluetException`:

- a [security policy](/reference/data-model#security-policy) violation is thrown as `SiluetSecurityException`,
- a layout named in a template that does not exist,
- a template or section group that is not valid JSON (`kotlinx.serialization.SerializationException`).

Catch `SiluetSecurityException` separately for now. All three will be moved into the hierarchy above, so do not rely on the exception types of the last two.
:::

---

## Source Names

The file names in error messages, `sourceLocation.filePath` and `sourceName`, come from your `SiluetAssetLoader`: every loaded asset is returned as a `SiluetAsset` with a `sourceName` chosen by the loader.

`ResourceSiluetAssetLoader` reports the classpath path including its prefix:

```kotlin
val loader = ResourceSiluetAssetLoader("theme")
loader.loadLayout("main")?.sourceName // "/theme/layouts/main.html"
```

A custom loader reports wherever the asset really comes from:

```kotlin
class DatabaseAssetLoader(private val repository: ThemeRepository) : SiluetAssetLoader {
    override fun loadSection(name: String): SiluetAsset? =
        repository.findSection(name)?.let { row ->
            SiluetAsset(sourceName = "db:theme_sections#${row.id}", content = row.html)
        }

    // ... other methods
}
```

::: warning Source names must be unique
Siluet also uses `sourceName` to tell views apart when it detects infinite recursion (see `maxRenderDepth`). Two different assets returned with the same `sourceName` are treated as the same view, so a component that includes another one could be reported as recursion. Use an identifier that is unique per asset: a path, a URL (`s3://shop-themes/acme/sections/hero.html`) or a record id (`db:theme_sections#42`).
:::

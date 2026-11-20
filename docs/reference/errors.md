# Error Handling

When a page cannot be rendered, `siluet.render(...)` throws an exception and returns no partial HTML. This page lists the exceptions, what each one tells you, and how asset loaders decide the file names shown in error messages.

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
| `SiluetViewException` | Syntax errors in an HTML view, including an unclosed `<style>` / `<script>`, attribute value, tag or comment; filter and expression errors, a missing component or section view, an unknown section group name in `@sections("...")`, loop and render depth limits | `sourceLocation`: file, line and column in the HTML view |
| `SiluetBlueprintException` | An invalid template or section group, e.g. a typo or duplicate in `sectionsOrder` / `blocksOrder` | `sourceName`: the JSON file. JSON errors have no line and column. |
| `SiluetAssetNotFoundException` | The template passed to `siluet.render(...)` does not exist | `assetType` and `name`, e.g. `template` / `homee` |

```kotlin
try {
    siluet.render("product", model)
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
Cannot parse '{}' as a valid date/time at /theme/layouts/main.html:8:19
Parse error: Unclosed <style> element: missing </style> at /theme/sections/hero.html:4:5
'heroo' listed in sectionsOrder is not defined. Defined: 'hero', 'promo'. in /theme/templates/home.json
Could not find template 'homee'
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

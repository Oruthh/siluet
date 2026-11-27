# Themes & Caching

A `Siluet` engine renders pages through **themes**. A theme is a set of templates, layouts, sections, components and translations, read through one asset loader. One engine can serve a single theme or thousands of them, for example one theme per shop in a multi-tenant platform.

---

## Engine and Themes

The engine holds everything that is the same for every theme. Each theme brings its own files and its own cache:

| Shared by all themes (`Siluet { }`) | Separate for every theme (`theme { }`) |
| :--- | :--- |
| Filters, directives and plugins | Asset loader |
| Security policy | Compiled layouts, sections and components |
| `maxLoopIterations`, `maxRenderDepth`, `strictMode` | Parsed templates and section groups |
| Cache limits (`cache { }`) | Translations |

```kotlin
val siluet = Siluet {
    strictMode = true
    filter("initials") { value, _, _ -> /* ... */ }
}

val theme = siluet.theme {
    assetLoader = ResourceSiluetAssetLoader("theme")
}

val html = theme.render("home", model) {
    locale = Locale.ENGLISH
}
```

Themes are isolated from each other: a theme never sees another theme's templates or views, and the `t` filter and `t:` properties always use the translations of the theme that renders. Two themes can therefore have a section with the same name, or the same translation key with different texts.

::: tip Create once, reuse
The engine and its themes are thread-safe. Create them once (e.g., at application startup) and share them across requests. Every call to `siluet.theme { }` creates a **new** theme with an empty cache, so calling it for every request would parse every view again.
:::

## One Theme per Tenant

When every shop has its own theme, give each one an asset loader that reads that theme's files, and keep the themes in your application, created on first use:

```kotlin
class ThemeResolver(private val siluet: Siluet, private val repository: ThemeRepository) {
    private val themes = ConcurrentHashMap<Long, SiluetTheme>()

    fun themeFor(themeId: Long): SiluetTheme = themes.computeIfAbsent(themeId) {
        siluet.theme {
            assetLoader = DatabaseAssetLoader(repository, themeId)
        }
    }
}
```

```kotlin
val html = resolver.themeFor(shop.themeId).render("product", model) {
    locale = request.locale
}
```

A theme your application no longer references (e.g., after removing it from the map) is released together with its cache. See [Custom Asset Loaders](/reference/extending#custom-asset-loaders) for writing a loader.

## Cache

Every theme compiles its assets on first use and keeps them in its own cache: compiled layouts, sections and components, parsed templates and section groups, and translations for each locale. Each of them counts as one **entry**.

All limits are enabled by default and configured once, on the engine:

```kotlin
val siluet = Siluet {
    cache {
        maxEntriesPerTheme = 500          // entries one theme may keep (default: 500)
        maxTotalEntries = 50_000          // entries across all themes (default: 50_000)
        evictIdleThemesAfter = 30.minutes // release the cache of a theme that has not rendered (default: 30 minutes)
    }
}
```

- **Per-theme limit.** When a theme reaches `maxEntriesPerTheme`, the entry that was used least recently is evicted.
- **Total limit.** When all themes together exceed `maxTotalEntries`, entries are evicted from the themes that rendered least recently, down to 90% of the limit. Themes that are in use keep their entries. The limit is soft: under concurrent rendering the total may exceed it briefly.
- **Idle themes.** A theme that has not rendered for `evictIdleThemesAfter` releases its whole cache. It stays valid and loads its assets again on the next render. Idle themes are checked while other themes render, there is no background thread. Use `Duration.INFINITE` to turn this off.

An evicted entry is simply loaded and compiled again the next time it is needed, so eviction never makes a render fail.

A theme can lower its own limit, e.g. for small or rarely visited shops:

```kotlin
val theme = siluet.theme {
    assetLoader = DatabaseAssetLoader(repository, themeId)
    maxCacheEntries = 100
}
```

Invalid limits (zero, negative, or a per-theme limit above `maxTotalEntries`) fail with an `IllegalArgumentException` when the engine or the theme is created.

## Picking Up Changes

Cached assets are not reloaded on their own. When a theme's files change at runtime, for example after a merchant saves a section in a theme editor, tell the theme what changed:

```kotlin
// One asset
theme.invalidate(SiluetAssetType.SECTION, "featuredProducts")

// Everything the theme has cached
theme.clearCache()
```

| `SiluetAssetType` | Asset | Name |
| :--- | :--- | :--- |
| `TEMPLATE` | `templates/<name>.json` | `home` |
| `LAYOUT` | `layouts/<name>.html` | `shop` |
| `SECTION` | `sections/<name>.html` | `featuredProducts` |
| `COMPONENT` | `components/<name>.html` | `productCard` |
| `SECTIONS_GROUP` | `sections/groups/<name>.json` | `footer` |
| `LOCALE` | `locales/<language>.json` | `en` |

Only the given theme is affected; other themes keep their cached copies. A section and a component with the same name are separate entries, so invalidating one does not touch the other. For `LOCALE`, the name is a language tag and the translations of every locale with that language (`en`, `en-US`, `en-GB`, ...) are removed.

## Cache Statistics

`theme.cacheStats()` returns a snapshot of the theme's cache, useful for metrics and for tuning the limits:

```kotlin
val stats = theme.cacheStats()
log.info("Theme cache: ${stats.size} entries, ${stats.hits} hits, ${stats.misses} misses, ${stats.evictions} evictions")
```

| Field | Meaning |
| :--- | :--- |
| `hits` | Lookups served from the cache |
| `misses` | Lookups that had to load and compile the asset |
| `evictions` | Entries removed because the theme limit or the total limit was reached |
| `size` | Entries currently cached |

The counters accumulate for the lifetime of the theme. `clearCache()`, `invalidate(...)` and idle eviction reset the size but are not counted as evictions. A high number of evictions next to a high number of misses means the limits are too small for the theme.

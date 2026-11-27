# Module core

Core execution engine and foundational abstractions for the **Siluet** templating system.

The `:core` module provides compilation, AST parsing, runtime evaluation, security sandboxing, and caching primitives for dynamic HTML rendering.

**Key Features**

- **Orchestrated Templates:** Render composite pages configured via JSON definitions (`templates/<name>.json`), binding layouts, dynamic sections, and section groups.

- **Security & Sandboxing:** Multi-tier reflection guardrails via `SecurityPolicy` preventing access to forbidden classes (`System`, `Runtime`, `ProcessBuilder`), internal packages, and dangerous members.

- **Extensible Architecture:** First-class support for custom pipe filters (`Filter`), tag/block directives (`Directive`), and modular extensions (`SiluetPlugin`).

- **Themes & Caching:** One engine serves one or many themes (`SiluetTheme`), e.g. one per tenant. Each theme has its own asset loader, translations and a bounded, thread-safe cache of compiled views, templates and section groups, with per-theme and engine-wide limits, LRU and idle eviction (`CacheConfig`).

- **Pluggable Asset Loading:** Storage-agnostic asset resolution (`SiluetAssetLoader`) supporting classpath resources (`ResourceSiluetAssetLoader`), local filesystems, or remote stores.

- **Pagination & Context Primitives:** Native data structures (`PaginationContext`, `PaginationSource`) and execution state (`RenderContext`) with locale and timezone awareness.

**Quick Start**

**1. Engine Initialization**

Configure an engine instance using the `Siluet` companion factory. Everything configured here is shared by all themes:

```kotlin
import dev.siluet.Siluet
import dev.siluet.loader.ResourceSiluetAssetLoader
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

val siluet = Siluet {
    // Guard against runaway iterations
    maxLoopIterations = 1_000

    // Limit the theme caches
    cache {
        maxEntriesPerTheme = 500
        maxTotalEntries = 50_000
        evictIdleThemesAfter = 30.minutes
    }

    // Register custom filters
    filter("uppercase") { value, _, _ ->
        value?.toString()?.uppercase()
    }

    // Register custom directives
    directive("alert") { args, namedArgs, body, _, out ->
        val level = args.firstOrNull() ?: "info"
        val content = body?.invoke() ?: ""
        out.append("""<div class="alert alert-$level">$content</div>""")
    }
}
```

**2. Theme**

```kotlin
// Specify where templates, layouts, components and translations are loaded from
val theme = siluet.theme {
    assetLoader = ResourceSiluetAssetLoader(prefix = "theme")
}
```

**3. Rendering**

```kotlin
// Render an orchestrated template defined in theme/templates/home.json
val pageHtml = theme.render("home", viewModel) {
    locale = Locale.US
    zoneId = ZoneId.of("America/New_York")
}
```

A `Siluet` instance and its themes are thread-safe and are meant to be shared: create them once and reuse them for all renders. After an asset changes at runtime, call `theme.invalidate(SiluetAssetType.SECTION, "hero")` or `theme.clearCache()`.

**Architecture & Concepts**

**Compilation & Caching**

1. **Source Loading:** The theme's `SiluetAssetLoader` locates the raw markup or JSON definition.
2. **Lexing & Parsing:** The raw source is transformed into an abstract syntax tree.
3. **Caching:** The compiled view is stored in the theme's bounded cache (LRU eviction, per-theme and engine-wide limits set via `cache { }`) to eliminate parsing overhead on subsequent renders. Parsed JSON templates, section groups and translations are cached the same way. An evicted entry is compiled again on next use.
4. **Evaluation:** `RenderContext` and the reflection-based property accessor bind model data to the AST, outputting the final string.

```text
Template Source ──▶ Lexer & Parser ──▶ Compiled view (AST) ──▶ Theme cache
                                             │
Model Data + RenderContext ──────────────────┴─────────────▶ Rendered Output
```

**Security & Reflection Policy**

By default, template expressions evaluate properties via `CachedReflectionPropertyAccessor` secured by `DefaultSecurityPolicy`. Any attempt to traverse dangerous reflection paths or invoke forbidden methods throws a `SiluetSecurityException`:

```kotlin
val strictSecurity = DefaultSecurityPolicy(
    blockedMembers = setOf("passwordHash", "internalSecret"),
    blockedPackages = setOf("com.mycompany.app.internal")
)

val engine = Siluet {
    securityPolicy = strictSecurity
}
```

**Package Overview**

| Package | Primary Role |
| :--- | :--- |
| [`dev.siluet`](dev.siluet/index.html) | Root entry point (`Siluet`, `SiluetConfig`) and source locations. |
| [`dev.siluet.theme`](dev.siluet.theme/index.html) | Themes (`SiluetTheme`) and their configuration (`SiluetThemeConfig`): rendering, cache invalidation and statistics. |
| [`dev.siluet.cache`](dev.siluet.cache/index.html) | Cache limits (`CacheConfig`) and cache statistics (`SiluetCacheStats`). |
| [`dev.siluet.filter`](dev.siluet.filter/index.html) | Expression pipe transformation contract (`Filter`). |
| [`dev.siluet.directive`](dev.siluet.directive/index.html) | Statement/block directive contract (`Directive`). |
| [`dev.siluet.render`](dev.siluet.render/index.html) | Render context (`RenderContext`) with locale and time zone. |
| [`dev.siluet.reflect`](dev.siluet.reflect/index.html) | Property resolution (`PropertyAccessor`) and reflection sandbox policies (`SecurityPolicy`, `DefaultSecurityPolicy`). |
| [`dev.siluet.loader`](dev.siluet.loader/index.html) | Asset discovery contracts (`SiluetAssetLoader`), classpath loader (`ResourceSiluetAssetLoader`) and asset kinds (`SiluetAssetType`). |
| [`dev.siluet.pagination`](dev.siluet.pagination/index.html) | Paginated data wrappers (`PaginationContext`) and page fetching abstractions (`PaginationSource`). |
| [`dev.siluet.plugin`](dev.siluet.plugin/index.html) | Modular plugin contract (`SiluetPlugin`) for bundling filters, directives, and configuration. |
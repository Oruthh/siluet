# Module core

Core execution engine and foundational abstractions for the **Siluet** templating system.

The `:core` module provides compilation, AST parsing, runtime evaluation, security sandboxing, and caching primitives for dynamic HTML rendering.

**Key Features**

- **Orchestrated Templates:** Render composite pages configured via JSON definitions (`templates/<name>.json`), binding layouts, dynamic sections, and section groups.

- **Security & Sandboxing:** Multi-tier reflection guardrails via `SecurityPolicy` preventing access to forbidden classes (`System`, `Runtime`, `ProcessBuilder`), internal packages, and dangerous members.

- **Extensible Architecture:** First-class support for custom pipe filters (`Filter`), tag/block directives (`Directive`), and modular extensions (`SiluetPlugin`).

- **Built-in AST Caching:** High-throughput `ViewCache` SPI with a thread-safe `InMemoryViewCache` implementation.

- **Pluggable Asset Loading:** Storage-agnostic asset resolution (`SiluetAssetLoader`) supporting classpath resources (`ResourceSiluetAssetLoader`), local filesystems, or remote stores.

- **Pagination & Context Primitives:** Native data structures (`PaginationContext`, `PaginationSource`) and execution state (`RenderContext`) with locale and timezone awareness.

**Quick Start**

**1. Engine Initialization**

Configure an engine instance using the `Siluet` companion factory:

```kotlin
import dev.siluet.Siluet
import dev.siluet.loader.ResourceSiluetAssetLoader
import java.time.ZoneId
import java.util.Locale

val siluet = Siluet {
    // Specify where templates, layouts, and components are loaded from
    assetLoader = ResourceSiluetAssetLoader(prefix = "theme")

    // Guard against runaway iterations
    maxLoopIterations = 1_000

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

**2. Rendering Operations**

```kotlin
// 1. Render an orchestrated template defined in theme/templates/home.json
val pageHtml = siluet.render("home", viewModel) {
    locale = Locale.US
    zoneId = ZoneId.of("America/New_York")
}

// 2. Render an individual partial or component directly
val cardHtml = siluet.renderView("components/productCard", productModel)

// 3. Render raw template markup from memory
val renderedSnippet = siluet.renderString(
    source = "Hello, {{ user.name | uppercase }}!",
    model = mapOf("user" to mapOf("name" to "Alice"))
)
```

**Architecture & Concepts**

**Compilation & Caching**

1. **Source Loading:** `SiluetAssetLoader` locates the raw markup or JSON definition.
2. **Lexing & Parsing:** The raw source is transformed into an AST of `SiluetNode` elements wrapped within a `SiluetView`.
3. **Caching:** The resulting `SiluetView` is stored in `ViewCache` to eliminate parsing overhead on subsequent evaluations.
4. **Evaluation:** `RenderContext` and `PropertyAccessor` bind model data to the AST, outputting the final string.

```text
Template Source ──▶ Lexer & Parser ──▶ SiluetView (AST) ──▶ ViewCache
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
| [`dev.siluet`](dev.siluet/index.html) | Root entry point (`Siluet`, `SiluetBuilder`), compilation wrapper (`SiluetView`), context, and source locations. |
| [`dev.siluet.cache`](dev.siluet.cache/index.html) | AST caching abstraction (`ViewCache`) and heap implementation (`InMemoryViewCache`). |
| [`dev.siluet.filter`](dev.siluet.filter/index.html) | Expression pipe transformation contract (`Filter`). |
| [`dev.siluet.render`](dev.siluet.render/index.html) | Statement/block directive contract (`Directive`), context builder (`RenderContext`), and property resolution (`PropertyAccessor`). |
| [`dev.siluet.reflect`](dev.siluet.reflect/index.html) | Reflection sandbox policies (`SecurityPolicy`, `DefaultSecurityPolicy`) and security exceptions. |
| [`dev.siluet.loader`](dev.siluet.loader/index.html) | Asset discovery contracts (`SiluetAssetLoader`) and classpath loader (`ResourceSiluetAssetLoader`). |
| [`dev.siluet.pagination`](dev.siluet.pagination/index.html) | Paginated data wrappers (`PaginationContext`) and page fetching abstractions (`PaginationSource`). |
| [`dev.siluet.plugin`](dev.siluet.plugin/index.html) | Modular plugin contract (`SiluetPlugin`) for bundling filters, directives, and configuration. |
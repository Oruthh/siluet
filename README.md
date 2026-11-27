<p align="center">
  <img src="baner.svg" alt="Siluet" width="420">
</p>

<p align="center">
  <strong>An AST-based templating engine for Kotlin.</strong><br>
  Compose pages from sections in JSON, write views in plain HTML.
</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF.svg?logo=kotlin&logoColor=white" alt="Kotlin 2.x"></a>
  <a href="https://openjdk.org"><img src="https://img.shields.io/badge/Java-21+-ED8B00.svg?logo=openjdk&logoColor=white" alt="Java 21+"></a>
  <a href="https://opensource.org/licenses/MPL-2.0"><img src="https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg" alt="License: MPL 2.0"></a>
  <a href="https://central.sonatype.com/artifact/dev.siluet/siluet-core"><img src="https://img.shields.io/maven-central/v/dev.siluet/siluet-core.svg" alt="Maven Central"></a>
</p>

<p align="center">
  <a href="https://siluet.dev/">Documentation</a> ·
  <a href="https://siluet.dev/guide/getting-started.html">Getting Started</a> ·
  <a href="https://siluet.dev/reference/syntax.html">Template Syntax</a> ·
  <a href="https://siluet.dev/api/index.html">API Reference</a>
</p>

---

> [!WARNING]
> **Work in progress.** Siluet is in early development and is **not yet production-ready**. APIs, template syntax and internal architecture may change between releases. Use it for experimentation, testing and feedback.

Siluet is a templating engine for e-commerce themes and modular web layouts, written in Kotlin (Java 21+, Kotlin 2.x).

Instead of monolithic page templates, Siluet separates **what is on a page** from **how it looks**. JSON templates list the sections of a page, their order, properties and blocks. Layouts, sections and components are plain HTML with a small set of directives. This makes pages easy to rearrange, both by developers and, in the future, by merchants in a visual theme editor.

## Features

- **Pages composed in JSON.** Templates and section groups describe which sections a page contains, in which order and with which properties and blocks.
- **Views in plain HTML.** Inline `<style>` and `<script>`, JSON-LD, `mailto:` links and JSON in `data-*` attributes work as written. Template syntax is only active where it belongs.
- **Compiled to an AST.** Views are parsed once into a syntax tree, cached and reused across requests. Syntax errors point to the file, line and column.
- **Safe by default.** Every printed value is HTML-escaped, templates can only read getters, and a security policy blocks reflection, I/O and other dangerous classes.
- **Built-in localization.** JSON dictionaries per locale, the `t` filter in views and `t:`-prefixed properties in templates.
- **Extensible.** Custom filters, directives and plugins, and asset loaders that read themes from the classpath, a database or any other store.
- **Many themes, one engine.** Each theme (e.g., one per tenant) has its own files, translations and bounded cache, while filters, directives and settings are shared.

## At a Glance

A template lists the sections of a page (`templates/home.json`):

```json
{
  "layout": "shop",
  "sections": {
    "hero": { "name": "banner", "properties": { "title": "t:home.hero.title", "color": "#3b5bdb" } },
    "products": { "name": "featuredProducts" }
  }
}
```

Sections are plain HTML (`sections/banner.html`, `sections/featuredProducts.html`):

```html
<section class="hero">
    <style>
        .hero { background: {{ section.properties.color }}; }
        @media (max-width: 600px) { .hero { padding: 1rem; } }
    </style>
    <h1>{{ section.properties.title }}</h1>
</section>
```

```html
<section class="products">
    @for(product in featuredProducts) {
        @component("productCard", item = product)
    }
</section>
```

Render the page from Kotlin:

```kotlin
val siluet = Siluet {}

val theme = siluet.theme {
    assetLoader = ResourceSiluetAssetLoader("theme")
}

// model: your view model, e.g. a data class with featuredProducts
val html = theme.render("home", model) {
    locale = Locale.ENGLISH
}
```

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.siluet:siluet-core:0.0.5")
}
```

```xml
<dependency>
    <groupId>dev.siluet</groupId>
    <artifactId>siluet-core</artifactId>
    <version>0.0.5</version>
</dependency>
```

Continue with the **[Getting Started guide](https://siluet.dev/guide/getting-started.html)** to build a complete storefront page.

## Theme Structure

```text
theme/
├── templates/          # JSON: layout and sections of each page type
├── layouts/            # HTML page skeletons with @sections() mount points
├── sections/           # HTML sections
│   └── groups/         # JSON: reusable groups of sections (footer, sidebar...)
├── components/         # HTML components, included with @component(...)
└── locales/            # JSON translation dictionaries (en.json, pl.json...)
```

## Syntax Overview

| Feature | Syntax | Description |
| :--- | :--- | :--- |
| Output | `{{ product.name }}` | Prints a value, HTML-escaped. |
| Filters | `{{ order.date \| date("long") }}` | Transforms a value; filters can be chained. |
| Conditionals | `@if(user) { ... } @else { ... }` | Renders a block when the condition is true. |
| Loops | `@for(item in items) { ... }` | Iterates over collections, arrays, maps and sequences. |
| Components | `@component("card", item = product)` | Includes a component with its own arguments. |
| Section slots | `@sections()` / `@sections("footer")` | Mounts the page's sections or a section group. |
| Pagination | `@paginate(items as page by 10, page = current) { ... }` | Splits a list into pages. |
| Translations | `{{ "store.addToCart" \| t }}` | Prints a translation for the render locale. |
| Literal `@` | `hello\@shop.com` | Escapes `@`, `{` and `}` in text between tags. |

Directives work in HTML content and inside tags. Inside attribute values, `<style>` and `<script>` everything except `{{ }}` is copied verbatim, so CSS at-rules and e-mail addresses in links need no escaping. See [Template Syntax](https://siluet.dev/reference/syntax.html).

## Vision: A Visual Theme Editor

The goal of Siluet is to power a **live, no-code theme builder** for e-commerce, similar to modern store customizers:

- **Agencies and developers** build modular themes with HTML and Siluet directives.
- **Merchants** rearrange sections, swap components and change settings in a visual editor, without touching code.

The planned building blocks:

1. **The `@schema` directive** in section files, describing the settings a section offers (text inputs, color pickers, toggles).
2. **Section `properties`** in JSON, holding the values chosen by the merchant.
3. **Pluggable data sources**, so templates and properties can be stored in a database instead of files.
4. **A visual builder** that turns `@schema` into an admin UI with live preview and drag-and-drop sections.

## Roadmap to v0.1.0

- [x] Core AST compiler, lexer and parser
- [x] Basic directives (`@if`, `@else`, `@for`, `@sections`, `@component`, `@paginate`)
- [x] HTML-aware template syntax (inline CSS and JavaScript, JSON-LD, attribute values)
- [x] Basic text filters (`rawHTML`, `default`, `lowercase`, `uppercase`, `capitalize`, `truncate`)
- [x] Basic data filters (`date`)
- [x] Localization filter (`t`)
- [x] Dynamic section and block properties in templates (`section.properties`, `block.properties`)
- [x] Kotlin API reference (KDoc)
- [x] Template syntax and directives guide
- [x] Multiple themes per engine with bounded, per-theme caches
- [ ] HTML output beautifier and whitespace management
- [ ] `@schema` directive for section configuration and metadata
- [ ] IntelliJ IDEA plugin with syntax highlighting

## Documentation

The documentation is published at **[siluet.dev](https://siluet.dev/)**. It combines:

1. **VitePress** for guides and reference pages (`docs/`).
2. **Dokka** for the Kotlin API reference generated from KDoc.

To build and preview the complete site locally, run from the project root:

```bash
./gradlew previewSite
```

This task installs the NPM dependencies, builds the VitePress site, generates the Dokka API reference, assembles both into `build/site/` and serves it at `http://localhost:3080`.

## Contributing

Contributions are very welcome. Siluet is in active development, so feedback, bug reports and pull requests all help.

1. **Report issues.** Found a bug or missing feature? [Open an issue](https://github.com/Oruthh/siluet/issues).
2. **Submit pull requests.**
    - Fork the repository and create a branch (`git checkout -b feature/awesome-feature`).
    - Make your changes and cover them with tests.
    - Commit (`git commit -m "feat: add awesome feature"`) and push the branch.
    - Open a pull request.

## License

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

Unless otherwise noted, this notice applies to all files in this repository.
See [LICENSE](LICENSE) for the full license text.

The Siluet logo and banner images, including `logo.svg`, `baner.svg` and their
copies in the documentation, are not covered by the MPL and may not be used
without permission. You are welcome to refer to the project by its name, but
please do not use the name Siluet for forks or in a way that suggests official
endorsement.
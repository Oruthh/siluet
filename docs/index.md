---
layout: home

hero:
  name: Siluet
  text: Templates for modular storefronts
  tagline: An AST-based templating engine for Kotlin. Compose pages from sections in JSON, write views in plain HTML.
  image:
    src: /images/logo.svg
    alt: Siluet
  actions:
    - theme: brand
      text: Get started
      link: /guide/getting-started
    - theme: alt
      text: Template syntax
      link: /reference/syntax
    - theme: alt
      text: View on GitHub
      link: https://github.com/Oruthh/siluet

features:
  - icon: 🧩
    title: Pages composed in JSON
    details: Templates list the sections of a page, their order, properties and blocks. Rearrange a page without touching HTML.
    link: /reference/template-configuration
    linkText: Template configuration
  - icon: 📄
    title: Views in plain HTML
    details: Inline CSS, JavaScript, JSON-LD and attributes work as written. Template syntax is only active where it belongs.
    link: /reference/syntax
    linkText: Template syntax
  - icon: 🛡️
    title: Safe by default
    details: Every printed value is escaped, templates can only read getters, and a security policy blocks dangerous classes.
    link: /reference/data-model
    linkText: Data model & security
  - icon: 🌍
    title: Built-in localization
    details: JSON dictionaries per locale, the t filter in views and t:-prefixed properties in templates.
    link: /reference/i18n
    linkText: Localization
  - icon: 🛠️
    title: Extensible
    details: Add your own filters, directives and plugins, and load themes from files, a database or any other store.
    link: /reference/extending
    linkText: Extending Siluet
  - icon: 🎨
    title: Built for theme editors
    details: Designed as the engine behind visual, no-code theme builders where merchants arrange sections themselves.
---

::: warning Work in progress
Siluet is in early development and is **not yet production-ready**. APIs, template syntax and internal architecture may change between releases.
:::

## At a Glance

A template lists the sections of a page in JSON:

```json
{
  "layout": "shop",
  "sections": {
    "hero": { "name": "banner", "properties": { "title": "t:home.hero.title", "color": "#3b5bdb" } },
    "products": { "name": "featuredProducts" }
  }
}
```

Each section is plain HTML with directives and print expressions:

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

Render it from Kotlin:

```kotlin
val siluet = Siluet {
    assetLoader = ResourceSiluetAssetLoader("theme")
}

// model: your view model, e.g. a data class with featuredProducts
val html = siluet.render("home", model) {
    locale = Locale.ENGLISH
}
```

Continue with [Getting Started](/guide/getting-started) to build a complete storefront page.

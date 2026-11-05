# Siluet

> **AST-based Templating Engine for Kotlin**  
> Decoupled JSON page orchestration meets native HTML components.

> ⚠️ **Warning: Work in Progress**
> Siluet is currently in active early development and is **not yet production-ready**. APIs, template syntax, and internal architecture are subject to breaking changes. Use it for experimentation, testing, and feedback.


[View on GitHub](https://github.com/Oruthh/siluet)

---

## Key Features

* **🧩 JSON-Driven Orchestration**  
  Assemble layouts, section hierarchies, and dynamic section groups declaratively without monolithic templates.

* **⚡ Compiles to AST**  
  Powered by a dedicated Lexer and Parser that builds an Abstract Syntax Tree for execution speed and strict syntax guarantees.

* **🎯 Modern Component Tree**  
  Write layouts, sections, and reusable components in clean HTML with intuitive directives like `@sections`, `@component`, and `@for`.

* **🌍 Built-in Internationalization**  
  Native dictionary-based i18n support out of the box using simple `@t` and `@translate` directives.

* **🛠️ Extensible Runtime**  
  Tailored for modern Kotlin and Java 21+. Customize asset loaders, filters, data models, and context resolvers.

* **🎨 Built for Visual Builders**  
  Designed from the ground up to power dynamic theme builders and headless CMS visual customizers.
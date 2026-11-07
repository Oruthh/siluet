# Siluet

> ⚠️ **Warning: Work in Progress**
> Siluet is currently in active early development and is **not yet production-ready**. APIs, template syntax, and internal architecture are subject to breaking changes. Use it for experimentation, testing, and feedback.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-21+-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

Siluet is a modern, AST-based templating engine for Kotlin (Java 21+, Kotlin 2.x), designed for e-commerce themes and modular web layouts.

Instead of monolithic templates, Siluet decouples page orchestration from rendering: templates and section hierarchies are configured in clean JSON files, while layouts, sections, and components are written in native HTML with concise template directives.

---

## Key Features

* **JSON-Driven Page Orchestration:** Assemble layouts, dynamic sections, and section groups declaratively via JSON schemas.
* **Component-Based Architecture:** Reusable HTML components and sections keep templates modular and maintainable.
* **Compiles to AST:** Built on a dedicated Lexer and Parser that compiles templates into an Abstract Syntax Tree for high execution efficiency.
* **Built-in Internationalization:** Native locale dictionary support via the `@t` directive.
* **Extensible & Type-Safe:** Built natively for Kotlin with support for custom loaders, data models, filters, and directives.

---

## Project Vision: The Visual Theme Editor

The ultimate goal of Siluet is to serve as the core engine for a **live, no-code theme builder** (similar to modern e-commerce customizers). It aims to bridge the gap between developers and non-technical end-users:

* **For Agencies:** Build robust, modular themes using standard HTML and Siluet directives.
* **For Store Owners:** Customize layouts, swap components, and change styling in a visual editor without touching the code.

To achieve this, the future architecture will introduce:
1. **The `@schema` Directive:** Embedded directly within `.html` component and section files to define configurable UI fields (e.g., color pickers, text inputs, toggles).
2. **Stateful `properties`:** JSON configuration files will utilize a `properties: {}` block to store the user's customized values based on the schema.
3. **Pluggable Data Sources:** While currently file-based, the engine will allow fetching layouts and properties dynamically from a database.
4. **Live Visual Builder:** A frontend application that parses `@schema` tags to generate an admin UI, allowing real-time previews and drag-and-drop section arrangement.

---

## Theme Structure

Siluet enforces a clear directory structure for themes:

```text
theme/
├── templates/          # JSON files defining the layout and active sections
├── layouts/            # Base HTML page skeletons
├── sections/           # Standalone HTML sections
│   └── groups/         # JSON files defining reusable section groups
├── components/         # Reusable HTML sub-components
└── locales/            # JSON translation dictionaries (e.g., en.json)
```

---

## Getting Started

To see Siluet in action, learn about the theme architecture, and build your first storefront template, please refer to our official guide:

👉 **[Read the Getting Started Guide](docs/guide/getting-started.md)**

---

## Syntax Overview

| Feature | Syntax Example                        | Description | 
| ----- |---------------------------------------| ----- | 
| **Variable Output** | `{{ projectName }}`                   | Outputs an interpolated variable into the template. | 
| **Filters** | `{{ date \| date("short") }}`         | Passes expressions through transformation filters. | 
| **Conditionals** | `@if(showDate) { ... } @else { ... }` | Conditional block evaluation. | 
| **Loops** | `@for(item in items) { ... }`         | Iteration over collections and iterables. | 
| **Components** | `@component("name", key = value)`     | Injects a component with scoped properties. | 
| **Section Slots** | `@sections()` or `@sections("group")` | Injects page-level sections or section groups. | 
| **Translations** | `@t("translation_key")`               | Resolves localized strings from the active locale file. | 
---

## Roadmap to v0.1.0
- [x] Core AST compiler, Lexer, and Parser
- [x] Basic directives (`@if`, `@else`, `@for`, `@sections`, `@component`, `@t`, `@translate`, `@paginate`)
- [x] Basic text filters (`rawHTML`, `default`, `lowercase`, `uppercase`, `capitalize`, `truncute`)
- [x] Basic data filters (`date`)
- [ ] HTML output beautifier and whitespace management
- [ ] Implement `@schema` directive for section configuration and metadata
- [x] Support dynamic section & block properties in templates (`section.properties`, `block.properties`)
- [x] Publish Kotlin API reference (KDoc)
- [x] Write template syntax and directives guide
- [ ] IntelliJ IDEA plugin with syntax highlighting

---

## Documentation

Siluet's documentation is unified into a single static site, combining:

1. **VitePress:** For guides, tutorials, and architecture overviews.
2. **Dokka:** For the auto-generated Kotlin API reference (KDoc).

To build and preview the complete documentation site locally, run the following Gradle task from the project root:

```bash
./gradlew previewSite
```

This custom task will automatically:
- Install NPM dependencies and build the VitePress site.
- Generate the HTML API reference via Dokka.
- Assemble both into `build/site/`.
- Start a local JDK web server (using `SimpleFileServer`) and open `http://localhost:3080` in your default web browser.
---

## Contributing

Contributions are highly welcome! Since Siluet is in active development, your feedback, bug reports, and code contributions are greatly appreciated.

### How to contribute:
1. **Report Issues:** If you find a bug or have a feature request, please open an issue in the repository.
2. **Submit Pull Requests:**
    - Fork the repository.
    - Create a new branch for your feature or bugfix (`git checkout -b feature/awesome-feature`).
    - Make your changes and ensure your code is well-tested.
    - Commit your changes (`git commit -m "Add awesome feature"`).
    - Push to the branch (`git push origin feature/awesome-feature`).
    - Open a Pull Request!

## License

This project is licensed under the MPL 2.0 License.
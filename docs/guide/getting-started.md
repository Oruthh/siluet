# Getting Started with Siluet

Siluet is a component and section-driven templating engine for Kotlin. It separates structural layouts, dynamic sections, and reusable components using simple JSON orchestration and HTML templates.

### Project Structure Overview

Siluet resolves templates using an asset loader. While the built-in loader fetches from your project resources (e.g., `src/main/resources/theme/`), you can easily implement a custom loader to fetch templates from anywhere—such as a database, network API, or external file system.

Here is an example of a simple storefront setup using the resource loader:

```text
resources/theme/
├── locales/
│   └── en.json                  # Translation dictionaries
├── layouts/
│   └── shop.html                # Base HTML skeleton & mount points
├── templates/
│   └── home.json                # Template configuration & section mapping
├── sections/
│   ├── featuredProducts.html    # Loop logic & component calls
│   ├── newsletter.html          # Newsletter signup section
│   ├── copyright.html           # Footer legal & copyright info
│   └── groups/
│       └── footer.json          # Composed section groups
└── components/
    └── productCard.html         # Atomic, reusable UI snippets
```

## 1. Quick Start

### Step 1: Define the Data Model

Create standard Kotlin data classes representing the view model. In this case, we are building a simple storefront:

```kotlin
package dev.siluet.quickstart

import java.time.LocalDateTime

data class Product(
    val id: String,
    val name: String,
    val price: Double
)

data class StorefrontModel(
    val storeName: String,
    val activeUser: String?,
    val generatedAt: LocalDateTime,
    val featuredProducts: List<Product>
)
```

### Step 2: Configure the Engine & Render

Initialize `Siluet` with an asset loader, populate your model with real data, and render the target template:

```kotlin
package dev.siluet.quickstart

import dev.siluet.Siluet
import dev.siluet.loader.ResourceSiluetAssetLoader
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

fun main() {
    // 1. Initialize the engine pointing to the resources directory
    // (You can replace this with your own custom asset loader implementation)
    val siluet = Siluet {
        assetLoader = ResourceSiluetAssetLoader("theme")
    }

    // 2. Prepare the model data
    val model = StorefrontModel(
        storeName = "Siluet Tech Store",
        activeUser = "Jane Doe",
        generatedAt = LocalDateTime.now(),
        featuredProducts = listOf(
            Product("p_101", "Mechanical Keyboard", 129.99),
            Product("p_102", "Wireless Mouse", 59.99),
            Product("p_103", "USB-C Hub", 34.50)
        )
    )

    // 3. Render the "home" template configuration
    // The locale determines which translation file (e.g., en.json) is loaded.
    val rendered = siluet.render("home", model) {
        locale = Locale.ENGLISH
        zoneId = ZoneId.of("Europe/London")
    }

    println(rendered)
}
```

## 2. Template Architecture

### A. Template Configuration (`templates/home.json`)

Binds the base layout to target sections. Keys represent injection IDs, while `"name"` points to the specific file in `sections/`:

```json
{
  "layout": "shop",
  "sections": {
    "productsGrid": {
      "name": "featuredProducts"
    }
  }
}
```

### B. Layout Skeleton (`layouts/shop.html`)

Defines the top-level HTML document and mounts configured sections via `@sections()`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ storeName }}</title>
</head>
<body>
    <header class="navbar">
        <h1>{{ storeName }}</h1>
        <div class="user-profile">
            Welcome, {{ activeUser | default("Guest") }}!
        </div>
    </header>

    <!-- Mounts all default sections configured in templates/home.json -->
    <main class="container">
        @sections()
    </main>

    <!-- Mounts section groups defined under sections/groups/footer.json -->
    <footer class="site-footer">
        @sections("footer")
    </footer>
</body>
</html>
```

### C. Sections (`sections/featuredProducts.html`)

Sections contain dynamic markup, data iteration, and component composition. Here we loop through our products to display a grid:

```html
<section class="featured-products">
    <h2>Trending Now</h2>
    
    <div class="product-grid">
        @for(product in featuredProducts) {
            @component("productCard", item = product)
        }
    </div>
</section>
```

### D. Section Groups (`sections/groups/footer.json`)

Compose multiple reusable sections under a single named mount slot. This is perfect for complex footers or sidebars:

```json
{
  "sections": {
    "newsletterForm": {
      "name": "newsletter"
    },
    "legalInfo": {
      "name": "copyright"
    }
  }
}
```

#### Group Section: Newsletter (`sections/newsletter.html`)

```html
<section class="newsletter">
    <h3>Stay Updated</h3>
    <form class="newsletter-form">
        <input type="email" placeholder="Enter your email" required>
        <button type="submit" class="btn-secondary">
            @t("store.actions.subscribe")
        </button>
    </form>
</section>
```

#### Group Section: Copyright (`sections/copyright.html`)

```html
<div class="legal-info">
    <p>&copy; {{ storeName }}. All rights reserved.</p>
</div>
```

### E. Translations & Localization (`locales/en.json`)

Siluet supports localization out of the box using JSON dictionaries placed in the `/locales` directory. The structure of the JSON file is flattened during loading, allowing you to access nested keys using dot notation via the `@t()` directive.

For example, your `en.json` file:

```json
{
  "store": {
    "actions": {
      "addToCart": "Add to Cart"
    }
  }
}
```

### F. Atomic Components (`components/productCard.html`)

Encapsulated UI fragments accepting local arguments passed from parent sections. In this example, we use `@t()` to fetch the localized button text defined in our JSON dictionary:

```html
<article class="card">
    <div class="card-body">
        <h3 class="product-title">{{ item.name }}</h3>
        <p class="product-price">$ {{ item.price }}</p>
    </div>
    <div class="card-footer">
        <button onclick="addToCart('{{ item.id }}')" class="btn-primary">
            @t("store.actions.addToCart")
        </button>
    </div>
</article>
```
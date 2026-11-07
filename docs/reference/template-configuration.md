# Template Configuration (JSON)

In Siluet, templates are not monolithic HTML files. Instead, they are declarative JSON configuration files that orchestrate how layouts, sections, and blocks are combined to build a page.

## 1. The Template Structure

A template represents a single page type (e.g., `home.json`, `product.json`). It defines which base layout to use and maps specific sections into the layout's mount points.

### Schema

* **`layout`** *(String)*: The name of the base HTML layout file to use (defaults to `"main"`).

* **`sections`** *(Object)*: A dictionary (map) of sections to be injected into the layout. The keys act as unique identifiers for the sections.

* **`sectionsOrder`** *(Array of Strings, optional)*: Explicitly defines the rendering order of the sections.

> **Note on Ordering:** By default, Siluet renders sections in the exact order they are declared inside the `sections` object. The `sectionsOrder` array is an optional helper feature. It allows you to quickly reorder the layout without having to cut and paste large JSON blocks. If `sectionsOrder` is empty or omitted, it simply falls back to the original declaration order.

**Example: `templates/home.json`**

```json
{
  "layout": "shop",
  "sectionsOrder": ["heroSection", "productsGrid"],
  "sections": {
    "heroSection": {
      "name": "heroBanner"
    },
    "productsGrid": {
      "name": "featuredProducts"
    }
  }
}
```

## 2. Sections and Blocks

Sections are the primary building blocks of your UI. A section can define its own **properties** (arguments passed to the HTML template) and can contain smaller, repeatable child elements called **blocks**.

### Section Definition Schema

* **`name`** *(String, required)*: The name of the HTML file in the `sections/` directory to render.

* **`properties`** *(Object, optional)*: A dictionary of key-value pairs passed directly to the section's rendering context.

* **`blocks`** *(Object, optional)*: A dictionary of child blocks nested within this section.

* **`blocksOrder`** *(Array of Strings, optional)*: An optional array to easily reorder blocks without modifying the `blocks` dictionary (works identically to `sectionsOrder`).

### Block Definition Schema

Blocks are very similar to sections but are typically used for repeatable elements inside a section (e.g., slides in a slider, columns in a grid).

* **`name`** *(String, required)*: The identifier/name of the block type.

* **`properties`** *(Object, optional)*: A dictionary of key-value pairs passed to the block.

**Example: A section with properties and blocks**

```json
{
  "name": "featuresGrid",
  "properties": {
    "title": "Why Choose Us?",
    "showBackground": true
  },
  "blocksOrder": ["feature2", "feature1"],
  "blocks": {
    "feature1": {
      "name": "featureItem",
      "properties": {
        "icon": "shield",
        "description": "100% Secure"
      }
    },
    "feature2": {
      "name": "featureItem",
      "properties": {
        "icon": "zap",
        "description": "Lightning Fast"
      }
    }
  }
}
```

## 3. Section Groups

Sometimes you need to reuse the same set of sections across multiple templates (for example, a complex footer or sidebar). Instead of duplicating the section definitions in every template, you can define a **Sections Group**.

A Section Group uses the same structure as a Template but omits the `layout` property. These are typically saved in `sections/groups/`.

To render a group in your HTML layout, you use the `@sections()` directive and pass the group's name as an argument.

**Example: `sections/groups/footer.json`**

```json
{
  "sectionsOrder": ["newsletter", "copyright"],
  "sections": {
    "newsletter": { "name": "newsletterForm" },
    "copyright": { "name": "legalText" }
  }
}
```

To mount this specific group inside your HTML layout (e.g., `layouts/shop.html`), you would write:

```html
<footer class="site-footer">
    @sections("footer")
</footer>
```

## 4. Properties and Data Types

When defining `properties` on a Section or Block, Siluet's compiler automatically infers the data type based on the JSON primitive. You can access these properties directly as variables inside your HTML templates.

Supported JSON primitive types:

* **String**: `"Hello World"`
* **Integer/Long**: `42`, `9000000000`
* **Double**: `99.99`
* **Boolean**: `true`, `false`

### The Translation Prefix (`t:`)

Siluet features deep integration with the localization engine. If a property value is a string that starts with the `t:` prefix, the compiler will intercept it and automatically resolve the corresponding translation key from your dictionaries (e.g., `locales/en.json`).

```json
{
  "name": "heroBanner",
  "properties": {
    "heading": "t:home.hero.title",
    "subheading": "t:home.hero.subtitle",
    "callToActionUrl": "/shop",
    "autoPlay": true,
    "interval": 5000
  }
}
```

In your `heroBanner.html` template, the `heading` variable will contain the fully translated string, not the raw `"t:..."` value!

## 5. Complete Template Example

Putting it all together, here is what a full, production-ready template configuration might look like:

```json
{
  "layout": "storefront",
  "sectionsOrder": ["hero", "promo", "testimonials"],
  "sections": {
    "hero": {
      "name": "slider",
      "properties": {
        "fullWidth": true
      },
      "blocksOrder": ["slide1", "slide2"],
      "blocks": {
        "slide1": {
          "name": "imageSlide",
          "properties": { "imageUrl": "/assets/promo1.jpg", "text": "t:promo.summerSale" }
        },
        "slide2": {
          "name": "imageSlide",
          "properties": { "imageUrl": "/assets/promo2.jpg", "text": "t:promo.newArrivals" }
        }
      }
    },
    "promo": {
      "name": "newsletter",
      "properties": {
        "buttonColor": "#FF5733"
      }
    },
    "testimonials": {
      "name": "reviewsGrid"
    }
  }
}
```
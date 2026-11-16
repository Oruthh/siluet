# Template Configuration (JSON)

In Siluet, templates are not monolithic HTML files. Instead, they are declarative JSON configuration files that orchestrate how layouts, sections, and blocks are combined to build a page.

## 1. The Template Structure

A template represents a single page type (e.g., `home.json`, `product.json`). It defines which base layout to use and maps specific sections into the layout's mount points.

### Schema

* **`layout`** *(String)*: The name of the base HTML layout file to use (defaults to `"main"`).

* **`sections`** *(Object)*: A dictionary (map) of sections to be injected into the layout. The keys act as unique identifiers for the sections.

* **`sectionsOrder`** *(Array of Strings, optional)*: Keys of sections that should be rendered first, in the listed order. See [Ordering Rules](#ordering-rules).

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

* **`blocksOrder`** *(Array of Strings, optional)*: Keys of blocks that should be rendered first, in the listed order. Follows the same [Ordering Rules](#ordering-rules) as `sectionsOrder`.

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

### Ordering Rules

`sectionsOrder` and `blocksOrder` let you reorder sections and blocks without cutting and pasting large JSON objects. Both follow the same rules, in templates and in section groups:

* Keys listed in the order are rendered **first**, in the listed order.
* Keys that are **not listed** are rendered **after them**, in the order they are declared in `sections` / `blocks`. Adding a new section without updating `sectionsOrder` therefore renders it at the end; nothing is skipped.
* An empty or omitted order renders everything in declaration order.
* A key that is **not defined** in `sections` / `blocks` is an error. This catches typos such as `"shiping"`.
* A key listed **more than once** is an error.

For example, with sections `hero`, `promo`, `footer` and `newsletter` declared in this order, `"sectionsOrder": ["footer"]` renders `footer`, `hero`, `promo`, `newsletter`.

Invalid orders fail the render with a `SiluetBlueprintException` that names the key, the section it belongs to and the file it comes from:

```text
'shiping' listed in blocksOrder of section 'features' is not defined. Defined: 'shipping', 'support'. in /theme/templates/home.json
```

See [Error Handling](/reference/errors) for the full list of exceptions.

## 3. Section Groups

Sometimes you need to reuse the same set of sections across multiple templates (for example, a complex footer or sidebar). Instead of duplicating the section definitions in every template, you can define a **Sections Group**.

A Section Group uses the same structure as a Template but omits the `layout` property. These are typically saved in `sections/groups/`. Its `sectionsOrder` follows the same [Ordering Rules](#ordering-rules); errors point to the group's file, not to the template that uses it.

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
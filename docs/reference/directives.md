# Template Directives

Directives in Siluet are special tags prefixed with the `@` symbol. They control the logic, flow, and composition of your templates, allowing you to build dynamic and complex views cleanly.

## Control Flow

### `@if` and `@else`

Conditionally render blocks of HTML based on a boolean expression or variable.

```html
<div class="stock-status">
    @if(product.inStock) {
        <span class="badge success">In Stock</span>
    } @else {
        <span class="badge danger">Out of Stock</span>
    }
</div>
```

### `@for`

Iterates over collections or iterables. This is commonly used for rendering lists of items, such as a product grid or shopping cart rows.

```html
<ul class="cart-items">
    @for(item in cartItems) {
        <li>{{ item.name }} - ${{ item.price }}</li>
    }
</ul>
```

**Supported sources:** collections and any `Iterable`, arrays, maps (iterated over their entries) and Kotlin `Sequence`. A `null` value renders nothing.

Lazy sources such as a `Sequence`, a stream or a database cursor are traversed exactly once, so single-use sequences are safe to pass to the model.

**Iteration limit:** each loop may run at most `maxLoopIterations` times (default: `1000`, configurable in `Siluet { }`). The limit is checked while iterating, so even an infinite sequence is stopped. When it is exceeded, the whole render is aborted with a `SiluetException` and no partial HTML is returned.

```kotlin
val siluet = Siluet {
    assetLoader = ResourceSiluetAssetLoader("theme")
    maxLoopIterations = 5_000
}
```

## Composition & Structure

### `@component`

Injects a reusable, self-contained HTML component. You can pass local arguments directly to the component, which keeps your UI atomic and modular.

```html
<div class="product-grid">
    @for(product in featuredProducts) {
        <!-- Injects components/productCard.html -->
        @component("productCard", item = product, showBadge = true)
    }
</div>
```

A component can render other components, including itself. This makes recursive structures such as a category tree possible:

```html
<!-- components/category.html -->
<li>
    {{ node.name }}
    @if(node.children) {
        <ul>
            @for(child in node.children) {
                @component("category", node = child)
            }
        </ul>
    }
</li>
```

**Render depth limit:** views can be nested at most `maxRenderDepth` levels deep (default: `32`, configurable in `Siluet { }`). Every `@component` and every section mounted with `@sections` adds one level; views rendered side by side do not add up. This stops infinite recursion, such as a component that includes itself unconditionally or two components that include each other. When the limit is exceeded, the render is aborted with a `SiluetException` that shows the repeating chain of views and where it started, for example:

```text
Maximum render depth (32) exceeded while rendering 'components/ping.html': components/ping.html → components/pong.html → components/ping.html (entered from layouts/main.html:1:2) at components/pong.html:1:5
```

### `@sections`

Defines mount points (slots) within your base layouts or wrapper components. Siluet replaces these directives with the sections configured in your template's JSON mapping.

* **`@sections()`**: Mounts the default/root sections defined for the current template.
* **`@sections("groupName")`**: Mounts a specific group of sections (e.g., from `sections/groups/groupName.json`).

```html
<!-- Inside layouts/shop.html -->
<main>
    @sections()
</main>

<aside class="sidebar">
    @sections("sidebar")
</aside>
```

Every mounted section counts toward the render depth limit (see [`@component`](#component)). Avoid mounting `@sections` inside a section that is itself part of the mounted sections, as it renders the section within itself.

## Data Handling & Pagination

### `@paginate`

A powerful directive for handling paginated collections directly in the template. It automatically chunks your list based on the provided page size and current page number.

The mapped alias (in this case, `page`) exposes a rich context object, giving you access not only to the items but also to metadata essential for building pagination controls.

**Syntax:** `@paginate(list as alias by pageSize, page = currentPageNumber)`

```html
<div class="reviews-section">
    <!-- Paginates the 'allReviews' list, taking 10 items for the 'currentPage' -->
    @paginate(allReviews as page by 10, page = currentPage) {
        
        <div class="pagination-stats">
            <p>Showing page {{ page.currentPage }} of {{ page.totalPages }} ({{ page.totalItems }} total reviews)</p>
        </div>

        <div class="review-list">
            @for(review in page.items) {
                @component("reviewCard", data = review)
            }
        </div>
        
        <!-- Smart Pagination Controls -->
        <nav class="pagination-nav">
            @if(page.hasPrevious) {
                <a href="?p={{ page.previousPage }}" class="btn">Previous</a>
            }
            
            @if(page.hasNext) {
                <a href="?p={{ page.nextPage }}" class="btn">Next</a>
            }
        </nav>
        
    }
</div>
```

#### Available Pagination Context Properties:
Inside the `@paginate` block, your alias object provides the following data:
* `items`: The chunked list of items for the current page.
* `currentPage`: The current page number.
* `pageSize`: The configured number of items per page.
* `totalItems`: The total number of items in the original list.
* `totalPages`: The total number of calculated pages.
* `hasPrevious` / `hasNext`: Booleans indicating if navigation to surrounding pages is possible.
* `previousPage` / `nextPage`: The exact page numbers for navigation (or null if unavailable).

## Localization (I18N)

### `@t` / `@translate`

Fetches localized strings from your translation JSON dictionaries (e.g., `locales/en.json`). Siluet automatically flattens nested JSON keys, allowing you to access them using dot notation.

Both `@t` and `@translate` act exactly the same — `@t` is just a convenient, shorter alias.

```html
<!-- Using the short alias (Recommended) -->
<button class="checkout-btn">
    @t("cart.actions.checkout")
</button>

<!-- Using the full directive -->
<p class="disclaimer">
    @translate("footer.legal.terms")
</p>
```
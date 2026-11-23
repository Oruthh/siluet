# Template Directives

Directives in Siluet are special tags prefixed with the `@` symbol. They control the logic, flow, and composition of your templates, allowing you to build dynamic and complex views cleanly.

Directives work in HTML content, inside tags and in comments. Inside attribute values, `<style>` and `<script>` an `@` is plain text, so CSS at-rules like `@media` and addresses like `mailto:hello@shop.com` need no escaping. In content, write `\@` to print a literal `@`. See [Where Template Syntax Is Active](/reference/syntax#where-template-syntax-is-active).

## Control Flow

### `@if` and `@else`

Conditionally render blocks of HTML based on a boolean expression or variable. Conditions can use comparisons and logical operators, and any value can be used as a condition: `null`, `false`, `0`, empty strings and empty collections, maps and arrays are false (see [Truthiness](/reference/syntax#truthiness)).

```html
<div class="stock-status">
    @if(product.inStock) {
        <span class="badge success">In Stock</span>
    } @else {
        <span class="badge danger">Out of Stock</span>
    }
</div>
```

`@if` also works inside a tag, which makes an attribute conditional:

```html
<option value="{{ option.id }}" @if(option.selected) { selected }>{{ option.name }}</option>
```

Blocks can contain inline `<style>` and `<script>` and attributes with braces, such as JSON in `data-*` or JavaScript in `onclick`. Their braces never close the block.

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

**Iteration limit:** each loop may run at most `maxLoopIterations` times (default: `1000`, configurable in `Siluet { }`). The limit is checked while iterating, so even an infinite sequence is stopped. When it is exceeded, the whole render is aborted with a `SiluetViewException` and no partial HTML is returned.

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

**Scope:** a component sees only the arguments passed to it. The root model, loop variables, pagination, the `section` variable and the arguments of an outer component are not visible inside it, so everything a component uses is listed in the call. Pass whatever it needs explicitly:

```html
<!-- sections/hero.html -->
@component("heroTitle", section = section, siteName = siteName)
```

Argument values are evaluated where the component is used, so they can read the caller's variables (`item = product`, `title = product.name`). Variables defined inside the component, such as its loop variables, do not leak back to the caller.

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

**Render depth limit:** views can be nested at most `maxRenderDepth` levels deep (default: `32`, configurable in `Siluet { }`). Every `@component` and every section mounted with `@sections` adds one level; views rendered side by side do not add up. This stops infinite recursion, such as a component that includes itself unconditionally or two components that include each other. When the limit is exceeded, the render is aborted with a `SiluetViewException` that shows the repeating chain of views and where it started, for example:

```text
Maximum render depth (32) exceeded while rendering '/theme/components/ping.html': /theme/components/ping.html → /theme/components/pong.html → /theme/components/ping.html (entered from /theme/layouts/main.html:1:2) at /theme/components/pong.html:1:5
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

## Custom Directives

You can register your own directives in Kotlin, e.g. `@alert("warning", dismissible = true) { ... }`. They are called with the same syntax as the built-in ones and follow the same rules. See [Extending Siluet](/reference/extending#custom-directives).

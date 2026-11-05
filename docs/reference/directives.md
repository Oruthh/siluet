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
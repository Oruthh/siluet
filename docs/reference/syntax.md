# Template Syntax

Siluet views (layouts, sections and components) are plain HTML files with two additions: **print expressions** in double curly braces and **directives** that start with `@`. Everything else is copied to the output as it is, so inline CSS, JavaScript and JSON-LD work without any changes.

```html
<section class="hero">
    <h1>{{ section.properties.title }}</h1>

    @if(products) {
        @for(product in products) {
            @component("productCard", item = product)
        }
    } @else {
        <p>{{ "catalog.empty" | t }}</p>
    }
</section>
```

---

## Print Expressions

<span v-pre>`{{ expression }}`</span> evaluates the expression and prints the result.

```html
<h1>{{ storeName }}</h1>
<p>{{ product.name }} costs {{ product.price }}</p>
```

Every printed value is **HTML-escaped**: `<`, `>`, `&`, `"` and `'` are rendered as text. A value can therefore never inject markup into the page, and it is safe to print inside attributes. To print trusted HTML, use the [`rawHTML`](/reference/filters#rawhtml) filter.

A `null` value prints nothing. To show a fallback instead, use the [`default`](/reference/filters#default) filter.

### Filters

Filters transform a value before it is printed. Apply them with `|` and chain as many as you need. Arguments go in parentheses:

```html
<p>{{ product.description | truncate(80) }}</p>
<p>{{ "store.welcome" | t | uppercase }}</p>
<time datetime="{{ order.createdAt | date("iso") }}">{{ order.createdAt | date("long") }}</time>
```

See [Built-in Filters](/reference/filters) for the full list, and [Extending Siluet](/reference/extending#custom-filters) to add your own.

---

## Expressions

The same expression language is used in <span v-pre>`{{ }}`</span> and in directive arguments such as `@if(...)`.

| Kind | Examples |
| :--- | :--- |
| Variables | `storeName`, `product` |
| Property access | `product.name`, `section.properties.title`, `settings.theme` (map key) |
| Strings | `"Hello"`, `'Hello'` |
| Numbers | `42`, `3000000000`, `9.99` |
| Booleans and null | `true`, `false`, `null` |
| Comparison | `==`, `!=`, `<`, `<=`, `>`, `>=` |
| Logic | `&&`, `\|\|`, `!` |
| Grouping | `(price > 10 && inStock) \|\| featured` |

```html
@if(product.stock > 0 && !product.archived) {
    <span class="badge">In stock</span>
}

@if(user.role == "admin" || (user.role == "editor" && page.draft)) {
    <a href="/edit">Edit</a>
}
```

Whole numbers are `Int` (or `Long` when they do not fit in `Int`) and numbers with a decimal point are `Double`, the same as in Kotlin, so <span v-pre>`{{ 3 }}`</span> prints `3` and <span v-pre>`{{ 3.0 }}`</span> prints `3.0`. Numbers are compared by value, so `1 == 1.0` is `true`. `&&` and `||` stop as soon as the result is known.

How properties are read from your Kotlin objects, and which members templates may never access, is described in [Data Model & Security](/reference/data-model).

### Truthiness

`@if` and the logical operators accept any value, not only booleans:

| Value | Treated as |
| :--- | :--- |
| `null` | false |
| `false` / `true` | false / true |
| Numbers | false when `0`, true otherwise |
| Strings | false when empty, true otherwise |
| Collections and maps | false when empty, true otherwise |
| Any other object | true |

This lets you write `@if(products) { ... }` instead of comparing sizes.

---

## Directives

A directive starts with `@` followed by its name. Most take arguments in parentheses, and block directives wrap content in `{ }`:

```html
@if(user) {
    <p>Welcome back, {{ user.name }}!</p>
} @else {
    <a href="/login">Sign in</a>
}

@component("productCard", item = product, showBadge = true)
```

Directives work anywhere in the HTML content and inside tags, so you can make an attribute conditional:

```html
<option value="{{ option.id }}" @if(option.selected) { selected }>{{ option.name }}</option>
```

See [Directives](/reference/directives) for the built-in ones, and [Extending Siluet](/reference/extending#custom-directives) to add your own.

---

## Where Template Syntax Is Active

Siluet understands the structure of the HTML around your template code. Directives and blocks only work where they make sense, and inline CSS, JavaScript and attribute values stay untouched:

| Where | <span v-pre>`{{ }}`</span> | `@directive`, `{ }` blocks, escapes | Everything else |
| :--- | :---: | :---: | :--- |
| Text between tags | ✓ | ✓ | copied as written |
| Inside a tag (`<div ...>`) | ✓ | ✓ | copied as written |
| HTML comments (`<!-- ... -->`) | ✓ | ✓ | copied as written |
| Attribute values (`"..."`, `'...'`, unquoted) | ✓ | – | copied verbatim |
| `<style>` and `<script>` | ✓ | – | copied verbatim |

In practice this means:

**CSS at-rules work as they are.** `@media`, `@keyframes`, `@font-face`, `@supports` and every other at-rule inside `<style>` are plain CSS, and their braces never interfere with template blocks:

```html
<style>
    @media (max-width: 600px) { .nav { position: sticky; } }
    @keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }
    .hero { color: {{ section.properties.color }}; }
</style>
```

**JavaScript and JSON-LD work as they are.** Braces, `@` keys, regular expressions and backslashes in `<script>` are copied verbatim, while <span v-pre>`{{ }}`</span> is still evaluated:

```html
<script type="application/ld+json">
{
    "@context": "https://schema.org",
    "@type": "Product",
    "name": "{{ product.name }}",
    "offers": { "@type": "Offer", "price": "{{ product.price }}" }
}
</script>
```

**Attribute values are copied verbatim.** E-mail addresses, social handles and JSON in `data-*` attributes need no escaping:

```html
<a href="mailto:hello@shop.com">Contact us</a>
<a href="https://social.example/@siluet">Follow us</a>
<div class="slider" data-config='{"autoplay": true, "delay": 3000}'></div>
<button onclick="if (cart) { cart.open() }">Cart</button>
```

All of the above can be placed inside a directive block. A `}` inside `<style>`, `<script>` or an attribute value never closes the block:

```html
@if(section.properties.slider) {
    <style>.slider { display: grid; }</style>
    <script>if (window.Slider) { new Slider('.slider'); }</script>
} @else {
    <p>Slider is disabled.</p>
}
```

::: info Server-side rendering only
Siluet renders HTML on the server. Templates of client-side frameworks (Vue, Alpine and similar) that use `@` in attribute names or <span v-pre>`{{ }}`</span> in the page are not supported: Siluet treats `@click` inside a tag as a directive and evaluates every <span v-pre>`{{ }}`</span>.
:::

---

## Printing Literal `@`, `{` and `}`

Where directives are active (text between tags, inside tags and in comments), `@` followed by a letter always starts a directive, and a `}` closes the current block. Escape them with a backslash to print them as text:

| Write | Prints |
| :--- | :--- |
| `\@` | `@` |
| `\{` | `{` |
| `\}` | `}` |
| `\\` | `\` |

```html
<p>Write to us at hello\@shop.com or follow \@siluet.</p>
<p>Literal directive: \@if(x)</p>
<p>\{{ price }} prints the braces as text.</p>
```

Renders:

```html
<p>Write to us at hello@shop.com or follow @siluet.</p>
<p>Literal directive: @if(x)</p>
<p>{{ price }} prints the braces as text.</p>
```

A single `{` or `}` outside a block is printed as it is, so text such as `Use code {SPRING}` needs no escaping.

In attribute values, `<style>` and `<script>`, escapes are **not** processed: everything is copied verbatim and `\@` stays `\@`. If you need literal <span v-pre>`{{`</span> or <span v-pre>`}}`</span> there, print them as strings:

```html
<script>
    const template = "{{ "{{" }} name {{ "}}" }}"; // const template = "{{ name }}";
</script>
```

---

## Whitespace

Siluet copies whitespace exactly as written, including the line breaks and indentation around directives. A block such as `@for` therefore leaves the indentation of its lines in the output. HTML ignores this whitespace, so the page looks the same. Output formatting is planned for a future release.

---

## Syntax Errors

Syntax errors are reported when a view is compiled, with the file, line and column. Besides errors in expressions and directives, Siluet reports HTML that is never closed, because it would otherwise swallow the rest of the template:

| Error | Reported at |
| :--- | :--- |
| `Unclosed <style> element: missing </style>` (also `<script>`) | the opening `<style>` tag |
| `Unclosed attribute value: missing closing "` (or `'`) | the opening quote |
| `Unclosed tag: missing '>'` | the `<` of the tag |
| `Unclosed comment: missing '-->'` | the `<!--` |

```text
Parse error: Unclosed <style> element: missing </style> at /theme/sections/hero.html:4:5
```

See [Error Handling](/reference/errors) for the exceptions thrown by `siluet.render(...)`.

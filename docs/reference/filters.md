# Built-in Filters

Filters in Siluet allow you to modify or format variables right before they are rendered in your templates. You apply a filter to a variable using the pipe symbol (`|`). Filters can also accept arguments.

Siluet comes with a set of default filters out of the box to handle common data transformations.

---

## String Formatting

### `lowercase`
Converts all characters in the string to lowercase.
```html
<!-- If status is "ACTIVE" -->
<span class="badge">{{ status | lowercase }}</span>
<!-- Renders: <span class="badge">active</span> -->
```

### `uppercase`
Converts all characters in the string to uppercase.
```html
<!-- If alert is "warning" -->
<strong>{{ alert | uppercase }}</strong>
<!-- Renders: <strong>WARNING</strong> -->
```

### `capitalize`
Capitalizes the first letter of the string, leaving the rest unchanged.
```html
<!-- If name is "jane" -->
<p>User: {{ name | capitalize }}</p>
<!-- Renders: <p>User: Jane</p> -->
```

### `truncate`
Truncates a string to a specified length. This is useful for product descriptions or article excerpts.
```html
<!-- If description is "This is a very long product description that needs shortening." -->
<p>{{ description | truncate(20) }}...</p>
<!-- Renders: <p>This is a very long ...</p> -->
```

---

## Data Handling & Fallbacks

### `default`
Provides a fallback value if the target variable is null, empty, or undefined.
```html
<!-- If userName is null -->
<p>Welcome, {{ userName | default("Guest") }}!</p>
<!-- Renders: <p>Welcome, Guest!</p> -->
```

### `rawHTML`
By default, Siluet safely escapes all variables to prevent XSS (Cross-Site Scripting) attacks. If you trust the content and need to render raw HTML tags, use the `rawHTML` filter.
```html
<!-- If htmlContent is "<b>Bold text</b>" -->
<div>{{ htmlContent | rawHTML }}</div>
<!-- Renders: <div><b>Bold text</b></div> -->
```
> **Warning:** Only use `rawHTML` on trusted content. Never use it on direct, unsanitized user input.

---

## Date Formatting

### `date`
Formats standard Java date/time objects (like `LocalDateTime`, `LocalDate`, `ZonedDateTime`) into readable strings. It is context-aware and respects the `Locale` and `ZoneId` provided during engine rendering.

The `date` filter accepts a format argument. You can use standard Java `DateTimeFormatter` patterns or one of the predefined aliases.

**1. Default Format**
If no argument is provided, the filter defaults to `yyyy-MM-dd HH:mm`.
```html
<!-- Renders: 2026-11-05 17:01 -->
<p>Generated at: {{ generatedAt | date }}</p>
```

**2. Predefined Aliases**
Siluet provides built-in aliases for common use cases:
*   `"short"`: Renders the date using the `dd.MM.yyyy` pattern, bound to the render context's locale.
*   `"iso"`: Renders a standard ISO-8601 string via `DateTimeFormatter.ISO_OFFSET_DATE_TIME`.

```html
<!-- Renders: 05.11.2026 -->
<span>{{ orderDate | date("short") }}</span>

<!-- Renders: 2026-11-05T17:01:00+01:00 -->
<time datetime="{{ orderDate | date("iso") }}"></time>
```

**3. Custom Java Patterns**
You can pass any standard Java date-time pattern directly into the filter.
```html
<!-- Renders: Thursday, November 5, 2026 -->
<p>{{ eventDate | date("EEEE, MMMM d, yyyy") }}</p>
```
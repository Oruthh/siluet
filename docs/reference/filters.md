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
Provides a fallback value when the variable is missing: `null`, undefined, or an empty string (`""`).
```html
<!-- If userName is null or "" -->
<p>Welcome, {{ userName | default("Guest") }}!</p>
<!-- Renders: <p>Welcome, Guest!</p> -->
```

Other values, including `0` and `false`, are treated as present and rendered as they are:
```html
<!-- If stock is 0 -->
<span>In stock: {{ stock | default("unknown") }}</span>
<!-- Renders: <span>In stock: 0</span> -->
```

The fallback value argument is required. Using the filter without it, e.g. `{{ userName | default }}`, fails the render with the error `Filter 'default' requires a fallback value argument.`

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
If no argument is provided, the filter defaults to `yyyy-MM-dd HH:mm`. This format is the same for every locale.
```html
<!-- Renders: 2026-11-05 17:01 -->
<p>Generated at: {{ generatedAt | date }}</p>
```

**2. Predefined Styles**
Siluet provides built-in styles for common use cases. Except for `"iso"`, they follow the conventions of the render context's `Locale`, so the same template shows dates the way readers in each language expect.

| Style | `Locale.US` | Polish (`pl`) |
| :--- | :--- | :--- |
| `"short"` | `11/10/26` | `10.11.2026` |
| `"medium"` | `Nov 10, 2026` | `10 lis 2026` |
| `"long"` | `November 10, 2026` | `10 listopada 2026` |
| `"full"` | `Tuesday, November 10, 2026` | `wtorek, 10 listopada 2026` |
| `"shortDateTime"` | `11/10/26, 2:30 PM` | `10.11.2026, 14:30` |
| `"mediumDateTime"` | `Nov 10, 2026, 2:30:00 PM` | `10 lis 2026, 14:30:00` |
| `"longDateTime"` | `November 10, 2026, 2:30:00 PM CET` | `10 listopada 2026 14:30:00 CET` |
| `"fullDateTime"` | `Tuesday, November 10, 2026, 2:30:00 PM Central European Standard Time` | `wtorek, 10 listopada 2026 14:30:00 czas środkowoeuropejski standardowy` |
| `"iso"` | `2026-11-10T14:30:00+01:00` | `2026-11-10T14:30:00+01:00` |

*Examples for 10 November 2026, 14:30, rendered with `zoneId = ZoneId.of("Europe/Warsaw")`.*

The date styles (`short`, `medium`, `long`, `full`) render the date only. The `…DateTime` styles add the time, converted to the render context's `ZoneId`; `longDateTime` and `fullDateTime` also include the time zone name. All of them use Java's localized formats (`DateTimeFormatter.ofLocalizedDate` / `ofLocalizedDateTime`), so the exact output comes from the JDK's locale data and may differ slightly between Java versions. `"iso"` always renders a standard ISO-8601 string via `DateTimeFormatter.ISO_OFFSET_DATE_TIME`.

```html
<!-- Renders: 11/10/26 (Locale.US) or 10.11.2026 (pl) -->
<span>{{ orderDate | date("short") }}</span>

<!-- Renders: November 10, 2026 (Locale.US) or 10 listopada 2026 (pl) -->
<span>{{ orderDate | date("long") }}</span>

<!-- Renders: Nov 10, 2026, 2:30:00 PM (Locale.US) or 10 lis 2026, 14:30:00 (pl) -->
<span>{{ orderDate | date("mediumDateTime") }}</span>

<!-- Renders: 2026-11-10T14:30:00+01:00 -->
<time datetime="{{ orderDate | date("iso") }}"></time>
```

> **Note:** Before version 0.0.3, `"short"` always used the fixed `dd.MM.yyyy` pattern. To keep that output regardless of locale, use the custom pattern `date("dd.MM.yyyy")`.

**3. Custom Java Patterns**
You can pass any standard Java date-time pattern directly into the filter. Month and day names in the pattern are translated using the render context's locale.
```html
<!-- Renders: Thursday, November 5, 2026 -->
<p>{{ eventDate | date("EEEE, MMMM d, yyyy") }}</p>
```
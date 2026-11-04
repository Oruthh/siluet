package dev.siluet.pagination

/**
 * Encapsulates a paginated collection of [items] alongside navigation metadata.
 *
 * Designed to be passed into template models or contexts, providing templates
 * with easy access to pagination controls (previous/next links, page counts, active page index).
 *
 * ### Template Usage Example:
 * ```html
 * @for(product in pagination.items)
 *   @component("productCard", product)
 * @endfor
 *
 * @if(pagination.hasPrevious)
 *   <a href="?page={{ pagination.previousPage }}">Previous</a>
 * @endif
 *
 * <span>Page {{ pagination.currentPage }} of {{ pagination.totalPages }}</span>
 *
 * @if(pagination.hasNext)
 *   <a href="?page={{ pagination.nextPage }}">Next</a>
 * @endif
 * ```
 *
 * @param T The type of elements contained within this page slice.
 * @property items The subset of items belonging to the current page.
 * @property currentPage The 1-based index of the currently active page.
 * @property pageSize The maximum number of items allocated per page.
 * @property totalItems The total count of items across all pages.
 * @property totalPages The total number of pages calculated from [totalItems] and [pageSize]. Always at least 1.
 * @property hasPrevious `true` if an earlier page exists before [currentPage]; otherwise `false`.
 * @property hasNext `true` if another page exists after [currentPage]; otherwise `false`.
 * @property previousPage The 1-based index of the preceding page, or `null` if [currentPage] is the first page.
 * @property nextPage The 1-based index of the subsequent page, or `null` if [currentPage] is the last page.
 */
data class PaginationContext<T>(
    val items: List<T>,
    val currentPage: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val previousPage: Int?,
    val nextPage: Int?
) {
    companion object {
        /**
         * Computes page boundaries and constructs a validated [PaginationContext].
         *
         * Automatically ensures:
         * - [pageSize] is at least `1`.
         * - Total pages calculation yields at least `1` page even when [totalItems] is `0`.
         * - [currentPage] is coerced within the valid range `[1, totalPages]`.
         *
         * ### Example:
         * ```kotlin
         * val page = PaginationContext.of(
         *     items = pagedProducts,
         *     currentPage = 2,
         *     pageSize = 20,
         *     totalItems = 150
         * )
         * ```
         *
         * @param T The item element type.
         * @param items The items for the current page slice.
         * @param currentPage Requested 1-based page number. Coerced into bounds if out of range.
         * @param pageSize Number of items per page. Coerced to at least 1.
         * @param totalItems Total number of items across all pages. Defaults to `items.size` if not supplied.
         * @return A fully populated and bounds-checked [PaginationContext].
         */
        fun <T> of(
            items: List<T>,
            currentPage: Int,
            pageSize: Int,
            totalItems: Int = items.size
        ): PaginationContext<T> {
            val validPageSize = pageSize.coerceAtLeast(1)
            val pagesCount = if (totalItems == 0) 1 else (totalItems + validPageSize - 1) / validPageSize
            val activePage = currentPage.coerceIn(1, pagesCount)

            return PaginationContext(
                items = items,
                currentPage = activePage,
                pageSize = validPageSize,
                totalItems = totalItems,
                totalPages = pagesCount,
                hasPrevious = activePage > 1,
                hasNext = activePage < pagesCount,
                previousPage = if (activePage > 1) activePage - 1 else null,
                nextPage = if (activePage < pagesCount) activePage + 1 else null
            )
        }
    }
}

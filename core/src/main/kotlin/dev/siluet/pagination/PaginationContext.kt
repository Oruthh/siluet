package dev.siluet.pagination

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

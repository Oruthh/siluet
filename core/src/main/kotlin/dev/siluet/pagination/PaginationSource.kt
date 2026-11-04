package dev.siluet.pagination

/**
 * Data retrieval contract for paginated data sources.
 *
 * Decouples the Siluet pagination runtime from underlying data access layers,
 * allowing templates or controller logic to fetch specific slices of data
 * on-demand from databases, external APIs, or in-memory collections.
 *
 * ### Example Implementation:
 * ```kotlin
 * class ProductRepositoryPaginationSource(
 *     private val repository: ProductRepository
 * ) : PaginationSource<Product> {
 *
 *     override fun fetchPage(page: Int, pageSize: Int): PaginationContext<Product> {
 *         val items = repository.findAll(offset = (page - 1) * pageSize, limit = pageSize)
 *         val total = repository.count()
 *         return PaginationContext.of(items, page, pageSize, total)
 *     }
 * }
 * ```
 *
 * @param T The type of elements fetched by this source.
 * @see PaginationContext
 */
interface PaginationSource<T> {
    /**
     * Fetches a paginated slice of data corresponding to the requested [page] and [pageSize].
     *
     * @param page The 1-based page number to retrieve.
     * @param pageSize The maximum number of records to return for the requested page.
     * @return A [PaginationContext] containing the fetched items along with computed navigation metadata.
     */
    fun fetchPage(page: Int, pageSize: Int): PaginationContext<T>
}
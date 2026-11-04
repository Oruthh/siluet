package dev.siluet.pagination

interface PaginationSource<T> {
    fun fetchPage(page: Int, pageSize: Int): PaginationContext<T>
}
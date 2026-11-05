package dev.siluet.quickstart

import java.time.LocalDateTime

data class StorefrontModel(
    val storeName: String,
    val activeUser: String?,
    val generatedAt: LocalDateTime,
    val featuredProducts: List<Product>
)

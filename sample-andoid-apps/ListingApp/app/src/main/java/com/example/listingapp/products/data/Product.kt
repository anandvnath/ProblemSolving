package com.example.listingapp.products.data

import com.example.listingapp.products.data.local.ProductEntity

data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String,
    val category: String,
    val thumbnail: String,
    val rating: Double
)

fun ProductEntity.toDomain(): Product {
    return Product(
        id = id,
        title = title,
        price = price,
        description = description,
        category = category,
        thumbnail = thumbnail,
        rating = rating
    )
}
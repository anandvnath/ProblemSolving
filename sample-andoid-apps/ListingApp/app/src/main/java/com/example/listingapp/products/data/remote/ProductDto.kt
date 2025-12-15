package com.example.listingapp.products.data.remote

import com.example.listingapp.products.data.local.ProductEntity

data class ProductDto(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val rating: Double,
    val thumbnail: String,
)

fun ProductDto.toEntity(): ProductEntity {
    return ProductEntity(
        id = id,
        title = title,
        description = description,
        price = price,
        rating = rating,
        category = category,
        thumbnail = thumbnail,
    )
}
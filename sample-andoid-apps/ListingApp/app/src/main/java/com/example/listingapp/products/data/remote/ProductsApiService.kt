package com.example.listingapp.products.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ProductsApiService {
    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): ProductsResponse
}

data class ProductsResponse(
    val products: List<ProductDto>,
    val total: Int
)
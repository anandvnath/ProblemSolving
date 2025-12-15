package com.example.listingapp.products.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface IProductsRepo {
    fun getProducts(): Flow<PagingData<Product>>
}
package com.example.listingapp.products.data

import android.content.Context
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.listingapp.products.data.local.AppDatabase
import com.example.listingapp.products.data.remote.ProductRemoteMediator
import com.example.listingapp.products.data.remote.ProductsApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalPagingApi::class)
class ProductsRepo @Inject constructor(
    private val apiService: ProductsApiService,
    private val database: AppDatabase
): IProductsRepo {
    override fun getProducts(): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(pageSize = 10, prefetchDistance = 3),
            remoteMediator = ProductRemoteMediator(apiService = apiService, database = database),
            pagingSourceFactory = { database.productsDao().getProductsPagingSource() }
        ).flow.map {
            it.map { entity -> entity.toDomain() }
        }
    }
}
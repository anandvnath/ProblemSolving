package com.example.listingapp.products.data.remote

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.listingapp.products.data.local.AppDatabase
import com.example.listingapp.products.data.local.ProductEntity

@OptIn(ExperimentalPagingApi::class)
class ProductRemoteMediator(
    private val apiService: ProductsApiService,
    private val database: AppDatabase
) : RemoteMediator<Int, ProductEntity>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, ProductEntity>): MediatorResult {
        return try {
            val skip = when (loadType) {
                LoadType.REFRESH -> 0
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    Log.d("ProductRemoteMediator", "APPEND load with last item id ${state.lastItemOrNull()?.id}, first item id ${state.firstItemOrNull()?.id}")
                    state.lastItemOrNull().let {
                        if (it == null) return MediatorResult.Success(endOfPaginationReached = true)
                        it.id
                    }
                }
            }

            Log.d("ProductRemoteMediator", "Fetching products for skip $skip, limit 10")
            val response = apiService.getProducts(limit = 10, skip = skip)
            val products = response.products

            if (products.isNotEmpty()) {
                Log.d(
                    "ProductRemoteMediator",
                    "Inserting ${response.products.size} products into database, starting ${products.first().id}"
                )
                database.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        database.productsDao().clearAll()
                    }
                    database.productsDao().insertAll(products.map { it.toEntity() })
                }
            }

            Log.d("ProductRemoteMediator", "Load completed for items until id $skip")
            MediatorResult.Success(endOfPaginationReached = response.products.isEmpty())
        } catch (e: Exception) {
            Log.e("ProductRemoteMediator", "Error during load: ${e.message}", e)
            MediatorResult.Error(e)
        }
    }
}
package com.example.listingapp.products.data.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.listingapp.products.data.IProductsRepo
import com.example.listingapp.products.data.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class ProductsViewModel @Inject constructor(private val repo: IProductsRepo) : ViewModel() {
    fun getProducts(): Flow<PagingData<Product>> = repo.getProducts().cachedIn(viewModelScope)
}
package com.example.listingapp.di

import com.example.listingapp.products.data.IProductsRepo
import com.example.listingapp.products.data.ProductsRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds
    abstract fun bindProductsRepo(impl: ProductsRepo): IProductsRepo
}
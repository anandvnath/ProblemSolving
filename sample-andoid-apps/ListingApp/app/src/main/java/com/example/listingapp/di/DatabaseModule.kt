package com.example.listingapp.di

import android.content.Context
import androidx.room.Room
import com.example.listingapp.products.data.local.AppDatabase
import com.example.listingapp.products.data.local.IProductsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "products_database"
        ).build()
    }

    @Provides
    fun provideProductsDao(appDatabase: AppDatabase): IProductsDao {
        return appDatabase.productsDao()
    }
}
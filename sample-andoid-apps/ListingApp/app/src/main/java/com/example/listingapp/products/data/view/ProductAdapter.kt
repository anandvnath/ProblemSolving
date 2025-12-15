package com.example.listingapp.products.data.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.paging.PagingDataAdapter
import coil.ImageLoader
import coil.load
import com.example.listingapp.R
import com.example.listingapp.databinding.ItemProductBinding
import com.example.listingapp.products.data.Product
import javax.inject.Inject

class ProductAdapter @Inject constructor(private val imageLoader: ImageLoader) : PagingDataAdapter<Product, ProductAdapter.ProductViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        Log.d("ProductAdapter", "Creating ViewHolder for viewType: $viewType")
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding, imageLoader)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    class ProductViewHolder(private val binding: ItemProductBinding, private val imageLoader: ImageLoader) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            Log.d("ProductAdapter", "Binding product: $product")
            binding.productName.text = product.title
            binding.productThumbnail.load(data = product.thumbnail, imageLoader = imageLoader) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_background)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem == newItem
            }
        }
    }
}
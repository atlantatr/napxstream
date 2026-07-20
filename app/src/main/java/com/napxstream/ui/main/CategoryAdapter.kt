package com.napxstream.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.napxstream.data.model.Category
import com.napxstream.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(DIFF) {

    private var selectedCategoryId: String? = null

    fun setSelected(categoryId: String?) {
        val previous = selectedCategoryId
        selectedCategoryId = categoryId
        notifyItemChanged(currentList.indexOfFirst { it.categoryId == previous })
        notifyItemChanged(currentList.indexOfFirst { it.categoryId == categoryId })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.categoryText.text = category.categoryName
            binding.categoryText.isSelected = category.categoryId == selectedCategoryId
            binding.root.setOnClickListener { onClick(category) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(oldItem: Category, newItem: Category) =
                oldItem.categoryId == newItem.categoryId

            override fun areContentsTheSame(oldItem: Category, newItem: Category) =
                oldItem == newItem
        }
    }
}

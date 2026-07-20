package com.napxstream.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.napxstream.R
import com.napxstream.databinding.ItemPosterBinding
import com.bumptech.glide.Glide

/** Basitleştirilmiş, hem film hem dizi hem de arama sonuçları için ortak grid modeli */
data class PosterItem(
    val id: Int,
    val title: String,
    val imageUrl: String?,
    val rating: String? = null
)

class PosterAdapter(
    private val onClick: (PosterItem) -> Unit
) : ListAdapter<PosterItem, PosterAdapter.PosterViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PosterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PosterViewHolder(private val binding: ItemPosterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PosterItem) {
            binding.posterTitle.text = item.title
            Glide.with(binding.posterImage.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_app_logo)
                .error(R.drawable.ic_app_logo)
                .into(binding.posterImage)

            if (!item.rating.isNullOrBlank() && item.rating != "0") {
                binding.ratingBadge.visibility = android.view.View.VISIBLE
                binding.ratingBadge.text = item.rating
            } else {
                binding.ratingBadge.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PosterItem>() {
            override fun areItemsTheSame(oldItem: PosterItem, newItem: PosterItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PosterItem, newItem: PosterItem) = oldItem == newItem
        }
    }
}

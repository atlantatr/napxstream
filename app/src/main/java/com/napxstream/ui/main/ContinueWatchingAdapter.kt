package com.napxstream.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.napxstream.R
import com.napxstream.data.local.WatchProgressEntity
import com.napxstream.databinding.ItemContinueWatchingBinding
import com.bumptech.glide.Glide

class ContinueWatchingAdapter(
    private val onClick: (WatchProgressEntity) -> Unit
) : ListAdapter<WatchProgressEntity, ContinueWatchingAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContinueWatchingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemContinueWatchingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchProgressEntity) {
            binding.cwTitle.text = item.title
            val percent = if (item.durationMs > 0) {
                ((item.positionMs.toFloat() / item.durationMs.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else 0
            binding.cwProgress.progress = percent

            Glide.with(binding.cwPoster.context)
                .load(item.posterUrl)
                .placeholder(R.drawable.ic_app_logo)
                .error(R.drawable.ic_app_logo)
                .into(binding.cwPoster)

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WatchProgressEntity>() {
            override fun areItemsTheSame(oldItem: WatchProgressEntity, newItem: WatchProgressEntity) =
                oldItem.contentId == newItem.contentId

            override fun areContentsTheSame(oldItem: WatchProgressEntity, newItem: WatchProgressEntity) =
                oldItem == newItem
        }
    }
}

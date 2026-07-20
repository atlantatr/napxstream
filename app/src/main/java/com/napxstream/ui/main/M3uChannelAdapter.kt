package com.napxstream.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.napxstream.R
import com.napxstream.data.model.M3uEntry
import com.napxstream.databinding.ItemChannelBinding
import com.bumptech.glide.Glide

/** M3U kaynağındaki "canlı" tipli girişler için ChannelAdapter'a benzer liste adapter'ı. */
class M3uChannelAdapter(
    private val onClick: (M3uEntry) -> Unit,
    private val onFavoriteClick: (M3uEntry) -> Unit,
    private val isFavorite: (String) -> Boolean
) : ListAdapter<M3uEntry, M3uChannelAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: M3uEntry) {
            binding.channelName.text = entry.name
            binding.channelNowPlaying.text = entry.groupTitle

            Glide.with(binding.channelIcon.context)
                .load(entry.logoUrl)
                .placeholder(R.drawable.ic_app_logo)
                .error(R.drawable.ic_app_logo)
                .into(binding.channelIcon)

            binding.favoriteIcon.setImageResource(
                if (isFavorite(entry.entryId)) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )

            binding.root.setOnClickListener { onClick(entry) }
            binding.favoriteIcon.setOnClickListener { onFavoriteClick(entry) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<M3uEntry>() {
            override fun areItemsTheSame(oldItem: M3uEntry, newItem: M3uEntry) = oldItem.entryId == newItem.entryId
            override fun areContentsTheSame(oldItem: M3uEntry, newItem: M3uEntry) = oldItem == newItem
        }
    }
}

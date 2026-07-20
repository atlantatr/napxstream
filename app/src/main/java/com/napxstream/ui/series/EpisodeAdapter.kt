package com.napxstream.ui.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.napxstream.data.model.Episode
import com.napxstream.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val onClick: (Episode) -> Unit
) : ListAdapter<Episode, EpisodeAdapter.EpisodeViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EpisodeViewHolder(private val binding: ItemEpisodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: Episode) {
            binding.episodeNumber.text = (episode.episodeNum ?: 0).toString()
            binding.episodeTitle.text = episode.title ?: "Bölüm ${episode.episodeNum}"
            val duration = episode.info?.duration
            binding.episodeDuration.text = if (!duration.isNullOrBlank()) duration else ""
            binding.root.setOnClickListener { onClick(episode) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Episode>() {
            override fun areItemsTheSame(oldItem: Episode, newItem: Episode) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Episode, newItem: Episode) = oldItem == newItem
        }
    }
}

package com.napxstream.ui.live

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.napxstream.R
import com.napxstream.data.model.LiveStream
import com.napxstream.databinding.ItemChannelBinding
import com.bumptech.glide.Glide

class ChannelAdapter(
    private val onClick: (LiveStream) -> Unit,
    private val onFavoriteClick: (LiveStream) -> Unit,
    private val isFavorite: (Int) -> Boolean,
    private val onLongClick: (LiveStream) -> Unit = {}
) : ListAdapter<LiveStream, ChannelAdapter.ChannelViewHolder>(DIFF) {

    /** streamId -> "Şimdi: ..." metni; EPG yüklendikçe dışarıdan güncellenir */
    private val nowPlayingMap = mutableMapOf<Int, String>()

    fun updateNowPlaying(streamId: Int, text: String) {
        nowPlayingMap[streamId] = text
        val index = currentList.indexOfFirst { it.streamId == streamId }
        if (index != -1) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(private val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: LiveStream) {
            binding.channelName.text = channel.name ?: "-"
            binding.channelNowPlaying.text = nowPlayingMap[channel.streamId] ?: ""

            Glide.with(binding.channelIcon.context)
                .load(channel.streamIcon)
                .placeholder(R.drawable.ic_app_logo)
                .error(R.drawable.ic_app_logo)
                .into(binding.channelIcon)

            binding.favoriteIcon.setImageResource(
                if (isFavorite(channel.streamId)) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )

            binding.root.setOnClickListener { onClick(channel) }
            binding.root.setOnLongClickListener { onLongClick(channel); true }
            binding.favoriteIcon.setOnClickListener { onFavoriteClick(channel) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LiveStream>() {
            override fun areItemsTheSame(oldItem: LiveStream, newItem: LiveStream) =
                oldItem.streamId == newItem.streamId

            override fun areContentsTheSame(oldItem: LiveStream, newItem: LiveStream) =
                oldItem == newItem
        }
    }
}

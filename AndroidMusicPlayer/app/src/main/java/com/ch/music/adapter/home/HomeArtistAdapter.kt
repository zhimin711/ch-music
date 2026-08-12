package com.ch.music.adapter.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ch.music.databinding.ItemHomeArtistBinding
import com.ch.music.network.models.HotArtist
import com.bumptech.glide.Glide

/**
 * 首页热门歌手横向滚动适配器
 */
class HomeArtistAdapter(
    private var artists: List<HotArtist>,
    private val onClick: ((HotArtist) -> Unit)? = null
) : RecyclerView.Adapter<HomeArtistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeArtistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(artists[position])
    }

    override fun getItemCount(): Int = artists.size

    fun updateData(newArtists: List<HotArtist>) {
        artists = newArtists
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemHomeArtistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: HotArtist) {
            binding.artistName.text = artist.name

            val imageUrl = artist.picUrl.takeIf { !it.isNullOrEmpty() }
                ?: artist.img1v1Url.takeIf { !it.isNullOrEmpty() }

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .circleCrop()
                    .into(binding.artistAvatar)
            }

            itemView.setOnClickListener { onClick?.invoke(artist) }
        }
    }
}

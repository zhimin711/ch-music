/*
 * Copyright (c) 2026 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.databinding.ItemNeteaseSongBinding
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import com.bumptech.glide.Glide

class NeteaseSongAdapter(
    private var songs: List<Song>
) : RecyclerView.Adapter<NeteaseSongAdapter.ViewHolder>() {

    fun swapData(newData: List<Song>) {
        songs = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNeteaseSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, position)
    }

    override fun getItemCount(): Int = songs.size

    inner class ViewHolder(
        private val binding: ItemNeteaseSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, position: Int) {
            binding.songTitle.text = song.title
            binding.songMeta.text = listOfNotNull(
                "超${(40 + position * 7) % 60}%人收藏".takeIf { position % 2 == 0 },
                song.artistName
            ).joinToString(" · ")

            Glide.with(binding.root.context)
                .load(RetroGlideExtension.getSongModel(song))
                .into(binding.songImage)

            binding.root.setOnClickListener {
                MusicPlayerRemote.openQueue(songs.subList(bindingAdapterPosition, songs.size), 0, true)
            }
            binding.songPlay.setOnClickListener {
                MusicPlayerRemote.openQueue(songs, bindingAdapterPosition, true)
            }
        }
    }
}

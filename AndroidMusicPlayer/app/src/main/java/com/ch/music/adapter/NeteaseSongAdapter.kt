/*
 * Copyright (c) 2026 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 */
package com.ch.music.adapter

import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.ch.music.R
import com.ch.music.extensions.resolveColor
import com.ch.music.extensions.textColorPrimary
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.ch.music.databinding.ItemNeteaseSongBinding
import com.ch.music.glide.RetroGlideExtension
import com.ch.music.helper.MusicPlayerRemote
import com.ch.music.model.Song
import com.ch.music.netease.NeteasePlaybackManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class NeteaseSongAdapter(
    private var songs: List<Song>,
    private val playbackManager: NeteasePlaybackManager? = null,
    private val lifecycleScope: LifecycleCoroutineScope? = null,
    private val onResolveError: ((String?) -> Unit)? = null,
    private val onLongClick: ((Song) -> Unit)? = null
) : RecyclerView.Adapter<NeteaseSongAdapter.ViewHolder>() {

    private var playingSongId = MusicPlayerRemote.currentSong.id

    fun refreshPlayback() {
        val previousId = playingSongId
        playingSongId = MusicPlayerRemote.currentSong.id
        songs.forEachIndexed { index, song ->
            if (song.id == previousId || song.id == playingSongId) notifyItemChanged(index)
        }
    }

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

    /**
     * 播放前先解析 URL：Song.data 为空时 Song.uri 会回退到 MediaStore URI（负 ID），
     * ExoPlayer 会抛 UnsupportedOperationException。
     *
     * 只解析被点击的那首歌 —— 后端 song/url 传多个 id 时不稳定，且列表其他歌
     * 未点击时不必占用带宽。
     */
    private fun playFrom(startIndex: Int) {
        val manager = playbackManager
        val scope = lifecycleScope
        val clicked = songs.getOrNull(startIndex) ?: return
        if (manager == null || scope == null) {
            // 未注入解析器时，退回原来的直接播放（可能失败）
            MusicPlayerRemote.openQueue(listOf(clicked), 0, true)
            return
        }
        scope.launch {
            val resolved = manager.resolveSong(clicked)
            if (resolved == null || !(resolved.data.startsWith("http://") || resolved.data.startsWith("https://"))) {
                onResolveError?.invoke("无可用播放链接")
                return@launch
            }
            // 同步 UI：把当前项替换成解析过的版本
            val currentIndex = songs.indexOfFirst { it.id == clicked.id }
            if (currentIndex >= 0) {
                songs = songs.toMutableList().apply { set(currentIndex, resolved) }
                notifyItemChanged(currentIndex)
            }
            MusicPlayerRemote.openQueue(arrayListOf(resolved), 0, true)
        }
    }

    inner class ViewHolder(
        private val binding: ItemNeteaseSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, position: Int) {
            binding.songTitle.text = song.title
            binding.songTitle.setTextColor(
                if (song.id == playingSongId) binding.root.context.resolveColor(com.google.android.material.R.attr.colorPrimary)
                else binding.root.context.textColorPrimary()
            )
            binding.songPlay.setImageResource(
                if (song.id == playingSongId) R.drawable.ic_volume_up else R.drawable.ic_play_arrow_outline
            )
            binding.songMore.isVisible = onLongClick != null
            binding.songMore.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onLongClick?.invoke(songs[pos])
            }
            binding.songMeta.text = listOf(song.artistName, song.albumName).joinToString(" · ")

            Glide.with(binding.root.context)
                .load(RetroGlideExtension.getSongModel(song))
                .placeholder(R.drawable.default_album_art)
                .error(R.drawable.default_album_art)
                .into(binding.songImage)

            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) playFrom(pos)
            }
            binding.root.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onLongClick?.invoke(songs[pos])
                    true
                } else false
            }
            binding.songPlay.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) playFrom(pos)
            }
        }
    }
}

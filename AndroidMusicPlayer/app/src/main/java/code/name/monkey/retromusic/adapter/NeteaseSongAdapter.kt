/*
 * Copyright (c) 2026 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.databinding.ItemNeteaseSongBinding
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.netease.NeteasePlaybackManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class NeteaseSongAdapter(
    private var songs: List<Song>,
    private val playbackManager: NeteasePlaybackManager? = null,
    private val lifecycleScope: LifecycleCoroutineScope? = null,
    private val onResolveError: ((String?) -> Unit)? = null
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
            songs = songs.toMutableList().apply { set(startIndex, resolved) }
            notifyItemChanged(startIndex)
            MusicPlayerRemote.openQueue(arrayListOf(resolved), 0, true)
        }
    }

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
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) playFrom(pos)
            }
            binding.songPlay.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) playFrom(pos)
            }
        }
    }
}

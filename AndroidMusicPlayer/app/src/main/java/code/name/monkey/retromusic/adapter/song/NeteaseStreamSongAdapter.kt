/*
 * Copyright (c) 2026 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.adapter.song

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.netease.NeteasePlaybackManager
import kotlinx.coroutines.launch

/**
 * 网易云在线播放歌曲适配器
 *
 * 重写单条点击事件：先批量预取队列内所有歌曲 URL，再启动播放。
 */
class NeteaseStreamSongAdapter(
    activity: FragmentActivity,
    dataSet: MutableList<Song>,
    itemLayoutRes: Int,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val playbackManager: NeteasePlaybackManager,
    private val onResolveError: ((String?) -> Unit)? = null
) : SongAdapter(activity, dataSet, itemLayoutRes, showSectionName = false) {

    override fun createViewHolder(view: View): ViewHolder {
        return StreamViewHolder(view)
    }

    private inner class StreamViewHolder(itemView: View) : ViewHolder(itemView) {
        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
                return
            }
            val position = layoutPosition
            if (position == -1) return

            lifecycleScope.launch {
                val resolved = playbackManager.resolveSongs(dataSet)
                val playable = playbackManager.playableOnly(resolved)
                if (playable.isEmpty()) {
                    onResolveError?.invoke("无可用播放链接")
                    return@launch
                }
                // 在 playable 列表中找到被点击的歌曲
                val clickedSong = dataSet.getOrNull(position) ?: return@launch
                val newIndex = playable.indexOfFirst { it.id == clickedSong.id }.coerceAtLeast(0)

                // 把解析过的列表写回 adapter，保持 UI 与队列一致
                dataSet.clear()
                dataSet.addAll(resolved)
                notifyDataSetChanged()

                MusicPlayerRemote.openQueue(ArrayList(playable), newIndex, true)
            }
        }
    }
}

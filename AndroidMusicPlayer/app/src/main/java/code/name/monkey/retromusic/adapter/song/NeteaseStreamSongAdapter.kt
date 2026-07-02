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
            val clicked = dataSet.getOrNull(position) ?: return

            lifecycleScope.launch {
                // 只解析被点击的那一首，避免多 id 请求上游不稳
                val resolved = playbackManager.resolveSong(clicked)
                if (resolved == null || !(resolved.data.startsWith("http://") || resolved.data.startsWith("https://"))) {
                    onResolveError?.invoke("无可用播放链接")
                    return@launch
                }
                // 把解析过的这条写回 dataSet，保持 UI 与队列一致
                dataSet[position] = resolved
                notifyItemChanged(position)

                MusicPlayerRemote.openQueue(arrayListOf(resolved), 0, true)
            }
        }
    }
}

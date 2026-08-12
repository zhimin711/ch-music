package com.ch.music.netease

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ch.music.R
import com.ch.music.model.Song
import com.ch.music.musicserver.MusicServerRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

/**
 * 将网易云歌曲添加到当前登录用户的服务端歌单。
 *
 * 从 Koin 拿 [MusicServerRepository]，读取当前 state 中的歌单列表。
 * 未登录或无歌单会弹提示。
 */
object NeteaseAddToPlaylistDialog {

    fun show(fragment: Fragment, song: Song) {
        val ctx = fragment.context ?: return
        val neteaseId = NeteaseSongMapper.neteaseIdFromSong(song)
        if (neteaseId == null) {
            android.widget.Toast.makeText(ctx, R.string.error_not_netease_song, android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val repo: MusicServerRepository = fragment.get()
        val state = repo.state.value
        if (state.user == null) {
            android.widget.Toast.makeText(ctx, R.string.music_server_login_required, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val playlists = state.playlists
        if (playlists.isEmpty()) {
            android.widget.Toast.makeText(ctx, R.string.music_server_no_playlists, android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.action_add_to_playlist)
            .setItems(playlists.map { it.name }.toTypedArray()) { _, which ->
                val playlist = playlists[which]
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        repo.addNeteaseTrackToPlaylist(
                            playlistId = playlist.id,
                            neteaseId = neteaseId,
                            title = song.title,
                            artist = song.artistName,
                            album = song.albumName,
                            picUrl = NeteaseCoverCache.get(neteaseId),
                            duration = song.duration.takeIf { it > 0 }
                        )
                        if (fragment.isAdded) {
                            android.widget.Toast.makeText(
                                fragment.requireContext(),
                                fragment.getString(R.string.added_to_playlist, playlist.name),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (t: Throwable) {
                        if (fragment.isAdded) {
                            android.widget.Toast.makeText(
                                fragment.requireContext(),
                                fragment.getString(R.string.add_to_playlist_failed, t.message ?: "unknown"),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}

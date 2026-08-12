package com.ch.music.fragments.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ch.music.R
import com.ch.music.adapter.NeteaseSongAdapter
import com.ch.music.adapter.home.HomeArtistAdapter
import com.ch.music.adapter.home.HomePlaylistCardAdapter
import com.ch.music.databinding.FragmentHomeRecommendBinding
import com.ch.music.netease.NeteasePlaybackManager
import com.ch.music.network.Result
import com.ch.music.network.models.PersonalizedPlaylist
import com.ch.music.viewmodel.HomeViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 首页推荐 Tab
 * 包含 Hero 区域、快捷入口、推荐歌单、热门歌手、新歌推荐、新碟上架
 */
class HomeRecommendFragment : Fragment() {

    private var _binding: FragmentHomeRecommendBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModel()
    private val neteasePlayback: NeteasePlaybackManager by inject()

    private var playlistAdapter: HomePlaylistCardAdapter? = null
    private var artistAdapter: HomeArtistAdapter? = null
    private var songAdapter: NeteaseSongAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()

        // 各分区默认进入 loading 态，等观察到数据/错误再切换
        setSectionState(
            recycler = binding.recommendedPlaylists,
            progress = binding.playlistsProgress,
            empty = binding.playlistsEmpty,
            state = SectionState.LOADING
        )
        setSectionState(
            recycler = binding.hotArtists,
            progress = binding.artistsProgress,
            empty = binding.artistsEmpty,
            state = SectionState.LOADING
        )
        setSectionState(
            recycler = binding.newSongs,
            progress = binding.newSongsProgress,
            empty = binding.newSongsEmpty,
            state = SectionState.LOADING
        )

        // 触发数据加载（ViewModel init 已自动加载，此处确保刷新）
        homeViewModel.loadHomeData()
    }

    private fun setupRecyclerViews() {
        // 推荐歌单 - 3列网格
        playlistAdapter = HomePlaylistCardAdapter(emptyList()) { playlistId, playlistName ->
            // 跳转到歌单详情页
            com.ch.music.fragments.songlist.SonglistDetailFragment
                .navigateTo(requireActivity(), playlistId, playlistName)
        }
        binding.recommendedPlaylists.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = playlistAdapter
            isNestedScrollingEnabled = false
        }

        // 热门歌手 - 横向滚动
        artistAdapter = HomeArtistAdapter(emptyList()) { artist ->
            com.ch.music.fragments.artistdetail.ArtistDetailFragment
                .navigateTo(requireActivity(), artist.id, artist.name)
        }
        binding.hotArtists.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = artistAdapter
        }

        // 新歌推荐 - 列表
        songAdapter = NeteaseSongAdapter(
            songs = emptyList(),
            playbackManager = neteasePlayback,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onResolveError = { msg ->
                android.widget.Toast.makeText(
                    requireContext(),
                    "播放失败：${msg ?: "未知错误"}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onLongClick = { song ->
                com.ch.music.netease.NeteaseAddToPlaylistDialog.show(this, song)
            }
        )
        binding.newSongs.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        // 每日推荐 / 快捷入口已移除，占位以便后续扩展
    }

    private fun observeViewModel() {
        // 推荐歌单
        homeViewModel.personalizedPlaylists.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    showPlaylists(result.data)
                }
                is Result.Error -> {
                    android.util.Log.e("HomeRecommend", "加载推荐歌单失败", result.error)
                    setSectionState(
                        binding.recommendedPlaylists,
                        binding.playlistsProgress,
                        binding.playlistsEmpty,
                        SectionState.ERROR,
                        message = friendlyErrorMessage(result.error)
                    )
                }
                is Result.Loading -> {
                    setSectionState(
                        binding.recommendedPlaylists,
                        binding.playlistsProgress,
                        binding.playlistsEmpty,
                        SectionState.LOADING
                    )
                }
            }
        }

        // 热门歌手
        homeViewModel.hotArtists.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val artists = result.data.take(10)
                    artistAdapter?.updateData(artists)
                    setSectionState(
                        binding.hotArtists,
                        binding.artistsProgress,
                        binding.artistsEmpty,
                        if (artists.isEmpty()) SectionState.EMPTY else SectionState.CONTENT,
                        message = "暂无热门歌手"
                    )
                }
                is Result.Error -> {
                    android.util.Log.e("HomeRecommend", "加载热门歌手失败", result.error)
                    setSectionState(
                        binding.hotArtists,
                        binding.artistsProgress,
                        binding.artistsEmpty,
                        SectionState.ERROR,
                        message = friendlyErrorMessage(result.error)
                    )
                }
                is Result.Loading -> {
                    setSectionState(
                        binding.hotArtists,
                        binding.artistsProgress,
                        binding.artistsEmpty,
                        SectionState.LOADING
                    )
                }
            }
        }

        // 新歌推荐
        homeViewModel.newSongs.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    showNewSongs(result.data)
                }
                is Result.Error -> {
                    android.util.Log.e("HomeRecommend", "加载新歌推荐失败", result.error)
                    setSectionState(
                        binding.newSongs,
                        binding.newSongsProgress,
                        binding.newSongsEmpty,
                        SectionState.ERROR,
                        message = friendlyErrorMessage(result.error)
                    )
                }
                is Result.Loading -> {
                    setSectionState(
                        binding.newSongs,
                        binding.newSongsProgress,
                        binding.newSongsEmpty,
                        SectionState.LOADING
                    )
                }
            }
        }
    }

    private fun showNewSongs(newSongs: List<com.ch.music.network.models.NewSong>) {
        // NewSong.song 是网易云原始歌曲结构，若为空则用 NewSong 顶层字段兜底
        val songs = newSongs.mapIndexedNotNull { index, item ->
            val ncSong = item.song
            if (ncSong != null) {
                com.ch.music.netease.NeteaseSongMapper.toSong(
                    ncSong,
                    trackIndex = index,
                    coverUrl = item.picUrl
                )
            } else {
                // 兜底：直接从 NewSong 顶层字段构造
                val fallback = com.ch.music.network.models.NeteaseSong(
                    id = item.id,
                    name = item.name,
                    ar = item.artists,
                    al = null,
                    dt = null
                )
                com.ch.music.netease.NeteaseSongMapper.toSong(
                    fallback,
                    trackIndex = index,
                    coverUrl = item.picUrl
                )
            }
        }
        songAdapter?.swapData(songs)
        setSectionState(
            binding.newSongs,
            binding.newSongsProgress,
            binding.newSongsEmpty,
            if (songs.isEmpty()) SectionState.EMPTY else SectionState.CONTENT,
            message = "暂无新歌"
        )
    }

    private fun showPlaylists(playlists: List<PersonalizedPlaylist>) {
        // 只显示前9个歌单（3列x3行）
        val subset = playlists.take(9)
        playlistAdapter?.updateData(subset)
        setSectionState(
            binding.recommendedPlaylists,
            binding.playlistsProgress,
            binding.playlistsEmpty,
            if (subset.isEmpty()) SectionState.EMPTY else SectionState.CONTENT,
            message = "暂无推荐歌单"
        )
    }

    private enum class SectionState { LOADING, CONTENT, EMPTY, ERROR }

    /**
     * 把网络/接口异常翻译成用户能看懂的短句。避免把服务端 URL、堆栈信息直接暴露给用户。
     */
    private fun friendlyErrorMessage(error: Throwable?): String {
        if (error == null) return "加载失败，请稍后重试"
        return when (error) {
            is java.net.UnknownHostException,
            is java.net.ConnectException -> "无法连接网络，请检查网络后重试"
            is java.net.SocketTimeoutException -> "连接超时，请稍后重试"
            is javax.net.ssl.SSLException -> "网络安全连接异常，请稍后重试"
            is retrofit2.HttpException -> when (error.code()) {
                401, 403 -> "请先登录后再试"
                404 -> "内容不存在或已下架"
                in 500..599 -> "服务暂时不可用，请稍后重试"
                else -> "加载失败，请稍后重试"
            }
            is java.io.IOException -> "网络异常，请稍后重试"
            else -> "加载失败，请稍后重试"
        }
    }

    private fun setSectionState(
        recycler: androidx.recyclerview.widget.RecyclerView,
        progress: android.widget.ProgressBar,
        empty: com.google.android.material.textview.MaterialTextView,
        state: SectionState,
        message: String? = null
    ) {
        when (state) {
            SectionState.LOADING -> {
                progress.visibility = View.VISIBLE
                recycler.visibility = View.GONE
                empty.visibility = View.GONE
            }
            SectionState.CONTENT -> {
                progress.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                empty.visibility = View.GONE
            }
            SectionState.EMPTY -> {
                progress.visibility = View.GONE
                recycler.visibility = View.GONE
                empty.visibility = View.VISIBLE
                empty.text = message ?: "暂无内容"
            }
            SectionState.ERROR -> {
                progress.visibility = View.GONE
                recycler.visibility = View.GONE
                empty.visibility = View.VISIBLE
                empty.text = message ?: "加载失败"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recommendedPlaylists.adapter = null
        binding.hotArtists.adapter = null
        binding.newSongs.adapter = null
        playlistAdapter = null
        artistAdapter = null
        songAdapter = null
        _binding = null
    }

    companion object {
        fun newInstance(): HomeRecommendFragment = HomeRecommendFragment()
    }
}

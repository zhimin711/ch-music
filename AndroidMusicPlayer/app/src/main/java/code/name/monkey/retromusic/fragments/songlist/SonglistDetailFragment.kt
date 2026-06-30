package code.name.monkey.retromusic.fragments.songlist

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.song.NeteaseStreamSongAdapter
import code.name.monkey.retromusic.adapter.song.SongAdapter
import code.name.monkey.retromusic.databinding.FragmentSonglistDetailBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.elevatedAccentColor
import code.name.monkey.retromusic.extensions.surfaceColor
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.netease.NeteasePlaybackManager
import code.name.monkey.retromusic.netease.NeteaseSongMapper
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.network.models.PlaylistDetail
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.viewmodel.HomeViewModel
import com.bumptech.glide.Glide
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 网易云歌单详情页
 * 展示歌单封面、名称、描述和歌曲列表
 */
class SonglistDetailFragment : Fragment(R.layout.fragment_songlist_detail) {

    private val homeViewModel: HomeViewModel by viewModel()
    private val neteasePlayback: NeteasePlaybackManager by inject()

    private var _binding: FragmentSonglistDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: SongAdapter
    private var playlistDetail: PlaylistDetail? = null

    private val playlistId: Long by lazy {
        arguments?.getLong(ARG_PLAYLIST_ID) ?: 0L
    }

    private val playlistName: String? by lazy {
        arguments?.getString(ARG_PLAYLIST_NAME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.fragment_container
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(surfaceColor())
            setPathMotion(MaterialArcMotion())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSonglistDetailBinding.bind(view)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).addTarget(view)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = null

        setUpRecyclerView()
        setupButtons()
        observeViewModel()

        // 加载歌单详情
        homeViewModel.loadPlaylistDetail(playlistId)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())
    }

    private fun setUpRecyclerView() {
        songAdapter = NeteaseStreamSongAdapter(
            activity = requireActivity(),
            dataSet = mutableListOf(),
            itemLayoutRes = R.layout.item_list,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            playbackManager = neteasePlayback,
            onResolveError = { msg ->
                binding.errorInfo.visibility = View.VISIBLE
                binding.errorMessage.text = getString(R.string.failed_to_load_playlist, msg ?: "未知错误")
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupButtons() {
        binding.playButton.apply {
            setOnClickListener {
                if (songAdapter.dataSet.isNotEmpty()) {
                    playSongs(songAdapter.dataSet, shuffle = false)
                }
            }
            accentColor()
        }

        binding.shuffleButton.apply {
            setOnClickListener {
                if (songAdapter.dataSet.isNotEmpty()) {
                    playSongs(songAdapter.dataSet, shuffle = true)
                }
            }
            elevatedAccentColor()
        }
    }

    /**
     * 播放歌单歌曲：批量预取 URL 后启动播放
     */
    private fun playSongs(songs: List<Song>, shuffle: Boolean) {
        binding.progressIndicator.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val resolved = neteasePlayback.resolveSongs(songs)
            val playable = neteasePlayback.playableOnly(resolved)
            binding.progressIndicator.visibility = View.GONE
            if (playable.isEmpty()) {
                binding.errorInfo.visibility = View.VISIBLE
                binding.errorMessage.text = getString(R.string.failed_to_load_playlist, "无可用播放链接")
                return@launch
            }
            // 同步更新 adapter，避免显示与播放的歌曲不一致
            songAdapter.swapDataSet(resolved)
            if (shuffle) {
                MusicPlayerRemote.openAndShuffleQueue(ArrayList(playable), true)
            } else {
                MusicPlayerRemote.openQueue(ArrayList(playable), 0, true)
            }
        }
    }

    private fun observeViewModel() {
        homeViewModel.playlistDetail.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    showPlaylistDetail(result.data)
                    showContent()
                }
                is Result.Error -> {
                    showError(result.error)
                }
                is Result.Loading -> {
                    showLoading()
                }
            }
        }
    }

    private fun showPlaylistDetail(detail: PlaylistDetail) {
        playlistDetail = detail

        // 加载封面
        Glide.with(this)
            .load(detail.coverImgUrl)
            .placeholder(R.drawable.default_album_art)
            .into(binding.image)

        // 设置标题
        binding.title.text = detail.name
        binding.collapsingAppBarLayout.title = detail.name

        // 设置描述信息
        val songCount = detail.trackCount ?: 0
        val playCount = detail.playCount ?: 0
        val playCountText = formatPlayCount(playCount)
        binding.subtitle.text = getString(R.string.playlist_subtitle, songCount, playCountText)

        // 转换歌曲列表并显示
        val songs = convertNeteaseSongsToLocalSongs(detail.tracks ?: emptyList())
        songAdapter.swapDataSet(songs)
    }

    /**
     * 将网易云歌曲转换为本地 Song 对象
     * data 字段（播放 URL）此时为空，点击播放时再批量预取
     */
    private fun convertNeteaseSongsToLocalSongs(
        neteaseSongs: List<code.name.monkey.retromusic.network.models.NeteaseSong>
    ): List<Song> {
        return neteaseSongs.mapIndexed { index, neteaseSong ->
            NeteaseSongMapper.toSong(neteaseSong, trackIndex = index)
        }
    }

    private fun showContent() {
        binding.progressIndicator.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.errorInfo.visibility = View.GONE
    }

    private fun showLoading() {
        binding.progressIndicator.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.errorInfo.visibility = View.GONE
    }

    private fun showError(exception: Exception) {
        binding.progressIndicator.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.errorInfo.visibility = View.VISIBLE
        binding.errorMessage.text = getString(R.string.failed_to_load_playlist, exception.message)
    }

    /**
     * 格式化播放次数
     */
    private fun formatPlayCount(count: Long): String {
        return when {
            count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
            count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
            else -> count.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"
        private const val ARG_PLAYLIST_NAME = "playlist_name"

        fun newInstance(playlistId: Long, playlistName: String): SonglistDetailFragment {
            return SonglistDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PLAYLIST_ID, playlistId)
                    putString(ARG_PLAYLIST_NAME, playlistName)
                }
            }
        }
    }
}

package com.ch.music.fragments.artistdetail

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ch.music.R
import com.ch.music.adapter.song.NeteaseStreamSongAdapter
import com.ch.music.adapter.song.SongAdapter
import com.ch.music.databinding.FragmentArtistDetailBinding
import com.ch.music.extensions.accentColor
import com.ch.music.extensions.elevatedAccentColor
import com.ch.music.extensions.surfaceColor
import com.ch.music.helper.MusicPlayerRemote
import com.ch.music.model.Song
import com.ch.music.netease.NeteasePlaybackManager
import com.ch.music.netease.NeteaseSongMapper
import com.ch.music.network.Result
import com.ch.music.network.models.ArtistDetail
import com.ch.music.network.models.NeteaseSong
import com.ch.music.viewmodel.HomeViewModel
import com.bumptech.glide.Glide
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 网易云歌手详情页。
 * 参考 MusicPlayer/src/renderer/views/artist/detail.vue：hero + 热门歌曲。
 */
class ArtistDetailFragment : Fragment(R.layout.fragment_artist_detail) {

    private val homeViewModel: HomeViewModel by viewModel()
    private val neteasePlayback: NeteasePlaybackManager by inject()

    private var _binding: FragmentArtistDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: SongAdapter
    private var artistDetail: ArtistDetail? = null

    private val artistId: Long by lazy { arguments?.getLong(ARG_ARTIST_ID) ?: 0L }
    private val artistName: String? by lazy { arguments?.getString(ARG_ARTIST_NAME) }

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
        _binding = FragmentArtistDetailBinding.bind(view)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).addTarget(view)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
        binding.toolbar.title = null

        // 先显示传入的名字，避免空白 hero
        binding.title.text = artistName.orEmpty()
        binding.collapsingAppBarLayout.title = artistName.orEmpty()

        setUpRecyclerView()
        setupButtons()
        observeViewModel()

        homeViewModel.loadArtistDetail(artistId)

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
            },
            onLongClick = { song ->
                com.ch.music.netease.NeteaseAddToPlaylistDialog.show(this, song)
                true
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
            songAdapter.swapDataSet(resolved)
            if (shuffle) {
                MusicPlayerRemote.openAndShuffleQueue(ArrayList(playable), true)
            } else {
                MusicPlayerRemote.openQueue(ArrayList(playable), 0, true)
            }
        }
    }

    private fun observeViewModel() {
        homeViewModel.artistDetail.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    artistDetail = result.data
                    showHero(result.data)
                }
                is Result.Error -> {
                    // 保留传入的名字作为兜底，不完全遮蔽热门歌曲
                    android.util.Log.e("ArtistDetail", "加载歌手详情失败", result.error)
                }
                is Result.Loading -> Unit
            }
        }

        homeViewModel.artistTopSongs.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val songs = convertNeteaseSongs(result.data)
                    songAdapter.swapDataSet(songs)
                    showContent()
                    updateSubtitle(songs.size)
                }
                is Result.Error -> showError(result.error)
                is Result.Loading -> showLoading()
            }
        }
    }

    private fun showHero(detail: ArtistDetail) {
        Glide.with(this)
            .load(detail.picUrl ?: detail.img1v1Url)
            .placeholder(R.drawable.default_album_art)
            .circleCrop()
            .into(binding.image)
        binding.title.text = detail.name
        binding.collapsingAppBarLayout.title = detail.name
        updateSubtitle(null)
    }

    private fun updateSubtitle(songCount: Int?) {
        val d = artistDetail
        val hot = songCount ?: 0
        val albumCount = d?.albumSize ?: 0
        binding.subtitle.text = when {
            d == null && songCount == null -> ""
            albumCount > 0 && hot > 0 -> "${hot} 首热门 · ${albumCount} 张专辑"
            hot > 0 -> "${hot} 首热门"
            albumCount > 0 -> "${albumCount} 张专辑"
            else -> ""
        }
    }

    private fun convertNeteaseSongs(neteaseSongs: List<NeteaseSong>): List<Song> {
        return neteaseSongs.mapIndexed { index, ns ->
            NeteaseSongMapper.toSong(ns, trackIndex = index)
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    companion object {
        private const val ARG_ARTIST_ID = "artist_id"
        private const val ARG_ARTIST_NAME = "artist_name"

        /**
         * 通过 NavController 导航到歌手详情页。保留 R.id.fragment_container 处的 NavHostFragment。
         */
        fun navigateTo(
            activity: androidx.fragment.app.FragmentActivity,
            artistId: Long,
            artistName: String
        ) {
            val args = Bundle().apply {
                putLong(ARG_ARTIST_ID, artistId)
                putString(ARG_ARTIST_NAME, artistName)
            }
            androidx.navigation.Navigation
                .findNavController(activity, R.id.fragment_container)
                .navigate(R.id.artist_detail_fragment, args)
        }
    }
}

package code.name.monkey.retromusic.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.NeteaseSongAdapter
import code.name.monkey.retromusic.adapter.home.HomeArtistAdapter
import code.name.monkey.retromusic.adapter.home.HomePlaylistCardAdapter
import code.name.monkey.retromusic.databinding.FragmentHomeRecommendBinding
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.network.models.PersonalizedPlaylist
import code.name.monkey.retromusic.viewmodel.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 首页推荐 Tab
 * 包含 Hero 区域、快捷入口、推荐歌单、热门歌手、新歌推荐、新碟上架
 */
class HomeRecommendFragment : Fragment() {

    private var _binding: FragmentHomeRecommendBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModel()

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

        // 触发数据加载（ViewModel init 已自动加载，此处确保刷新）
        homeViewModel.loadHomeData()
    }

    private fun setupRecyclerViews() {
        // 推荐歌单 - 3列网格
        playlistAdapter = HomePlaylistCardAdapter(emptyList()) { playlistId, playlistName ->
            // 跳转到歌单详情页
            val fragment = code.name.monkey.retromusic.fragments.songlist.SonglistDetailFragment.newInstance(playlistId, playlistName)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.retro_fragment_open_enter,
                    R.anim.retro_fragment_open_exit,
                    R.anim.retro_fragment_close_enter,
                    R.anim.retro_fragment_close_exit
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recommendedPlaylists.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = playlistAdapter
            isNestedScrollingEnabled = false
        }

        // 热门歌手 - 横向滚动
        artistAdapter = HomeArtistAdapter(emptyList())
        binding.hotArtists.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = artistAdapter
        }

        // 新歌推荐 - 列表
        songAdapter = NeteaseSongAdapter(emptyList())
        binding.newSongs.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        // 每日推荐卡片点击
        binding.featuredPlay.setOnClickListener {
            // TODO: 跳转到每日推荐详情
            android.widget.Toast.makeText(requireContext(), R.string.daily_recommend, android.widget.Toast.LENGTH_SHORT).show()
        }

        // 快捷入口
        binding.quickRoaming.root.setOnClickListener {
            // TODO: 私人漫游
        }
        binding.quickRadar.root.setOnClickListener {
            // TODO: 雷达歌单
        }
        binding.quickCustom.root.setOnClickListener {
            // TODO: 专属定制
        }
    }

    private fun observeViewModel() {
        // 监听加载状态
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: 显示/隐藏加载骨架屏
        }

        // 推荐歌单
        homeViewModel.personalizedPlaylists.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    showPlaylists(result.data)
                }
                is Result.Error -> {
                    android.util.Log.e("HomeRecommend", "加载推荐歌单失败", result.error)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "推荐歌单加载失败: ${result.error.message ?: "未知错误"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                is Result.Loading -> {
                }
            }
        }

        // 热门歌手
        homeViewModel.hotArtists.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    artistAdapter?.updateData(result.data.take(10))
                }
                is Result.Error -> {
                    android.util.Log.e("HomeRecommend", "加载热门歌手失败", result.error)
                }
                is Result.Loading -> {
                }
            }
        }

        // 新歌推荐
        homeViewModel.newSongs.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    // TODO: 转换为 Song 对象并更新适配器
                }
                is Result.Error -> {
                    android.util.Log.e("HomeRecommend", "加载新歌推荐失败", result.error)
                }
                is Result.Loading -> {
                }
            }
        }
    }

    private fun showPlaylists(playlists: List<PersonalizedPlaylist>) {
        // 只显示前9个歌单（3列x3行）
        playlistAdapter?.updateData(playlists.take(9))
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

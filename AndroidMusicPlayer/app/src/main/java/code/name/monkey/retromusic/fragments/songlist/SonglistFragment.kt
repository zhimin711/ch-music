package code.name.monkey.retromusic.fragments.songlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.home.HomePlaylistCardAdapter
import code.name.monkey.retromusic.databinding.FragmentSonglistBinding
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.viewmodel.HomeViewModel
import com.google.android.material.tabs.TabLayout
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 歌单分类页
 * 包含顶部分类 Tab 和歌单网格列表
 */
class SonglistFragment : Fragment() {

    private var _binding: FragmentSonglistBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModel()

    private var playlistAdapter: HomePlaylistCardAdapter? = null
    private var currentCategory = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSonglistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        // 加载歌单分类
        homeViewModel.loadPlaylistCategories()
        // 加载默认分类的歌单
        homeViewModel.loadPlaylistByCategory("")
    }

    private fun setupRecyclerView() {
        playlistAdapter = HomePlaylistCardAdapter(emptyList()) { playlistId, playlistName ->
            // 跳转到歌单详情页
            val fragment = SonglistDetailFragment.newInstance(playlistId, playlistName)
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
        binding.playlistGrid.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = playlistAdapter
        }

        // TODO: 添加无限滚动加载更多
        // binding.playlistGrid.addOnScrollListener(...)
    }

    private fun observeViewModel() {
        // 歌单分类
        homeViewModel.playlistCategories.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    setupCategoryTabs(result.data.sub ?: emptyList())
                }
                is Result.Error -> {
                }
                is Result.Loading -> {
                }
            }
        }

        // 歌单列表
        homeViewModel.playlistList.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    result.data.playlists?.let { playlists ->
                        playlistAdapter?.updateData(playlists)
                    }
                }
                is Result.Error -> {
                }
                is Result.Loading -> {
                }
            }
        }
    }

    private fun setupCategoryTabs(categories: List<code.name.monkey.retromusic.network.models.PlaylistCategory>) {
        binding.categoryTabLayout.removeAllTabs()

        // 添加"全部"分类
        binding.categoryTabLayout.addTab(
            binding.categoryTabLayout.newTab().setText(R.string.category_all).setTag("")
        )

        // 添加热门分类（取前10个）
        categories.take(10).forEach { category ->
            binding.categoryTabLayout.addTab(
                binding.categoryTabLayout.newTab().setText(category.name).setTag(category.name)
            )
        }

        binding.categoryTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = tab.tag as? String ?: ""
                if (currentCategory != category) {
                    currentCategory = category
                    homeViewModel.loadPlaylistByCategory(category)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playlistGrid.adapter = null
        playlistAdapter = null
        _binding = null
    }

    companion object {
        fun newInstance(): SonglistFragment = SonglistFragment()
    }
}

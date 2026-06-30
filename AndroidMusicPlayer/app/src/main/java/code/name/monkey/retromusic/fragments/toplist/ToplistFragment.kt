package code.name.monkey.retromusic.fragments.toplist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.home.ToplistAdapter
import code.name.monkey.retromusic.databinding.FragmentToplistBinding
import code.name.monkey.retromusic.fragments.songlist.SonglistDetailFragment
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.viewmodel.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 排行榜页面
 * 显示所有官方排行榜
 */
class ToplistFragment : Fragment() {

    private var _binding: FragmentToplistBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModel()

    private var toplistAdapter: ToplistAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToplistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        // 加载排行榜
        homeViewModel.loadToplist()
    }

    private fun setupRecyclerView() {
        toplistAdapter = ToplistAdapter(emptyList()) { playlistId, playlistName ->
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
        binding.toplistGrid.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = toplistAdapter
        }
    }

    private fun observeViewModel() {
        homeViewModel.toplist.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    toplistAdapter?.updateData(result.data)
                }
                is Result.Error -> {
                }
                is Result.Loading -> {
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.toplistGrid.adapter = null
        toplistAdapter = null
        _binding = null
    }

    companion object {
        fun newInstance(): ToplistFragment = ToplistFragment()
    }
}

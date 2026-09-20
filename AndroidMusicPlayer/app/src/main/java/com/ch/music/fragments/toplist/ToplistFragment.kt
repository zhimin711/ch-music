package com.ch.music.fragments.toplist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.ch.music.R
import com.ch.music.adapter.home.ToplistAdapter
import com.ch.music.databinding.FragmentToplistBinding
import com.ch.music.fragments.songlist.SonglistDetailFragment
import com.ch.music.network.Result
import com.ch.music.viewmodel.HomeViewModel
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
            SonglistDetailFragment.navigateTo(requireActivity(), playlistId, playlistName)
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
                    Log.e("Toplist", "加载排行榜失败", result.error)
                    Toast.makeText(
                        requireContext(),
                        "排行榜加载失败: ${result.error.message ?: "未知错误"}",
                        Toast.LENGTH_SHORT
                    ).show()
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

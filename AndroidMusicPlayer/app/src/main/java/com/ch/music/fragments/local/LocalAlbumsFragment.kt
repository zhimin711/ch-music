package com.ch.music.fragments.local

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ch.music.EXTRA_ALBUM_ID
import com.ch.music.R
import com.ch.music.adapter.album.AlbumAdapter
import com.ch.music.databinding.FragmentLocalListBinding
import com.ch.music.fragments.LibraryViewModel
import com.ch.music.fragments.ReloadType
import com.ch.music.interfaces.IAlbumClickListener
import com.ch.music.util.PreferenceUtil
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LocalAlbumsFragment : Fragment(R.layout.fragment_local_list), IAlbumClickListener {

    private var _binding: FragmentLocalListBinding? = null
    private val binding get() = _binding!!

    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private var albumAdapter: AlbumAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalListBinding.bind(view)

        albumAdapter = AlbumAdapter(
            requireActivity(),
            emptyList(),
            PreferenceUtil.albumGridStyle.layoutResId,
            this
        )
        albumAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() { updateEmpty() }
        })

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireActivity(), PreferenceUtil.albumGridSize)
            adapter = albumAdapter
            setHasFixedSize(true)
        }
        binding.emptyText.setText(R.string.no_albums)
        binding.shuffleButton.isVisible = false

        libraryViewModel.getAlbums().observe(viewLifecycleOwner) { list ->
            albumAdapter?.swapDataSet(list ?: emptyList())
            updateEmpty()
        }
    }

    private fun updateEmpty() {
        val count = albumAdapter?.itemCount ?: 0
        binding.emptyContainer.isVisible = count == 0
    }

    override fun onAlbumClick(albumId: Long, view: View) {
        findNavController().navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to albumId),
            null,
            FragmentNavigatorExtras(view to albumId.toString())
        )
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Albums)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        albumAdapter = null
        _binding = null
    }
}

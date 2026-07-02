package code.name.monkey.retromusic.fragments.local

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.EXTRA_ARTIST_ID
import code.name.monkey.retromusic.EXTRA_ARTIST_NAME
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.artist.ArtistAdapter
import code.name.monkey.retromusic.databinding.FragmentLocalListBinding
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.interfaces.IAlbumArtistClickListener
import code.name.monkey.retromusic.interfaces.IArtistClickListener
import code.name.monkey.retromusic.util.PreferenceUtil
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LocalArtistsFragment : Fragment(R.layout.fragment_local_list),
    IArtistClickListener, IAlbumArtistClickListener {

    private var _binding: FragmentLocalListBinding? = null
    private val binding get() = _binding!!

    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private var artistAdapter: ArtistAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalListBinding.bind(view)

        artistAdapter = ArtistAdapter(
            requireActivity(),
            emptyList(),
            PreferenceUtil.artistGridStyle.layoutResId,
            this,
            this
        )
        artistAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() { updateEmpty() }
        })

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireActivity(), PreferenceUtil.artistGridSize)
            adapter = artistAdapter
            setHasFixedSize(true)
        }
        binding.emptyText.setText(R.string.no_artists)
        binding.shuffleButton.isVisible = false

        libraryViewModel.getArtists().observe(viewLifecycleOwner) { list ->
            artistAdapter?.swapDataSet(list ?: emptyList())
            updateEmpty()
        }
    }

    private fun updateEmpty() {
        val count = artistAdapter?.itemCount ?: 0
        binding.emptyContainer.isVisible = count == 0
    }

    override fun onArtist(artistId: Long, view: View) {
        findNavController().navigate(
            R.id.artistDetailsFragment,
            bundleOf(EXTRA_ARTIST_ID to artistId),
            null,
            FragmentNavigatorExtras(view to "artist")
        )
    }

    override fun onAlbumArtist(artistName: String, view: View) {
        findNavController().navigate(
            R.id.albumArtistDetailsFragment,
            bundleOf(EXTRA_ARTIST_NAME to artistName),
            null,
            FragmentNavigatorExtras(view to "artist")
        )
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Artists)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        artistAdapter = null
        _binding = null
    }
}

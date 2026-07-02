package code.name.monkey.retromusic.fragments.local

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.song.SongAdapter
import code.name.monkey.retromusic.databinding.FragmentLocalListBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.util.PreferenceUtil
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * "本地-歌曲" 子页：直接复用 SongAdapter + LibraryViewModel.getSongs()，
 * 但不去接管 ActionBar / 顶部 AppBar，也不注入 MenuProvider。
 */
class LocalSongsFragment : Fragment(R.layout.fragment_local_list) {

    private var _binding: FragmentLocalListBinding? = null
    private val binding get() = _binding!!

    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private var songAdapter: SongAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalListBinding.bind(view)

        songAdapter = SongAdapter(
            requireActivity(),
            mutableListOf(),
            PreferenceUtil.songGridStyle.layoutResId
        )
        songAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                updateEmpty()
            }
        })

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireActivity(), PreferenceUtil.songGridSize)
            adapter = songAdapter
            setHasFixedSize(true)
        }
        binding.emptyText.setText(R.string.no_songs)

        binding.shuffleButton.isVisible = true
        binding.shuffleButton.setOnClickListener {
            libraryViewModel.shuffleSongs()
        }
        binding.shuffleButton.accentColor()

        libraryViewModel.getSongs().observe(viewLifecycleOwner) { list ->
            songAdapter?.swapDataSet(list ?: emptyList())
            updateEmpty()
        }
    }

    private fun updateEmpty() {
        val count = songAdapter?.itemCount ?: 0
        binding.emptyContainer.isVisible = count == 0
        binding.shuffleButton.isVisible = count > 0
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        songAdapter = null
        _binding = null
    }
}

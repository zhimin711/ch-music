/*
 * Copyright (c) 2026 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.NeteaseSongAdapter
import code.name.monkey.retromusic.databinding.FragmentHomePagerItemBinding
import code.name.monkey.retromusic.model.Song

/** One page inside the home ViewPager. Reused for every tab — the layout itself is the
 *  NetEase-style "Recommended" feed, and the [position] only affects the section title. */
class HomePagerItemFragment : Fragment() {

    private var _binding: FragmentHomePagerItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomePagerItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val position = arguments?.getInt(ARG_POSITION) ?: 1

        // Section title varies per tab so the pages feel like distinct feeds.
        binding.sectionTitle.text = when (position) {
            0 -> getString(R.string.tab_heart) + " · " + getString(R.string.section_sad_picks)
            1 -> getString(R.string.section_sad_picks)
            2 -> getString(R.string.tab_music) + " · " + getString(R.string.daily_recommend)
            3 -> getString(R.string.tab_podcast)
            4 -> getString(R.string.tab_audiobook)
            else -> getString(R.string.tab_free) + " · " + getString(R.string.daily_recommend)
        }

        binding.quickRoaming.quickTitle.setText(R.string.quick_roaming)
        binding.quickRoaming.quickSubtitle.setText(R.string.quick_roaming_sub)
        binding.quickRadar.quickTitle.setText(R.string.quick_radar)
        binding.quickRadar.quickSubtitle.setText(R.string.quick_radar_sub)
        binding.quickCustom.quickTitle.setText(R.string.quick_custom)
        binding.quickCustom.quickSubtitle.setText(R.string.quick_custom_sub)

        binding.featuredPlay.setOnClickListener {
            android.widget.Toast.makeText(
                requireContext(),
                R.string.daily_recommend,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        val adapter = NeteaseSongAdapter(getDemoSongs())
        binding.songList.layoutManager = LinearLayoutManager(requireContext())
        binding.songList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.songList.adapter = null
        _binding = null
    }

    private fun getDemoSongs(): List<Song> {
        val titles = listOf(
            "关山酒（天命轻狂 应似孤鸿游）",
            "若问相思 ——七夕外观",
            "万重山（轻舟已过万重山）",
            "江湖夜雨十年灯",
            "烟火星辰",
            "长亭外"
        )
        val artists = listOf("萧默然", "兰音Reine", "平生不晚", "空想之喵", "拾光乐团", "古道西风")
        return titles.mapIndexed { index, title ->
            Song(
                id = (index + 1).toLong(),
                title = title,
                trackNumber = index + 1,
                year = 2025,
                duration = 240_000L,
                data = "",
                dateModified = 0L,
                albumId = 0L,
                albumName = "推荐",
                artistId = 0L,
                artistName = artists[index],
                composer = null,
                albumArtist = null
            )
        }
    }

    companion object {
        private const val ARG_POSITION = "arg_position"
        fun newInstance(position: Int): HomePagerItemFragment = HomePagerItemFragment().apply {
            arguments = Bundle().apply { putInt(ARG_POSITION, position) }
        }
    }
}

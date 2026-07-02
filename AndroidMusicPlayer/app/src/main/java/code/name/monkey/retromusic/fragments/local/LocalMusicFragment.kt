package code.name.monkey.retromusic.fragments.local

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentLocalMusicBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * "本地"标签容器：内嵌 4 个子 Tab —— 歌曲 / 专辑 / 艺人 / 文件夹。
 * 首页 HomeFragment 已经承担了顶部头像/搜索/一级 Tab；本容器只放二级 Tab + Pager，
 * 与外层顶栏互相独立，避免 ActionBar 冲突。
 */
class LocalMusicFragment : Fragment(R.layout.fragment_local_music) {

    private var _binding: FragmentLocalMusicBinding? = null
    private val binding get() = _binding!!

    private val tabTitles = listOf(
        R.string.songs,
        R.string.albums,
        R.string.artists,
        R.string.folders
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalMusicBinding.bind(view)

        binding.localPager.adapter = LocalPagerAdapter(this)
        binding.localPager.offscreenPageLimit = 1

        TabLayoutMediator(binding.localTabLayout, binding.localPager) { tab, position ->
            tab.text = getString(tabTitles[position])
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.localPager.adapter = null
        _binding = null
    }

    private class LocalPagerAdapter(host: Fragment) : FragmentStateAdapter(host) {
        override fun getItemCount(): Int = 4
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> LocalSongsFragment()
            1 -> LocalAlbumsFragment()
            2 -> LocalArtistsFragment()
            3 -> LocalFoldersFragment()
            else -> LocalSongsFragment()
        }
    }

    companion object {
        fun newInstance(): LocalMusicFragment = LocalMusicFragment()
    }
}

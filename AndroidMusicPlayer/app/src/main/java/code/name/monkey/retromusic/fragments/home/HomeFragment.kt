/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.fragments.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentHomeBinding
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.fragments.songlist.SonglistFragment
import code.name.monkey.retromusic.fragments.toplist.ToplistFragment
import code.name.monkey.retromusic.interfaces.IScrollHelper
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment :
    AbsMainActivityFragment(R.layout.fragment_home), IScrollHelper {

    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    private val tabTitles = listOf(
        R.string.tab_recommend,
        R.string.tab_songlist,
        R.string.tab_toplist
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeBinding = FragmentHomeBinding.bind(view)
        _binding = HomeBinding(homeBinding)

        // Hide the legacy title-bearing toolbar action bar; the new layout has its own top bar.
        mainActivity.supportActionBar?.hide()

        binding.homePager.adapter = HomePagerAdapter(this)
        binding.homePager.offscreenPageLimit = 1

        TabLayoutMediator(binding.topTabLayout, binding.homePager) { tab, position ->
            tab.text = getString(tabTitles[position])
        }.attach()

        binding.userImage.setOnClickListener {
            mainActivity.openDrawer()
        }

        // 顶部搜索图标：跳转到搜索页
        binding.searchInput.setOnClickListener {
            findNavController().navigate(
                R.id.action_search,
                null,
                androidx.navigation.navOptions {
                    launchSingleTop = true
                    anim {
                        enter = R.anim.retro_fragment_open_enter
                        exit = R.anim.retro_fragment_open_exit
                        popEnter = R.anim.retro_fragment_close_enter
                        popExit = R.anim.retro_fragment_close_exit
                    }
                }
            )
        }
    }

    override fun scrollToTop() {
        binding.appBarLayout.setExpanded(true, true)
        binding.homePager.setCurrentItem(0, true) // jump to first tab content (首页)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // NetEase-style home has no top app-bar menu; intentionally empty.
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

    override fun onDestroyView() {
        super.onDestroyView()
        binding.homePager.adapter = null
        _binding = null
    }

    /** Pager that uses distinct fragments for each tab. */
    private class HomePagerAdapter(host: HomeFragment) : FragmentStateAdapter(host) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> HomeRecommendFragment.newInstance()  // 推荐
            1 -> SonglistFragment.newInstance()       // 歌单
            2 -> ToplistFragment.newInstance()        // 排行榜
            else -> HomeRecommendFragment.newInstance()
        }
    }
}

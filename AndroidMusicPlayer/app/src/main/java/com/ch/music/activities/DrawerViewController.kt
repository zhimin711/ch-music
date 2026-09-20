/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 */
package com.ch.music.activities

import android.content.Context
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.ch.music.HISTORY_PLAYLIST
import com.ch.music.R
import com.ch.music.dialogs.SleepTimerDialog
import com.ch.music.extensions.findNavController
import com.ch.music.extensions.showToast
import com.ch.music.musicserver.MusicServerRepository
import com.ch.music.musicserver.MusicServerState
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView
import com.ch.music.views.RetroShapeableImageView
import kotlinx.coroutines.launch

/**
 * 侧边抽屉 UI 控制器：
 * - 观察 [MusicServerRepository.state]，切换登录/未登录态的菜单项显隐
 * - 更新头部头像/用户名/副标题
 * - 分发菜单点击到对应 fragment / dialog
 */
class DrawerViewController(
    private val activity: MainActivity,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView,
    private val musicServerRepository: MusicServerRepository
) {

    /** 抽屉头部（inflate 后拿到句柄） */
    private var headerRoot: View? = null
    private var headerAvatar: RetroShapeableImageView? = null
    private var headerUsername: MaterialTextView? = null
    private var headerSubtitle: MaterialTextView? = null

    private val navController: NavController
        get() = activity.findNavController(R.id.fragment_container)

    fun attach(lifecycleOwner: LifecycleOwner) {
        setupHeader()
        setupItemListener()
        observeState(lifecycleOwner)
    }

    private fun setupHeader() {
        val header = navigationView.getHeaderView(0)
            ?: navigationView.inflateHeaderView(R.layout.nav_header)
        headerRoot = header.findViewById(R.id.drawerHeaderRoot)
        headerAvatar = header.findViewById(R.id.drawerAvatar)
        headerUsername = header.findViewById(R.id.drawerUsername)
        headerSubtitle = header.findViewById(R.id.drawerSubtitle)

        headerRoot?.setOnClickListener {
            // 点击头部：无论登录/未登录，都跳到 UserInfoFragment
            drawerLayout.closeDrawer(GravityCompat.START)
            navigateUserInfo("profile")
        }
    }

    private fun setupItemListener() {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                // 登录后可见
                R.id.drawer_profile -> requireLogin { navigateUserInfo("profile") }
                R.id.drawer_playlists -> requireLogin { navigateUserInfo("playlists") }
                R.id.drawer_favorites -> requireLogin { navigateUserInfo("favorites") }
                R.id.drawer_music_library -> requireLogin { navigateUserInfo("music_library") }
                R.id.drawer_logout -> {
                    activity.lifecycleScope.launch {
                        runCatching { musicServerRepository.logout() }
                    }
                }
                // 通用项
                R.id.drawer_recent -> {
                    val args = bundleOf("type" to HISTORY_PLAYLIST)
                    navController.navigate(R.id.detailListFragment, args)
                }
                R.id.drawer_sleep -> {
                    SleepTimerDialog().show(activity.supportFragmentManager, "SLEEP_TIMER")
                }
                R.id.drawer_settings -> navController.navigate(R.id.settings_fragment)
                else -> return@setNavigationItemSelectedListener false
            }
            if (item.itemId != R.id.drawer_logout && item.itemId != R.id.drawer_sleep) {
                item.isChecked = true
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun navigateUserInfo(defaultTab: String) {
        val args = bundleOf("defaultTab" to defaultTab)
        navController.navigate(R.id.user_info_fragment, args)
    }

    /** 需要登录时使用：未登录则提示并跳到 UserInfoFragment 的登录表单 */
    private fun requireLogin(block: () -> Unit) {
        if (musicServerRepository.state.value.isLoggedIn) {
            block()
        } else {
            activity.showToast(R.string.drawer_login_required_toast)
            navigateUserInfo("profile")
        }
    }

    private fun observeState(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                musicServerRepository.state.collect { state ->
                    applyState(state)
                }
            }
        }
    }

    private fun applyState(state: MusicServerState) {
        val loggedIn = state.isLoggedIn

        // 菜单：登录组和 logout 只在登录后可见
        val menu = navigationView.menu
        setGroupVisible(menu, R.id.drawer_group_account, loggedIn)
        menu.findItem(R.id.drawer_logout)?.isVisible = loggedIn

        // 头部：用户名 + 副标题
        val ctx: Context = activity
        if (loggedIn) {
            val user = state.user
            headerUsername?.text = user?.displayLabel
                ?: ctx.getString(R.string.drawer_not_logged_in)
            headerSubtitle?.text = user?.username?.takeIf { it.isNotBlank() }
                ?: ctx.getString(R.string.drawer_tap_to_login)
        } else {
            headerUsername?.text = ctx.getString(R.string.drawer_not_logged_in)
            headerSubtitle?.text = ctx.getString(R.string.drawer_tap_to_login)
        }

        // 头像：blob > avatarUrl > 默认占位图（放弃本地 profile.jpg 兜底，避免频繁 ENOENT）
        headerAvatar?.let { avatar ->
            val blob = musicServerRepository.avatarBlob
            val avatarUrl = state.user?.avatarUrl
            when {
                blob != null && blob.isNotEmpty() -> {
                    Glide.with(ctx)
                        .load(blob)
                        .placeholder(R.drawable.ic_person_flat)
                        .error(R.drawable.ic_person_flat)
                        .into(avatar)
                }
                !avatarUrl.isNullOrBlank() -> {
                    Glide.with(ctx)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_person_flat)
                        .error(R.drawable.ic_person_flat)
                        .into(avatar)
                }
                else -> {
                    avatar.setImageResource(R.drawable.ic_person_flat)
                }
            }
            avatar.isVisible = true
        }
    }

    private fun setGroupVisible(
        menu: android.view.Menu,
        groupId: Int,
        visible: Boolean
    ) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.groupId == groupId) {
                item.isVisible = visible
            }
        }
    }
}

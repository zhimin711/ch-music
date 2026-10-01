package com.ch.music.fragments.misc

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ch.music.R
import com.ch.music.databinding.FragmentMineBinding
import com.ch.music.musicserver.MusicServerCacheState
import com.ch.music.musicserver.MusicServerRepository
import com.ch.music.musicserver.MusicServerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MineFragment : Fragment(R.layout.fragment_mine) {
    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!
    private val repository: MusicServerRepository by inject()
    private var countsLoaded = false
    private var refreshing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMineBinding.bind(view)
        countsLoaded = false
        binding.profile.setOnClickListener { openAccount("profile") }
        binding.account.setOnClickListener { openAccount("profile") }
        binding.favorites.setOnClickListener { openAccount("favorites") }
        binding.playlists.setOnClickListener { openAccount("playlists") }
        binding.library.setOnClickListener { openAccount("music_library") }
        binding.cache.setOnClickListener { openAccount("music_library") }
        binding.refreshStatus.setOnClickListener { refreshAccount() }
        refreshAccount()
        binding.settings.setOnClickListener { findNavController().navigate(R.id.settings_fragment) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.state.collect { render(it) }
            }
        }
    }

    private fun refreshAccount() {
        if (refreshing) return
        refreshing = true
        binding.refreshStatus.isVisible = true
        binding.refreshStatus.isEnabled = false
        binding.refreshStatus.setText(R.string.ch_account_loading)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.restoreSession()
                countsLoaded = true
                render(repository.state.value)
                binding.refreshStatus.isVisible = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                binding.refreshStatus.setText(R.string.ch_retry)
                binding.refreshStatus.isEnabled = true
            } finally {
                refreshing = false
            }
        }
    }

    private fun openAccount(tab: String) {
        findNavController().navigate(R.id.user_info_fragment, bundleOf("defaultTab" to tab))
    }

    private fun render(state: MusicServerState) {
        binding.accountName.text = state.user?.displayLabel ?: getString(R.string.ch_sign_in)
        binding.accountHint.setText(if (state.isLoggedIn) R.string.ch_account_hint else R.string.ch_login_hint)
        binding.favorites.text = entryLabel(R.string.ch_favorites, state.favorites.size, state.isLoggedIn && countsLoaded)
        binding.playlists.text = entryLabel(R.string.ch_playlists, state.playlists.size, state.isLoggedIn && countsLoaded)
        binding.library.text = entryLabel(R.string.ch_library, state.music.size, state.isLoggedIn && countsLoaded)
        val count = state.cacheEntries.values.count {
            it.userId == state.user?.id && it.state == MusicServerCacheState.READY
        }
        binding.cacheSummary.isVisible = state.isLoggedIn && countsLoaded
        binding.cacheSummary.text = getString(R.string.ch_cache_summary, count)
        Glide.with(this)
            .load(if (state.isLoggedIn) repository.avatarBlob ?: state.user?.avatarUrl else null)
            .placeholder(R.drawable.ic_person_flat)
            .error(R.drawable.ic_person_flat)
            .into(binding.avatar)
    }

    private fun entryLabel(label: Int, count: Int, loggedIn: Boolean): String =
        if (loggedIn) "${getString(label)} · $count" else getString(label)

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

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
package com.ch.music.fragments.other

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import com.ch.music.R
import com.ch.music.databinding.FragmentMiniPlayerBinding
import com.ch.music.extensions.show
import com.ch.music.fragments.base.AbsMusicServiceFragment
import com.ch.music.glide.RetroGlideExtension
import com.ch.music.glide.RetroGlideExtension.songCoverOptions
import com.ch.music.helper.MusicPlayerRemote
import com.ch.music.helper.MusicProgressViewUpdateHelper
import com.ch.music.helper.PlayPauseButtonOnClickHandler
import com.ch.music.util.ArtistSeparator
import com.ch.music.util.PreferenceUtil
import com.ch.music.util.RetroUtil
import com.bumptech.glide.Glide
import kotlin.math.abs

open class MiniPlayerFragment : AbsMusicServiceFragment(R.layout.fragment_mini_player),
    MusicProgressViewUpdateHelper.Callback, View.OnClickListener {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.actionNext -> MusicPlayerRemote.playNextSong()
            R.id.actionPrevious -> MusicPlayerRemote.back()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)
        view.setOnTouchListener(FlingPlayBackController(requireContext()))
        setUpMiniPlayer()
        setUpButtons()
    }

    fun setUpButtons() {
        if (RetroUtil.isTablet) {
            binding.actionNext.show()
            binding.actionPrevious.show()
        } else {
            binding.actionNext.isVisible = PreferenceUtil.isExtraControls
            binding.actionPrevious.isVisible = PreferenceUtil.isExtraControls
        }
        binding.actionNext.setOnClickListener(this)
        binding.actionPrevious.setOnClickListener(this)
    }

    private fun setUpMiniPlayer() {
        setUpPlayPauseButton()
        binding.actionQueue.setOnClickListener {
            androidx.navigation.Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(R.id.playing_queue_fragment)
        }
    }

    private fun setUpPlayPauseButton() {
        binding.miniPlayerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    private fun updateSongTitle() {
        val song = MusicPlayerRemote.currentSong

        binding.miniPlayerTitle.text = song.title
        binding.miniPlayerArtist.text = ArtistSeparator.split(song.artistName).joinToString(", ")
    }

    private fun updateSongCover() {
        val song = MusicPlayerRemote.currentSong
        Glide.with(requireContext())
            .load(RetroGlideExtension.getSongModel(song))
            .transition(RetroGlideExtension.getDefaultTransition())
            .songCoverOptions(song)
            .into(binding.image)
    }

    override fun onServiceConnected() {
        updateSongTitle()
        updateSongCover()
        updatePlayPauseDrawableState()
    }

    override fun onPlayingMetaChanged() {
        updateSongTitle()
        updateSongCover()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.progressBar.max = total
        binding.progressBar.progress = progress
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    protected fun updatePlayPauseDrawableState() {
        binding.miniPlayerPlayPauseButton.contentDescription = getString(
            if (MusicPlayerRemote.isPlaying) R.string.ch_pause else R.string.ch_play
        )
        if (MusicPlayerRemote.isPlaying) {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    class FlingPlayBackController(context: Context) : View.OnTouchListener {

        private var flingPlayBackController = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) {
                            MusicPlayerRemote.playNextSong()
                            return true
                        } else if (velocityX > 0) {
                            MusicPlayerRemote.playPreviousSong()
                            return true
                        }
                    }
                    return false
                }
            })

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return flingPlayBackController.onTouchEvent(event)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

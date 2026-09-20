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
package com.ch.music.activities.base

import android.Manifest
import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ch.appthemehelper.util.VersionUtils
import com.ch.music.R
import com.ch.music.db.toPlayCount
import com.ch.music.helper.MusicPlayerRemote
import com.ch.music.interfaces.IMusicServiceEventListener
import com.ch.music.repository.RealRepository
import com.ch.music.util.PreferenceUtil
import com.ch.music.util.logD
import com.ch.music.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.lang.ref.WeakReference

abstract class AbsMusicServiceActivity : AbsBaseActivity(), IMusicServiceEventListener {

    private val mMusicServiceEventListeners = ArrayList<IMusicServiceEventListener>()
    private val repository: RealRepository by inject()
    private var serviceToken: MusicPlayerRemote.ServiceToken? = null
    private var musicStateReceiver: MusicStateReceiver? = null
    private var receiverRegistered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceToken = MusicPlayerRemote.bindToService(this, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                this@AbsMusicServiceActivity.onServiceConnected()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                this@AbsMusicServiceActivity.onServiceDisconnected()
            }
        })

        setPermissionDeniedMessage(getString(R.string.permission_external_storage_denied))
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPlayerRemote.unbindFromService(serviceToken)
        if (receiverRegistered && musicStateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(musicStateReceiver!!)
            receiverRegistered = false
        }
    }

    fun addMusicServiceEventListener(listenerI: IMusicServiceEventListener?) {
        if (listenerI != null) {
            mMusicServiceEventListeners.add(listenerI)
        }
    }

    fun removeMusicServiceEventListener(listenerI: IMusicServiceEventListener?) {
        if (listenerI != null) {
            mMusicServiceEventListeners.remove(listenerI)
        }
    }

    override fun onServiceConnected() {
        if (!receiverRegistered) {
            musicStateReceiver = MusicStateReceiver(this)

            val filter = IntentFilter()
            filter.addAction(MusicService.PLAY_STATE_CHANGED)
            filter.addAction(MusicService.SHUFFLE_MODE_CHANGED)
            filter.addAction(MusicService.REPEAT_MODE_CHANGED)
            filter.addAction(MusicService.META_CHANGED)
            filter.addAction(MusicService.QUEUE_CHANGED)
            filter.addAction(MusicService.MEDIA_STORE_CHANGED)
            filter.addAction(MusicService.FAVORITE_STATE_CHANGED)

            LocalBroadcastManager.getInstance(this).registerReceiver(musicStateReceiver!!, filter)
            receiverRegistered = true
        }

        for (listener in mMusicServiceEventListeners) {
            listener.onServiceConnected()
        }
    }

    override fun onServiceDisconnected() {
        if (receiverRegistered && musicStateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(musicStateReceiver!!)
            receiverRegistered = false
        }

        for (listener in mMusicServiceEventListeners) {
            listener.onServiceDisconnected()
        }
    }

    override fun onPlayingMetaChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayingMetaChanged()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            if (!PreferenceUtil.pauseHistory) {
                repository.upsertSongInHistory(MusicPlayerRemote.currentSong)
            }
            val song = repository.findSongExistInPlayCount(MusicPlayerRemote.currentSong.id)
                ?.apply { playCount += 1 }
                ?: MusicPlayerRemote.currentSong.toPlayCount()

            repository.upsertSongInPlayCount(song)
        }
    }

    override fun onQueueChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onQueueChanged()
        }
    }

    override fun onPlayStateChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayStateChanged()
        }
    }

    override fun onMediaStoreChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onMediaStoreChanged()
        }
    }

    override fun onRepeatModeChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onRepeatModeChanged()
        }
    }

    override fun onShuffleModeChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onShuffleModeChanged()
        }
    }

    override fun onFavoriteStateChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onFavoriteStateChanged()
        }
    }

    override fun onHasPermissionsChanged(hasPermissions: Boolean) {
        super.onHasPermissionsChanged(hasPermissions)
        val intent = Intent(MusicService.MEDIA_STORE_CHANGED)
        intent.putExtra(
            "from_permissions_changed",
            true
        ) // just in case we need to know this at some point
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        logD("sendBroadcast $hasPermissions")
    }

    override fun getPermissionsToRequest(): Array<String> {
        return mutableListOf<String>().apply {
            if (VersionUtils.hasT()) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (!VersionUtils.hasR()) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private class MusicStateReceiver(activity: AbsMusicServiceActivity) : BroadcastReceiver() {

        private val reference: WeakReference<AbsMusicServiceActivity> = WeakReference(activity)

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val activity = reference.get()
            if (activity != null && action != null) {
                when (action) {
                    MusicService.FAVORITE_STATE_CHANGED -> activity.onFavoriteStateChanged()
                    MusicService.META_CHANGED -> activity.onPlayingMetaChanged()
                    MusicService.QUEUE_CHANGED -> activity.onQueueChanged()
                    MusicService.PLAY_STATE_CHANGED -> activity.onPlayStateChanged()
                    MusicService.REPEAT_MODE_CHANGED -> activity.onRepeatModeChanged()
                    MusicService.SHUFFLE_MODE_CHANGED -> activity.onShuffleModeChanged()
                    MusicService.MEDIA_STORE_CHANGED -> activity.onMediaStoreChanged()
                }
            }
        }
    }

    companion object {
        val TAG: String = AbsMusicServiceActivity::class.java.simpleName
    }
}

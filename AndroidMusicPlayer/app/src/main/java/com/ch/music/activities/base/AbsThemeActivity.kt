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

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.os.LocaleListCompat
import com.ch.appthemehelper.common.ATHToolbarActivity
import com.ch.appthemehelper.util.VersionUtils
import com.ch.music.R
import com.ch.music.extensions.exitFullscreen
import com.ch.music.extensions.hideStatusBar
import com.ch.music.extensions.installSplitCompat
import com.ch.music.extensions.maybeSetScreenOn
import com.ch.music.extensions.maybeShowWhenLocked
import com.ch.music.extensions.setEdgeToEdgeOrImmersive
import com.ch.music.extensions.setImmersiveFullscreen
import com.ch.music.extensions.setLightNavigationBarAuto
import com.ch.music.extensions.setLightStatusBarAuto
import com.ch.music.extensions.surfaceColor
import com.ch.music.util.PreferenceUtil
import com.ch.music.util.theme.getNightMode
import com.ch.music.util.theme.getThemeResValue

abstract class AbsThemeActivity : ATHToolbarActivity(), Runnable {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        updateLocale()
        updateTheme()
        hideStatusBar()
        super.onCreate(savedInstanceState)
        setEdgeToEdgeOrImmersive()
        maybeSetScreenOn()
        maybeShowWhenLocked()
        setLightNavigationBarAuto()
        setLightStatusBarAuto(surfaceColor())
        if (VersionUtils.hasQ()) {
            window.decorView.isForceDarkAllowed = false
        }
    }

    private fun updateTheme() {
        setTheme(getThemeResValue())
        if (PreferenceUtil.materialYou) {
            setDefaultNightMode(getNightMode())
        }

        if (PreferenceUtil.isCustomFont) {
            setTheme(R.style.FontThemeOverlay)
        }
    }

    private fun updateLocale() {
        val localeCode = PreferenceUtil.languageCode
        if (PreferenceUtil.isLocaleAutoStorageEnabled) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeCode))
            PreferenceUtil.isLocaleAutoStorageEnabled = true
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
            handler.removeCallbacks(this)
            handler.postDelayed(this, 300)
        } else {
            handler.removeCallbacks(this)
        }
    }

    override fun run() {
        setImmersiveFullscreen()
    }

    override fun onStop() {
        handler.removeCallbacks(this)
        super.onStop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        exitFullscreen()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            handler.removeCallbacks(this)
            handler.postDelayed(this, 500)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        installSplitCompat()
    }
}

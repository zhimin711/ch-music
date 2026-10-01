package com.ch.music.util

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import com.ch.music.R
import com.ch.music.extensions.resolveColor

object ArtworkGradient {
    fun create(context: Context, artworkColor: Int): GradientDrawable {
        val surface = context.resolveColor(R.attr.chPageBackground)
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(ColorUtils.blendARGB(surface, artworkColor, 0.16f), surface)
        )
    }
}

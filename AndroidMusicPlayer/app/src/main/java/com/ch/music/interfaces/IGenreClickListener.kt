package com.ch.music.interfaces

import android.view.View
import com.ch.music.model.Genre

interface IGenreClickListener {
    fun onClickGenre(genre: Genre, view: View)
}
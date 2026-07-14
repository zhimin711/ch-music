package com.ch.music.interfaces

import com.ch.music.model.Album
import com.ch.music.model.Artist
import com.ch.music.model.Genre

interface IHomeClickListener {
    fun onAlbumClick(album: Album)

    fun onArtistClick(artist: Artist)

    fun onGenreClick(genre: Genre)
}
package com.ch.music

import androidx.room.Room
import com.ch.music.auto.AutoMusicProvider
import com.ch.music.cast.RetroWebServer
import com.ch.music.db.MIGRATION_23_24
import com.ch.music.db.RetroDatabase
import com.ch.music.fragments.LibraryViewModel
import com.ch.music.fragments.albums.AlbumDetailsViewModel
import com.ch.music.fragments.artists.ArtistDetailsViewModel
import com.ch.music.fragments.genres.GenreDetailsViewModel
import com.ch.music.fragments.playlists.PlaylistDetailsViewModel
import com.ch.music.model.Genre
import com.ch.music.viewmodel.HomeViewModel
import com.ch.music.network.provideDefaultCache
import com.ch.music.viewmodel.OnlineSearchViewModel
import com.ch.music.network.provideLastFmRest
import com.ch.music.network.provideLastFmRetrofit
import com.ch.music.network.provideNeteaseRest
import com.ch.music.network.provideNeteaseRetrofit
import com.ch.music.network.provideOkHttp
import com.ch.music.network.provideNeteaseOkHttp
import com.ch.music.musicserver.MusicServerCacheManager
import com.ch.music.musicserver.MusicServerDataSourceFactory
import com.ch.music.musicserver.MusicServerRepository
import com.ch.music.musicserver.MusicServerSession
import com.ch.music.musicserver.provideMusicServerApi
import com.ch.music.musicserver.provideMusicServerOkHttp
import com.ch.music.netease.NeteasePlaybackManager
import com.ch.music.repository.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {

    factory {
        provideDefaultCache()
    }
    factory {
        provideOkHttp(get(), get())
    }
    single {
        provideLastFmRetrofit(get())
    }
    single {
        provideLastFmRest(get())
    }
    // 网易云音乐 API
    factory {
        provideNeteaseOkHttp(get(), get(), get())
    }
    single {
        provideNeteaseRetrofit(get())
    }
    single {
        provideNeteaseRest(get())
    }
}

private val roomModule = module {

    single {
        Room.databaseBuilder(androidContext(), RetroDatabase::class.java, "playlist.db")
            .addMigrations(MIGRATION_23_24)
            .build()
    }

    factory {
        get<RetroDatabase>().playlistDao()
    }

    factory {
        get<RetroDatabase>().playCountDao()
    }

    factory {
        get<RetroDatabase>().historyDao()
    }

    single {
        RealRoomRepository(get(), get(), get())
    } bind RoomRepository::class
}
private val autoModule = module {
    single {
        AutoMusicProvider(
            androidContext(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
}
private val mainModule = module {
    single {
        androidContext().contentResolver
    }
    single {
        RetroWebServer(get())
    }
}
private val dataModule = module {
    single {
        RealRepository(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    } bind Repository::class

    single {
        RealSongRepository(get())
    } bind SongRepository::class

    single {
        RealGenreRepository(get(), get())
    } bind GenreRepository::class

    single {
        RealAlbumRepository(get())
    } bind AlbumRepository::class

    single {
        RealArtistRepository(get(), get())
    } bind ArtistRepository::class

    single {
        RealPlaylistRepository(get())
    } bind PlaylistRepository::class

    single {
        RealTopPlayedRepository(get(), get(), get(), get())
    } bind TopPlayedRepository::class

    single {
        RealLastAddedRepository(
            get(),
            get(),
            get()
        )
    } bind LastAddedRepository::class

    single {
        RealSearchRepository(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single {
        RealLocalDataRepository(get())
    } bind LocalDataRepository::class

    single {
        MusicServerSession(androidContext())
    }

    single {
        MusicServerCacheManager(androidContext(), provideMusicServerOkHttp(get()))
    }

    single {
        MusicServerDataSourceFactory(androidContext(), get(), get())
    }

    single {
        MusicServerRepository(
            provideMusicServerApi(provideMusicServerOkHttp(get())),
            get(),
            get()
        )
    }

    // 网易云音乐数据仓库
    single {
        NeteaseRepository(get())
    }

    // 网易云播放管理器
    single {
        NeteasePlaybackManager(get())
    }
}

private val viewModules = module {

    viewModel {
        LibraryViewModel(get())
    }

    // 首页 ViewModel（网易云在线音乐）
    viewModel {
        HomeViewModel(get())
    }

    viewModel {
        OnlineSearchViewModel(get())
    }

    viewModel { (albumId: Long) ->
        AlbumDetailsViewModel(
            get(),
            albumId
        )
    }

    viewModel { (artistId: Long?, artistName: String?) ->
        ArtistDetailsViewModel(
            get(),
            artistId,
            artistName
        )
    }

    viewModel { (playlistId: Long) ->
        PlaylistDetailsViewModel(
            get(),
            playlistId
        )
    }

    viewModel { (genre: Genre) ->
        GenreDetailsViewModel(
            get(),
            genre
        )
    }
}

val appModules = listOf(mainModule, dataModule, autoModule, viewModules, networkModule, roomModule)

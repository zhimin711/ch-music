package code.name.monkey.retromusic.musicserver

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface MusicServerApi {
    @POST("/api/auth/register")
    suspend fun register(@Body request: MusicServerAuthRequest): MusicServerAuthResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: MusicServerAuthRequest): MusicServerAuthResponse

    @POST("/api/auth/logout")
    suspend fun logout()

    @GET("/api/auth/me")
    suspend fun me(): MusicServerUser

    @PUT("/api/auth/me")
    suspend fun updateMe(@Body request: MusicServerProfileUpdateRequest): MusicServerUser

    @Multipart
    @POST("/api/auth/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): MusicServerUser

    @GET("/api/music")
    suspend fun music(): List<MusicServerMusic>

    @Multipart
    @POST("/api/music")
    suspend fun uploadMusic(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody?,
        @Part("artist") artist: RequestBody?,
        @Part("album") album: RequestBody?
    ): MusicServerMusic

    @DELETE("/api/music/{musicId}")
    suspend fun deleteMusic(@Path("musicId") musicId: Long)

    @GET("/api/favorites")
    suspend fun favorites(): List<MusicServerFavorite>

    @POST("/api/favorites/{musicId}")
    suspend fun addFavorite(@Path("musicId") musicId: Long): List<MusicServerFavorite>

    @DELETE("/api/favorites/{musicId}")
    suspend fun removeFavorite(@Path("musicId") musicId: Long): List<MusicServerFavorite>

    @GET("/api/playlists")
    suspend fun playlists(): List<MusicServerPlaylist>

    @POST("/api/playlists")
    suspend fun createPlaylist(@Body request: MusicServerPlaylistRequest): MusicServerPlaylist

    @PUT("/api/playlists/{playlistId}")
    suspend fun updatePlaylist(
        @Path("playlistId") playlistId: Long,
        @Body request: MusicServerPlaylistRequest
    ): MusicServerPlaylist

    @DELETE("/api/playlists/{playlistId}")
    suspend fun deletePlaylist(@Path("playlistId") playlistId: Long)

    @POST("/api/playlists/{playlistId}/tracks")
    suspend fun addPlaylistTrack(
        @Path("playlistId") playlistId: Long,
        @Body request: MusicServerAddTrackRequest
    ): MusicServerPlaylist

    @DELETE("/api/playlists/{playlistId}/tracks/{trackId}")
    suspend fun removePlaylistTrack(
        @Path("playlistId") playlistId: Long,
        @Path("trackId") trackId: Long
    ): MusicServerPlaylist
}

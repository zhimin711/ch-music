package code.name.monkey.retromusic.fragments.other

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import code.name.monkey.retromusic.Constants.USER_BANNER
import code.name.monkey.retromusic.Constants.USER_PROFILE
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentUserInfoBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.applyToolbar
import code.name.monkey.retromusic.extensions.dip
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.profileBannerOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.simpleSongCoverOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.userProfileOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.musicserver.MusicServerCacheEntry
import code.name.monkey.retromusic.musicserver.MusicServerCacheState
import code.name.monkey.retromusic.musicserver.MusicServerMusic
import code.name.monkey.retromusic.musicserver.MusicServerPlaylist
import code.name.monkey.retromusic.musicserver.MusicServerRepository
import code.name.monkey.retromusic.musicserver.MusicServerSession
import code.name.monkey.retromusic.musicserver.MusicServerSongMapper
import code.name.monkey.retromusic.musicserver.MusicServerState
import code.name.monkey.retromusic.musicserver.readableMessage
import code.name.monkey.retromusic.util.ImageUtil
import code.name.monkey.retromusic.util.PreferenceUtil.userName
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.io.ByteArrayOutputStream
import java.io.File

class UserInfoFragment : Fragment() {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!
    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val musicServerRepository: MusicServerRepository by inject()
    private val musicServerSession: MusicServerSession by inject()
    private var registerMode = false
    private var lastState = MusicServerState()

    /**
     * 抽屉入口模式。见 [DrawerViewController]：
     * - "profile"       账户信息（含未登录时的登录/注册表单）
     * - "playlists"     我的歌单
     * - "favorites"     我的收藏
     * - "music_library" 私有音乐
     */
    private val defaultTab: String
        get() = arguments?.getString("defaultTab") ?: "profile"

    /**
     * 表示用户"刚刚点击了登录/注册按钮"，此次登录成功后应该跳回首页。
     * 与后台自动恢复 session（restoreSession）区分开——恢复只是把本地状态放回来，
     * 不应触发导航。
     */
    private var pendingAuthNavigate: Boolean = false

    private val pickBannerImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                setAndSaveBannerImage(uri)
            }
        }

    private val pickAvatarLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                uploadAvatar(uri)
            }
        }

    private val pickLocalUserImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                setAndSaveUserImage(uri)
            }
        }

    private val pickMusicLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                showUploadMusicDialog(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            duration = 300L
            scrimColor = Color.TRANSPARENT
        }
        _binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyToolbar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // 记录进入时的登录态（保留占位，防止外部依赖 sublist 顺序）
        setupStaticUi()
        setupActions()
        observeMusicServer()
        restoreSession()

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        libraryViewModel.getFabMargin().observe(viewLifecycleOwner) {
            binding.refresh?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it + dip(16)
            }
        }
    }

    /** 是否是"账户"入口——账户入口才展示登录卡；其他入口未登录时会引导到账户入口 */
    @Suppress("unused")
    private fun isProfileMode(): Boolean = defaultTab == "profile"

    private fun setupStaticUi() {
        binding.accountSubtitle?.text = ""
        binding.accountSubtitle?.isVisible = false
        binding.nameContainer.accentColor()
        binding.usernameContainer?.accentColor()
        binding.passwordContainer?.accentColor()
        binding.displayNameContainer?.accentColor()
        binding.authModeGroup?.check(R.id.loginMode)
        binding.displayNameContainer?.isGone = true
        loadProfile()
    }

    private fun setupActions() {
        binding.bannerImage.setOnClickListener { showBannerImageOptions() }
        binding.userImage.setOnClickListener {
            if (lastState.isLoggedIn) {
                pickAvatarLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                showLocalUserImageOptions()
            }
        }
        binding.authModeGroup?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            registerMode = checkedId == R.id.registerMode
            binding.displayNameContainer?.isVisible = registerMode
            binding.authSubmit?.setText(if (registerMode) R.string.register else R.string.login)
        }
        binding.authSubmit?.setOnClickListener { submitAuth() }
        binding.saveProfile?.setOnClickListener { saveProfile() }
        binding.next?.setOnClickListener { saveProfile() }
        val pickAvatar = View.OnClickListener {
            pickAvatarLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.userImage.setOnClickListener(pickAvatar)
        binding.logout?.setOnClickListener {
            runServerAction { musicServerRepository.logout() }
        }
        binding.refresh?.setOnClickListener {
            runServerAction { musicServerRepository.refreshAll() }
        }
        binding.uploadMusic?.setOnClickListener {
            pickMusicLauncher.launch(arrayOf("audio/*"))
        }
        binding.createPlaylist?.setOnClickListener {
            showPlaylistEditorDialog()
        }
    }

    private fun observeMusicServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                musicServerRepository.state.collect { state ->
                    lastState = state
                    render(state)
                }
            }
        }
    }

    private fun restoreSession() {
        // restoreSession 只是本地恢复，不算"用户主动登录成功"，不应触发跳转
        runServerAction(showErrors = false) {
            musicServerRepository.restoreSession()
        }
    }

    private fun submitAuth() {
        val username = binding.username?.text?.toString()?.trim().orEmpty()
        val password = binding.password?.text?.toString().orEmpty()
        val displayName = binding.displayName?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        if (username.isBlank() || password.isBlank()) {
            showToast("Username and password are required")
            return
        }
        hideKeyboard()
        pendingAuthNavigate = true
        runServerAction(
            onError = { pendingAuthNavigate = false }
        ) {
            if (registerMode) {
                musicServerRepository.register(username, password, displayName)
            } else {
                musicServerRepository.login(username, password)
            }
        }
    }

    private fun saveProfile() {
        val displayName = binding.name.text.toString().trim()
        if (displayName.isBlank()) {
            showToast(R.string.error_empty_name)
            return
        }
        runServerAction {
            musicServerRepository.updateProfile(displayName)
            userName = displayName
        }
    }

    private fun render(state: MusicServerState) {
        val loggedIn = state.isLoggedIn

        // === "用户点了登录/注册"并成功：跳回首页 ===
        if (pendingAuthNavigate && loggedIn) {
            pendingAuthNavigate = false
            popToHome()
            return
        }

        // === 未登录：无论从哪个抽屉入口进来，都只显示登录/注册表单 ===
        if (!loggedIn) {
            binding.profileHeaderCard?.isVisible = true
            binding.loginGroup?.isVisible = true
            binding.accountGroup?.isVisible = false
            binding.refresh?.isVisible = false
            setSectionVisible(R.id.musicLibrarySection, false)
            setSectionVisible(R.id.favoritesSection, false)
            setSectionVisible(R.id.playlistsSection, false)

            binding.toolbar.title = getString(R.string.music_server)
            binding.accountTitle?.text = getString(R.string.music_server)
            binding.accountSubtitle?.text = getString(R.string.music_server_login_subtitle)
            binding.accountSubtitle?.isVisible = true
            loadProfile()
            return
        }

        // === 已登录：按抽屉入口只显示对应一块 ===
        val tab = defaultTab
        binding.loginGroup?.isVisible = false
        binding.accountGroup?.isVisible = tab == "profile"
        binding.refresh?.isVisible = tab != "profile"
        binding.profileHeaderCard?.isVisible = tab == "profile"
        setSectionVisible(R.id.musicLibrarySection, tab == "music_library")
        setSectionVisible(R.id.favoritesSection, tab == "favorites")
        setSectionVisible(R.id.playlistsSection, tab == "playlists")

        val user = state.user
        binding.accountTitle?.text = user?.displayLabel ?: getString(R.string.music_server)
        binding.accountSubtitle?.text = getString(R.string.music_server_profile_subtitle)
        binding.musicLibrarySummary?.text = getString(R.string.music_server_library_summary, state.music.size)
        binding.favoritesSummary?.text = getString(R.string.music_server_favorites_summary, state.favorites.size)
        binding.playlistsSummary?.text = getString(R.string.music_server_playlists_summary, state.playlists.size)

        binding.toolbar.title = when (tab) {
            "playlists" -> getString(R.string.playlists)
            "favorites" -> getString(R.string.favorites)
            "music_library" -> getString(R.string.private_music)
            else -> getString(R.string.profile)
        }

        if (user != null) {
            binding.name.setText(user.displayLabel)
            userName = user.displayLabel
            // Prefer local blob (survives server responses that drop avatarUrl),
            // then remote URL, then default drawable — mirror loadProfile()'s priority.
            val blob = musicServerSession.avatarBlob
            when {
                blob != null && blob.isNotEmpty() -> {
                    Glide.with(this)
                        .load(blob)
                        .placeholder(R.drawable.ic_person_flat)
                        .error(R.drawable.ic_person_flat)
                        .into(binding.userImage)
                }
                !user.avatarUrl.isNullOrBlank() -> {
                    Glide.with(requireContext())
                        .load(user.avatarUrl)
                        .placeholder(R.drawable.ic_person_flat)
                        .error(R.drawable.ic_person_flat)
                        .into(binding.userImage)
                }
                else -> {
                    binding.userImage.setImageResource(R.drawable.ic_person_flat)
                }
            }
        } else {
            loadProfile()
        }

        renderMusicList(state)
        renderFavoriteList(state)
        renderPlaylistList(state)
    }

    private fun renderMusicList(state: MusicServerState) {
        binding.musicList?.removeAllViews()
        if (state.music.isEmpty()) {
            binding.musicList?.addView(emptyText(getString(R.string.music_server_empty_library)))
            return
        }
        state.music.forEach { music ->
            binding.musicList?.addView(
                musicRow(
                    music = music,
                    isFavorite = state.favorites.any { it.music.stableMusicId == music.stableMusicId },
                    showDelete = true,
                    cacheEntry = state.cacheEntryFor(music)
                )
            )
        }
    }

    private fun renderFavoriteList(state: MusicServerState) {
        binding.favoriteList?.removeAllViews()
        if (state.favorites.isEmpty()) {
            binding.favoriteList?.addView(emptyText(getString(R.string.music_server_empty_favorites)))
            return
        }
        state.favorites.forEach { favorite ->
            binding.favoriteList?.addView(
                musicRow(
                    music = favorite.music,
                    isFavorite = true,
                    showDelete = false,
                    cacheEntry = state.cacheEntryFor(favorite.music)
                )
            )
        }
    }

    private fun renderPlaylistList(state: MusicServerState) {
        binding.playlistList?.removeAllViews()
        if (state.playlists.isEmpty()) {
            binding.playlistList?.addView(emptyText(getString(R.string.music_server_empty_playlists)))
            return
        }
        val inflater = LayoutInflater.from(requireContext())
        state.playlists.forEach { playlist ->
            val row = inflater.inflate(R.layout.item_user_info_playlist, binding.playlistList, false)
            row.findViewById<MaterialTextView>(R.id.playlistTitle).text = playlist.name
            row.findViewById<MaterialTextView>(R.id.playlistSubtitle).text =
                "${playlist.tracks.size} 首"

            row.findViewById<MaterialButton>(R.id.playlistPlay).setOnClickListener {
                playSongs(playlist.tracks.filter { it.isPrivateMusic }.map { musicServerRepository.toSong(it) })
            }
            val overflowClick = View.OnClickListener { anchor ->
                showPlaylistOverflow(anchor, playlist)
            }
            row.findViewById<MaterialButton>(R.id.playlistOverflow).setOnClickListener(overflowClick)
            row.setOnClickListener { showPlaylistDetailsDialog(playlist) }
            binding.playlistList?.addView(row)
        }
    }

    private fun showPlaylistOverflow(anchor: View, playlist: MusicServerPlaylist) {
        val popup = PopupMenu(requireContext(), anchor)
        val menu = popup.menu
        val ids = mutableMapOf<Int, () -> Unit>()
        var next = 1
        fun add(titleRes: Int, action: () -> Unit) {
            menu.add(0, next, next, titleRes)
            ids[next] = action
            next++
        }
        add(R.string.action_details) { showPlaylistDetailsDialog(playlist) }
        add(R.string.action_edit) { showPlaylistEditorDialog(playlist) }
        menu.add(0, next, next, "Add current local").also { ids[next] = {
            val currentSong = MusicPlayerRemote.currentSong
            if (currentSong == Song.emptySong || MusicServerSongMapper.isRemoteSong(currentSong)) {
                showToast("请先播放一首本地歌曲")
            } else {
                runServerAction { musicServerRepository.addLocalTrackToPlaylist(playlist.id, currentSong) }
            }
        } }
        next++
        add(R.string.action_delete) {
            confirm("Delete ${playlist.name}?") {
                runServerAction { musicServerRepository.deletePlaylist(playlist.id) }
            }
        }
        popup.setOnMenuItemClickListener { item ->
            ids[item.itemId]?.invoke()
            true
        }
        popup.show()
    }

    private fun musicRow(
        music: MusicServerMusic,
        isFavorite: Boolean,
        showDelete: Boolean,
        cacheEntry: MusicServerCacheEntry?
    ): View {
        val ctx = requireContext()
        val row = LayoutInflater.from(ctx)
            .inflate(R.layout.item_user_info_track, binding.musicList, false)
        val song = musicServerRepository.toSong(music)

        val artView = row.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.trackArt)
        val cover = music.picUrl
        val glide = Glide.with(this)
        if (!cover.isNullOrBlank()) {
            glide.load(cover)
                .placeholder(R.drawable.default_audio_art)
                .error(R.drawable.default_audio_art)
                .into(artView)
        } else {
            glide.load(RetroGlideExtension.getSongModel(song))
                .simpleSongCoverOptions(song)
                .placeholder(R.drawable.default_audio_art)
                .error(R.drawable.default_audio_art)
                .into(artView)
        }

        row.findViewById<MaterialTextView>(R.id.trackTitle).text = music.title
        row.findViewById<MaterialTextView>(R.id.trackSubtitle).text =
            listOfNotNull(music.artist, music.album).joinToString(" · ")
                .ifBlank { getString(R.string.music_server) }

        val metaView = row.findViewById<MaterialTextView>(R.id.trackMeta)
        val metaText = cacheStatusText(music, cacheEntry)
        if (metaText.isBlank()) {
            metaView.isVisible = false
        } else {
            metaView.isVisible = true
            metaView.text = metaText
        }

        val favoriteBtn = row.findViewById<MaterialButton>(R.id.trackFavorite)
        favoriteBtn.setIconResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        favoriteBtn.iconTint =
            android.content.res.ColorStateList.valueOf(
                if (isFavorite) accentColor()
                else ctx.getColor(android.R.color.darker_gray)
            )
        favoriteBtn.setOnClickListener {
            runServerAction { musicServerRepository.toggleFavorite(music) }
        }

        val overflow = row.findViewById<MaterialButton>(R.id.trackOverflow)
        overflow.setOnClickListener { anchor ->
            showTrackOverflow(anchor, music, song, cacheEntry, showDelete)
        }

        row.setOnClickListener { playSongs(listOf(song)) }
        return row
    }

    private fun showTrackOverflow(
        anchor: View,
        music: MusicServerMusic,
        song: Song,
        cacheEntry: MusicServerCacheEntry?,
        showDelete: Boolean
    ) {
        val popup = PopupMenu(requireContext(), anchor)
        val menu = popup.menu
        val ids = mutableMapOf<Int, () -> Unit>()
        var next = 1

        fun add(titleRes: Int, action: () -> Unit) {
            menu.add(0, next, next, titleRes)
            ids[next] = action
            next++
        }
        fun add(title: String, action: () -> Unit) {
            menu.add(0, next, next, title)
            ids[next] = action
            next++
        }

        add(R.string.action_play) { playSongs(listOf(song)) }
        add(R.string.action_play_next) { MusicPlayerRemote.playNext(song) }
        add(R.string.action_add_to_playing_queue) { MusicPlayerRemote.enqueue(song) }
        add(R.string.action_add_to_playlist) { showAddToPlaylistDialog(music) }

        val supportsCache = music.playback?.supportsOfflineCache != false
        if (supportsCache) {
            when (cacheEntry?.state) {
                MusicServerCacheState.READY -> add(R.string.action_remove_cache) {
                    runServerAction { musicServerRepository.removeCachedMusic(music) }
                }
                MusicServerCacheState.DOWNLOADING,
                MusicServerCacheState.QUEUED -> { /* no action */ }
                MusicServerCacheState.FAILED,
                MusicServerCacheState.STALE,
                MusicServerCacheState.PAUSED,
                MusicServerCacheState.WAITING_FOR_WIFI,
                MusicServerCacheState.STORAGE_LOW -> add(R.string.action_retry_cache) {
                    runServerAction {
                        cacheEntry.let { musicServerRepository.retryCachedMusic(it.cacheKey) }
                        musicServerRepository.downloadCachedMusic(music)
                    }
                }
                null -> add(R.string.action_cache_offline) {
                    runServerAction { musicServerRepository.downloadCachedMusic(music) }
                }
            }
        }

        if (showDelete) {
            add(R.string.action_delete) {
                confirm("Delete ${music.title}?") {
                    val musicId = music.stableMusicId ?: return@confirm
                    runServerAction { musicServerRepository.deleteMusic(musicId) }
                }
            }
        }

        popup.setOnMenuItemClickListener { item: MenuItem ->
            ids[item.itemId]?.invoke()
            true
        }
        popup.show()
    }

    private fun MusicServerState.cacheEntryFor(music: MusicServerMusic): MusicServerCacheEntry? {
        val musicId = music.stableMusicId ?: return null
        return cacheEntries.values.firstOrNull {
            it.musicId == musicId && it.profileId == ORIGINAL_PROFILE_ID
        }
    }

    private fun cacheStatusText(music: MusicServerMusic, entry: MusicServerCacheEntry?): String {
        if (music.playback?.supportsOfflineCache == false) {
            return ""
        }
        return when (entry?.state) {
            null -> ""
            MusicServerCacheState.QUEUED -> getString(R.string.cache_status_queued)
            MusicServerCacheState.DOWNLOADING -> getString(R.string.cache_status_downloading)
            MusicServerCacheState.READY -> getString(R.string.cache_status_ready)
            MusicServerCacheState.FAILED -> getString(R.string.cache_status_failed)
            MusicServerCacheState.STALE -> getString(R.string.cache_status_stale)
            MusicServerCacheState.PAUSED -> getString(R.string.cache_status_paused)
            MusicServerCacheState.WAITING_FOR_WIFI -> getString(R.string.cache_status_waiting_for_wifi)
            MusicServerCacheState.STORAGE_LOW -> getString(R.string.cache_status_storage_low)
        }
    }

    private fun cacheActionButton(
        music: MusicServerMusic,
        entry: MusicServerCacheEntry?
    ): MaterialButton? {
        if (music.playback?.supportsOfflineCache == false) return null
        return when (entry?.state) {
            MusicServerCacheState.READY -> smallButton(R.string.action_remove_cache) {
                runServerAction { musicServerRepository.removeCachedMusic(music) }
            }
            MusicServerCacheState.DOWNLOADING,
            MusicServerCacheState.QUEUED -> smallButton(R.string.action_cache_offline) {}.apply {
                isEnabled = false
            }
            MusicServerCacheState.FAILED,
            MusicServerCacheState.STALE,
            MusicServerCacheState.PAUSED,
            MusicServerCacheState.WAITING_FOR_WIFI,
            MusicServerCacheState.STORAGE_LOW -> smallButton(R.string.action_retry_cache) {
                runServerAction {
                    entry?.let { musicServerRepository.retryCachedMusic(it.cacheKey) }
                    musicServerRepository.downloadCachedMusic(music)
                }
            }
            null -> smallButton(R.string.action_cache_offline) {
                runServerAction { musicServerRepository.downloadCachedMusic(music) }
            }
        }
    }

    private fun showAddToPlaylistDialog(music: MusicServerMusic) {
        val playlists = lastState.playlists
        if (playlists.isEmpty()) {
            showToast("请先创建一个歌单")
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_add_to_playlist)
            .setItems(playlists.map { it.name }.toTypedArray()) { _, which ->
                runServerAction {
                    musicServerRepository.addPrivateTrackToPlaylist(playlists[which].id, music)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showPlaylistDetailsDialog(playlist: MusicServerPlaylist) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dip(16))
        }
        if (playlist.tracks.isEmpty()) {
            container.addView(emptyText("No tracks"))
        } else {
            playlist.tracks.forEach { track ->
                val row = rowContainer()
                row.addView(titleText(track.title))
                row.addView(subtitleText(listOfNotNull(track.artist, track.album, track.source).joinToString(" · ")))
                val remove = smallButton(R.string.action_remove_from_playlist) {
                    val trackId = track.trackId ?: return@smallButton
                    runServerAction { musicServerRepository.removePlaylistTrack(playlist.id, trackId) }
                }
                if (track.isPrivateMusic) {
                    val play = smallButton(R.string.action_play) {
                        playSongs(listOf(musicServerRepository.toSong(track)))
                    }
                    row.addView(buttonRow(play, remove))
                } else {
                    row.addView(buttonRow(remove))
                }
                container.addView(row)
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(playlist.name)
            .setView(container)
            .setPositiveButton(R.string.done, null)
            .show()
    }

    private fun showPlaylistEditorDialog(playlist: MusicServerPlaylist? = null) {
        val name = dialogInput(getString(R.string.playlists), playlist?.name.orEmpty())
        val description = dialogInput(getString(R.string.description), playlist?.description.orEmpty())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dip(16))
            addView(name.first)
            addView(description.first)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (playlist == null) R.string.create_action else R.string.action_edit)
            .setView(container)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.done) { _, _ ->
                val playlistName = name.second.text.toString().trim()
                val playlistDescription = description.second.text.toString().trim().takeIf { it.isNotBlank() }
                if (playlistName.isBlank()) {
                    showToast("Playlist name is required")
                } else {
                    runServerAction {
                        if (playlist == null) {
                            musicServerRepository.createPlaylist(playlistName, playlistDescription)
                        } else {
                            musicServerRepository.updatePlaylist(playlist.id, playlistName, playlistDescription)
                        }
                    }
                }
            }
            .show()
    }

    private fun showUploadMusicDialog(uri: Uri) {
        val title = dialogInput("Title", uri.displayNameWithoutExtension())
        val artist = dialogInput(getString(R.string.artist), "")
        val album = dialogInput(getString(R.string.album), "")
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dip(16))
            addView(title.first)
            addView(artist.first)
            addView(album.first)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.upload_music)
            .setView(container)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.done) { _, _ ->
                runServerAction {
                    val file = copyUriToCache(uri)
                    musicServerRepository.uploadMusic(
                        file = file,
                        contentType = requireContext().contentResolver.getType(uri),
                        title = title.second.text.toString(),
                        artist = artist.second.text.toString(),
                        album = album.second.text.toString()
                    )
                    file.delete()
                }
            }
            .show()
    }

    private fun uploadAvatar(uri: Uri) {
        val compressed = try {
            compressAvatar(uri)
        } catch (t: Throwable) {
            Log.w("UserInfoFragment", "compressAvatar failed for $uri", t)
            null
        }
        if (compressed == null) {
            showToast("头像处理失败，请换一张图片")
            return
        }
        // Persist the tiny blob locally so avatar survives without profile.jpg on disk
        musicServerSession.avatarBlob = compressed
        // Refresh UI right away
        loadProfile()
        showToast("头像已更新")
        // Still try to sync to the server (best effort)
        runServerAction(showErrors = false) {
            val tmp = File.createTempFile("avatar_", ".jpg", requireContext().cacheDir)
            tmp.writeBytes(compressed)
            try {
                musicServerRepository.uploadAvatar(tmp, "image/jpeg")
            } finally {
                tmp.delete()
            }
        }
    }

    /**
     * Downscale the picked image to a small square JPEG (max 256px, ~40-80KB) so we
     * can safely stash it inside SharedPreferences as a base64 blob.
     */
    private fun compressAvatar(uri: Uri): ByteArray? {
        val ctx = requireContext()

        // Prefer ImageDecoder on API 28+ — handles HEIC/WEBP/animated + does downsampling for us.
        val decoded: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(ctx.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                    val w = info.size.width
                    val h = info.size.height
                    if (w > 0 && h > 0) {
                        val target = MAX_AVATAR_SIDE * 2
                        val long = maxOf(w, h)
                        if (long > target) {
                            val scale = target.toFloat() / long
                            decoder.setTargetSize(
                                (w * scale).toInt().coerceAtLeast(1),
                                (h * scale).toInt().coerceAtLeast(1)
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w("UserInfoFragment", "ImageDecoder failed, falling back to BitmapFactory", t)
                null
            }
        } else null

        val bitmap = decoded ?: run {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return null
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return null

            var sample = 1
            val target = MAX_AVATAR_SIDE * 2
            while (w / (sample * 2) >= target && h / (sample * 2) >= target) sample *= 2

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null
        }

        // Center-crop to square, then scale to MAX_SIDE
        val side = minOf(bitmap.width, bitmap.height)
        if (side <= 0) {
            bitmap.recycle()
            return null
        }
        val cropX = (bitmap.width - side) / 2
        val cropY = (bitmap.height - side) / 2
        val square = Bitmap.createBitmap(bitmap, cropX, cropY, side, side)
        if (square !== bitmap) bitmap.recycle()
        val scaled = if (square.width > MAX_AVATAR_SIDE) {
            Bitmap.createScaledBitmap(square, MAX_AVATAR_SIDE, MAX_AVATAR_SIDE, true).also {
                if (it !== square) square.recycle()
            }
        } else square

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, out)
        scaled.recycle()
        return out.toByteArray()
    }

    private companion object {
        const val MAX_AVATAR_SIDE = 256
        const val AVATAR_JPEG_QUALITY = 82
        const val ORIGINAL_PROFILE_ID = "original"
    }

    private fun showBannerImageOptions() {
        val list = requireContext().resources.getStringArray(R.array.image_settings_options)
        MaterialAlertDialogBuilder(requireContext()).setTitle("Banner Image")
            .setItems(list) { _, which ->
                when (which) {
                    0 -> pickBannerImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    1 -> {
                        File(requireContext().filesDir, USER_BANNER).delete()
                        loadProfile()
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showLocalUserImageOptions() {
        val list = requireContext().resources.getStringArray(R.array.image_settings_options)
        MaterialAlertDialogBuilder(requireContext()).setTitle("Profile Image")
            .setItems(list) { _, which ->
                when (which) {
                    0 -> pickLocalUserImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    1 -> {
                        File(requireContext().filesDir, USER_PROFILE).delete()
                        loadProfile()
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun setAndSaveBannerImage(fileUri: Uri) {
        setAndSaveLocalImage(fileUri, USER_BANNER)
    }

    private fun setAndSaveUserImage(fileUri: Uri) {
        setAndSaveLocalImage(fileUri, USER_PROFILE)
    }

    private fun setAndSaveLocalImage(fileUri: Uri, fileName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = Glide.with(this@UserInfoFragment)
                .asBitmap()
                .load(fileUri)
                .submit()
                .get()
            val file = File(requireContext().filesDir, fileName)
            file.outputStream().buffered().use {
                ImageUtil.resizeBitmap(bitmap, 2048).compress(android.graphics.Bitmap.CompressFormat.WEBP, 100, it)
            }
            withContext(Dispatchers.Main) {
                loadProfile()
                showToast(R.string.message_updated)
            }
        }
    }

    private fun loadProfile() {
        val bannerFile = RetroGlideExtension.getBannerModel()
        if (bannerFile.exists() && bannerFile.length() > 0) {
            Glide.with(this)
                .load(bannerFile)
                .profileBannerOptions(bannerFile)
                .into(binding.bannerImage)
        } else {
            binding.bannerImage.setImageResource(R.drawable.material_design_default)
        }

        val blob = musicServerSession.avatarBlob
        if (blob != null && blob.isNotEmpty()) {
            Glide.with(this)
                .load(blob)
                .placeholder(R.drawable.ic_person_flat)
                .error(R.drawable.ic_person_flat)
                .into(binding.userImage)
            return
        }
        val avatarUrl = musicServerSession.user?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_person_flat)
                .error(R.drawable.ic_person_flat)
                .into(binding.userImage)
        } else {
            binding.userImage.setImageResource(R.drawable.ic_person_flat)
        }
    }

    private fun playSongs(songs: List<Song>) {
        if (songs.isEmpty()) {
            showToast("暂无可播放的私人音乐")
            return
        }
        MusicPlayerRemote.openQueue(songs, 0, true)
    }

    private fun runServerAction(
        showErrors: Boolean = true,
        onError: (Throwable) -> Unit = {},
        action: suspend () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            _binding?.refresh?.isEnabled = false
            try {
                withContext(Dispatchers.IO) { action() }
            } catch (error: Throwable) {
                if (showErrors && isAdded) {
                    val ctx = context
                    if (ctx != null) {
                        val message = error.readableMessage().ifBlank {
                            ctx.getString(R.string.error_load_failed)
                        }
                        showToast(message)
                    }
                }
                onError(error)
            } finally {
                _binding?.refresh?.isEnabled = true
            }
        }
    }

    private fun rowContainer() = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dip(16), dip(14), dip(16), dip(14))
        foreground = requireContext().obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            .let {
                val drawable = it.getDrawable(0)
                it.recycle()
                drawable
            }
    }

    private fun buttonRow(vararg buttons: MaterialButton) = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        buttons.forEachIndexed { index, button ->
            button.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (index > 0) marginStart = dip(8)
            }
            addView(button)
        }
    }

    private fun titleText(text: String) = MaterialTextView(requireContext()).apply {
        this.text = text
        setTextAppearance(R.style.TextViewSubtitle1)
    }

    private fun subtitleText(text: String) = MaterialTextView(requireContext()).apply {
        this.text = text
        setTextAppearance(R.style.TextViewBody2)
        setTextColor(requireContext().getColor(android.R.color.darker_gray))
    }

    private fun emptyText(text: String) = subtitleText(text).apply {
        gravity = android.view.Gravity.CENTER
        setPadding(dip(24), dip(32), dip(24), dip(32))
    }

    private fun smallButton(textRes: Int, onClick: () -> Unit) = MaterialButton(requireContext()).apply {
        setText(textRes)
        setOnClickListener { onClick() }
    }

    private fun smallTextButton(text: String, onClick: () -> Unit) = MaterialButton(requireContext()).apply {
        this.text = text
        setOnClickListener { onClick() }
    }

    private fun dialogInput(hint: String, value: String): Pair<TextInputLayout, TextInputEditText> {
        val editText = TextInputEditText(requireContext()).apply {
            setText(value)
        }
        val layout = TextInputLayout(requireContext()).apply {
            this.hint = hint
            addView(editText)
        }
        return layout to editText
    }

    private fun confirm(message: String, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> action() }
            .show()
    }

    private fun Uri.displayNameWithoutExtension(): String {
        return displayName().substringBeforeLast('.', displayName())
    }

    private fun Uri.displayName(): String {
        requireContext().contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return lastPathSegment ?: "music"
    }

    private fun copyUriToCache(uri: Uri): File {
        val file = File(requireContext().cacheDir, "music-server-${System.currentTimeMillis()}-${uri.displayName()}")
        requireContext().contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open file" }
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun setSectionVisible(sectionId: Int, visible: Boolean) {
        binding.root.findViewById<View>(sectionId)?.isVisible = visible
    }

    /**
     * 登录成功回到首页：pop 掉 UserInfo 这一层，让用户回到 [HomeFragment]。
     * 用 `popBackStack(R.id.action_home, inclusive=false)`，即使中间还夹着其他页也能一路清回首页。
     */
    private fun popToHome() {
        val nav = findNavController()
        // 若栈里没有 home（极少见），退化成 navigateUp
        val popped = nav.popBackStack(R.id.action_home, /* inclusive = */ false)
        if (!popped) {
            nav.navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

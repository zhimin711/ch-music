package code.name.monkey.retromusic.fragments.other

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
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
import code.name.monkey.retromusic.glide.RetroGlideExtension.userProfileOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.musicserver.MusicServerCacheEntry
import code.name.monkey.retromusic.musicserver.MusicServerCacheState
import code.name.monkey.retromusic.musicserver.MusicServerDefaults
import code.name.monkey.retromusic.musicserver.MusicServerMusic
import code.name.monkey.retromusic.musicserver.MusicServerPlaylist
import code.name.monkey.retromusic.musicserver.MusicServerRepository
import code.name.monkey.retromusic.musicserver.MusicServerSongMapper
import code.name.monkey.retromusic.musicserver.MusicServerState
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
import java.io.File

class UserInfoFragment : Fragment() {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!
    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val musicServerRepository: MusicServerRepository by inject()
    private var registerMode = false
    private var lastState = MusicServerState()

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

    private fun setupStaticUi() {
        binding.accountSubtitle?.text = MusicServerDefaults.baseUrl
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
        binding.uploadAvatar?.setOnClickListener {
            pickAvatarLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
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
        runServerAction {
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
        binding.loginGroup?.isVisible = !state.isLoggedIn
        binding.accountGroup?.isVisible = state.isLoggedIn
        binding.refresh?.isVisible = state.isLoggedIn

        val user = state.user
        binding.accountTitle?.text = user?.displayLabel ?: getString(R.string.music_server)
        binding.accountSubtitle?.text = if (user == null) {
            MusicServerDefaults.baseUrl
        } else {
            "${user.username} · ${MusicServerDefaults.baseUrl}"
        }

        if (user != null) {
            binding.name.setText(user.displayLabel)
            userName = user.displayLabel
            Glide.with(requireContext())
                .load(user.avatarUrl ?: RetroGlideExtension.getUserModel())
                .userProfileOptions(RetroGlideExtension.getUserModel(), requireContext())
                .into(binding.userImage)
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
            binding.musicList?.addView(emptyText("No private music"))
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
            binding.favoriteList?.addView(emptyText("No favorites"))
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
            binding.playlistList?.addView(emptyText("No playlists"))
            return
        }
        state.playlists.forEach { playlist ->
            val row = rowContainer()
            val title = titleText(playlist.name)
            val subtitle = subtitleText("${playlist.tracks.size} songs")
            val play = smallButton(R.string.action_play) {
                playSongs(playlist.tracks.filter { it.isPrivateMusic }.map { musicServerRepository.toSong(it) })
            }
            val details = smallButton(R.string.action_details) {
                showPlaylistDetailsDialog(playlist)
            }
            val edit = smallButton(R.string.action_edit) {
                showPlaylistEditorDialog(playlist)
            }
            val delete = smallButton(R.string.action_delete) {
                confirm("Delete ${playlist.name}?") {
                    runServerAction { musicServerRepository.deletePlaylist(playlist.id) }
                }
            }
            val addLocal = smallTextButton("Add current local") {
                val currentSong = MusicPlayerRemote.currentSong
                if (currentSong == Song.emptySong || MusicServerSongMapper.isRemoteSong(currentSong)) {
                    showToast("Play a local song first")
                } else {
                    runServerAction { musicServerRepository.addLocalTrackToPlaylist(playlist.id, currentSong) }
                }
            }
            row.addView(title)
            row.addView(subtitle)
            row.addView(buttonRow(play, details, edit, delete))
            row.addView(buttonRow(addLocal))
            binding.playlistList?.addView(row)
        }
    }

    private fun musicRow(
        music: MusicServerMusic,
        isFavorite: Boolean,
        showDelete: Boolean,
        cacheEntry: MusicServerCacheEntry?
    ): View {
        val row = rowContainer()
        val song = musicServerRepository.toSong(music)
        row.addView(titleText(music.title))
        row.addView(subtitleText(listOfNotNull(music.artist, music.album).joinToString(" · ").ifBlank {
            getString(R.string.music_server)
        }))
        row.addView(subtitleText(cacheStatusText(music, cacheEntry)))
        val play = smallButton(R.string.action_play) { playSongs(listOf(song)) }
        val next = smallButton(R.string.action_play_next) { MusicPlayerRemote.playNext(song) }
        val queue = smallButton(R.string.action_add_to_playing_queue) { MusicPlayerRemote.enqueue(song) }
        val favorite = smallTextButton(
            getString(if (isFavorite) R.string.action_remove_from_favorites else R.string.action_add_to_favorites)
        ) {
            runServerAction { musicServerRepository.toggleFavorite(music) }
        }
        row.addView(buttonRow(play, next, queue, favorite))

        val secondaryButtons = mutableListOf<MaterialButton>()
        secondaryButtons.add(smallTextButton(getString(R.string.action_add_to_playlist)) {
            showAddToPlaylistDialog(music)
        })
        cacheActionButton(music, cacheEntry)?.let { secondaryButtons.add(it) }
        if (showDelete) {
            secondaryButtons.add(smallButton(R.string.action_delete) {
                confirm("Delete ${music.title}?") {
                    val musicId = music.stableMusicId ?: return@confirm
                    runServerAction { musicServerRepository.deleteMusic(musicId) }
                }
            })
        }
        row.addView(buttonRow(*secondaryButtons.toTypedArray()))
        return row
    }

    private fun MusicServerState.cacheEntryFor(music: MusicServerMusic): MusicServerCacheEntry? {
        val musicId = music.stableMusicId ?: return null
        return cacheEntries.values.firstOrNull {
            it.musicId == musicId && it.profileId == ORIGINAL_PROFILE_ID
        }
    }

    private fun cacheStatusText(music: MusicServerMusic, entry: MusicServerCacheEntry?): String {
        if (music.playback?.supportsOfflineCache == false) {
            return getString(R.string.cache_status_unavailable)
        }
        return when (entry?.state) {
            null -> getString(R.string.cache_status_not_cached)
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
            showToast("Create a playlist first")
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
        runServerAction {
            val file = copyUriToCache(uri)
            musicServerRepository.uploadAvatar(file, requireContext().contentResolver.getType(uri))
            file.delete()
        }
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
        Glide.with(this)
            .load(RetroGlideExtension.getBannerModel())
            .profileBannerOptions(RetroGlideExtension.getBannerModel())
            .into(binding.bannerImage)
        Glide.with(this)
            .load(RetroGlideExtension.getUserModel())
            .userProfileOptions(RetroGlideExtension.getUserModel(), requireContext())
            .into(binding.userImage)
    }

    private fun playSongs(songs: List<Song>) {
        if (songs.isEmpty()) {
            showToast("No playable private tracks")
            return
        }
        MusicPlayerRemote.openQueue(songs, 0, true)
    }

    private fun runServerAction(showErrors: Boolean = true, action: suspend () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.refresh?.isEnabled = false
            try {
                withContext(Dispatchers.IO) { action() }
            } catch (error: Throwable) {
                if (showErrors) {
                    showToast(error.message ?: getString(R.string.error_load_failed))
                }
            } finally {
                binding.refresh?.isEnabled = true
            }
        }
    }

    private fun rowContainer() = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dip(12), 0, dip(12))
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
        setPadding(0, dip(8), 0, dip(8))
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

    private companion object {
        const val ORIGINAL_PROFILE_ID = "original"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

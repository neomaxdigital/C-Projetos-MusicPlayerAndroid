package com.musicplayer.offline

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.musicplayer.offline.data.UserLibraryRepository
import com.musicplayer.offline.data.PlaylistRepository
import com.musicplayer.offline.data.AppSettingsRepository
import com.musicplayer.offline.data.PlaybackSessionRepository
import com.musicplayer.offline.data.PlaybackSnapshot
import com.musicplayer.offline.conversion.AudioConversionManager
import com.musicplayer.offline.music.AudioFileSupport
import com.musicplayer.offline.music.SafMusicRepository
import com.musicplayer.offline.music.SafSongImportResult
import com.musicplayer.offline.music.Song
import com.musicplayer.offline.music.SongSort
import com.musicplayer.offline.music.asAlbums
import com.musicplayer.offline.music.asArtists
import com.musicplayer.offline.music.asGenres
import com.musicplayer.offline.music.sortedByOption
import com.musicplayer.offline.playback.PlaybackService
import com.musicplayer.offline.ui.AlbumDetailScreen
import com.musicplayer.offline.ui.AppBackground
import com.musicplayer.offline.ui.AppBottomBar
import com.musicplayer.offline.ui.ArtistDetailScreen
import com.musicplayer.offline.ui.CollectionScreen
import com.musicplayer.offline.ui.ConversionDialogs
import com.musicplayer.offline.ui.ConversionRequest
import com.musicplayer.offline.ui.Destination
import com.musicplayer.offline.ui.FeatureRoute
import com.musicplayer.offline.ui.EqualizerScreen
import com.musicplayer.offline.ui.SleepTimerScreen
import com.musicplayer.offline.ui.LyricsScreen
import com.musicplayer.offline.ui.SettingsScreen
import com.musicplayer.offline.ui.EmptyLibraryPage
import com.musicplayer.offline.ui.FolderBrowserScreen
import com.musicplayer.offline.ui.GenreDetailScreen
import com.musicplayer.offline.ui.GenresScreen
import com.musicplayer.offline.ui.LibraryRootScreen
import com.musicplayer.offline.ui.LibraryRoute
import com.musicplayer.offline.ui.LibraryTab
import com.musicplayer.offline.ui.LocalConversionRequest
import com.musicplayer.offline.ui.JukeCircularLogo
import com.musicplayer.offline.ui.JukeIntroScreen
import com.musicplayer.offline.ui.MiniPlayer
import com.musicplayer.offline.ui.MusicPlayerTheme
import com.musicplayer.offline.ui.NowPlayingScreen
import com.musicplayer.offline.ui.PlaylistDetailScreen
import com.musicplayer.offline.ui.PlaylistPickerDialog
import com.musicplayer.offline.ui.PlaylistsScreen
import com.musicplayer.offline.ui.SmartPlaylist
import com.musicplayer.offline.ui.SmartPlaylistDetailScreen
import com.musicplayer.offline.ui.PrimaryBlue
import com.musicplayer.offline.ui.SearchScreen
import com.musicplayer.offline.ui.QueueScreen
import com.musicplayer.offline.ui.TextMuted
import com.musicplayer.offline.ui.playSong
import com.musicplayer.offline.ui.playNext
import com.musicplayer.offline.ui.addToQueue
import com.musicplayer.offline.ui.toMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MusicPlayerViewModel> {
        MusicPlayerViewModelFactory(
            UserLibraryRepository(applicationContext),
            PlaylistRepository(applicationContext),
            AppSettingsRepository(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MusicPlayerApp(viewModel) }
    }
}

@Composable
private fun MusicPlayerApp(viewModel: MusicPlayerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state = viewModel.state
    var player by remember { mutableStateOf<Player?>(null) }
    var currentSongId by remember { mutableStateOf<Long?>(null) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var nowPlaying by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    var queueRevision by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf(Destination.HOME) }
    var route by remember { mutableStateOf<LibraryRoute>(LibraryRoute.Root()) }
    var searching by remember { mutableStateOf(false) }
    var feature by remember { mutableStateOf<FeatureRoute?>(null) }
    var rescanRevision by remember { mutableIntStateOf(0) }
    var sessionRestored by remember { mutableStateOf(false) }
    var playbackMessage by remember { mutableStateOf<String?>(null) }
    var conversionRequest by remember { mutableStateOf<ConversionRequest?>(null) }
    var nowPlayingPlaylistSong by remember { mutableStateOf<Song?>(null) }
    val conversionState by AudioConversionManager.state.collectAsStateWithLifecycle()
    val sessionRepository = remember { PlaybackSessionRepository(context) }
    val safMusicRepository = remember(context) { SafMusicRepository(context) }
    val importScope = rememberCoroutineScope()
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            safMusicRepository.addFolder(it)
            playbackMessage = "Pasta adicionada à biblioteca"
            rescanRevision++
        }
    }
    val songLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            importScope.launch {
                when (val result = withContext(Dispatchers.IO) { safMusicRepository.addSong(it) }) {
                    is SafSongImportResult.Imported -> {
                        val addedToUi = viewModel.addSongImmediately(result.song)
                        playbackMessage = when {
                            addedToUi -> "Música adicionada à biblioteca"
                            result.newlyPersisted -> "A música já está na biblioteca"
                            else -> "Esta música já foi adicionada à biblioteca"
                        }
                        rescanRevision++
                    }
                    SafSongImportResult.Unsupported -> playbackMessage = "O arquivo selecionado não é um áudio compatível."
                    SafSongImportResult.Failed -> playbackMessage = "Não foi possível adicionar a música selecionada."
                }
            }
        }
    }

    LaunchedEffect(rescanRevision, state.settings.showShortSongs, state.settings.minimumDurationSeconds) {
        viewModel.beginLibraryLoad()
        runCatching {
            withContext(Dispatchers.IO) {
                safMusicRepository.loadSongs().filter {
                    state.settings.showShortSongs || it.duration >= state.settings.minimumDurationSeconds * 1_000L
                }
            }
        }.onSuccess(viewModel::setSongs).onFailure {
            viewModel.setLibraryError("Não foi possível atualizar as músicas e pastas adicionadas.")
        }
    }
    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }
                .onSuccess { player = it }
                .onFailure { playbackMessage = "Não foi possível conectar ao serviço de reprodução." }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            if (future.isDone && !future.isCancelled) runCatching {
                val controller = future.get()
                if (controller.mediaItemCount > 0) sessionRepository.save(controller.toSnapshot())
                controller.release()
            } else future.cancel(true)
            player = null
        }
    }
    LaunchedEffect(player) {
        while (player != null) {
            val activePlayer = player
            currentSongId = activePlayer?.currentMediaItem?.mediaId?.toLongOrNull()
            position = activePlayer?.currentPosition ?: 0L
            duration = (activePlayer?.duration ?: 0L).coerceAtLeast(0L)
            playing = activePlayer?.isPlaying == true
            shuffleEnabled = activePlayer?.shuffleModeEnabled == true
            repeatMode = activePlayer?.repeatMode ?: Player.REPEAT_MODE_OFF
            delay(350)
        }
    }
    LaunchedEffect(player, state.songs, state.settings.resumeLastTrack) {
        val activePlayer = player ?: return@LaunchedEffect
        if (sessionRestored || state.songs.isEmpty()) return@LaunchedEffect
        sessionRestored = true
        if (activePlayer.mediaItemCount > 0 || !state.settings.resumeLastTrack) return@LaunchedEffect
        val snapshot = withContext(Dispatchers.IO) { sessionRepository.load() } ?: return@LaunchedEffect
        val songsById = state.songs.associateBy { it.id }
        val restoredQueue = snapshot.queueIds.mapNotNull(songsById::get)
        if (restoredQueue.isEmpty()) return@LaunchedEffect
        val intendedId = snapshot.queueIds.getOrNull(snapshot.currentIndex)
        val index = restoredQueue.indexOfFirst { it.id == intendedId }.takeIf { it >= 0 }
            ?: snapshot.currentIndex.coerceIn(0, restoredQueue.lastIndex)
        activePlayer.setMediaItems(restoredQueue.map { it.toMediaItem() }, index, if (state.settings.resumePosition) snapshot.positionMs else 0L)
        if (state.settings.restoreShuffle) activePlayer.shuffleModeEnabled = snapshot.shuffleEnabled
        if (state.settings.restoreRepeat) activePlayer.repeatMode = snapshot.repeatMode
        activePlayer.prepare()
        if (state.settings.autoResume && snapshot.wasPlaying) activePlayer.play()
    }
    LaunchedEffect(player, sessionRestored) {
        var lastSaved: PlaybackSnapshot? = null
        while (player != null) {
            delay(2_000)
            player?.takeIf { it.mediaItemCount > 0 }?.let { active ->
                val snapshot = active.toSnapshot()
                if (snapshot != lastSaved) {
                    withContext(Dispatchers.IO) { sessionRepository.save(snapshot) }
                    lastSaved = snapshot
                }
            }
        }
    }
    DisposableEffect(player) {
        val activePlayer = player
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (isPlaying) activePlayer?.currentMediaItem?.mediaId?.toLongOrNull()?.let(viewModel::recordRecent)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleEnabled = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(newRepeatMode: Int) {
                repeatMode = newRepeatMode
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSongId = mediaItem?.mediaId?.toLongOrNull()
                if (activePlayer?.isPlaying == true) currentSongId?.let(viewModel::recordRecent)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                queueRevision++
            }

            override fun onPlayerError(error: PlaybackException) {
                val failed = activePlayer ?: return
                val failedId = failed.currentMediaItem?.mediaId?.toLongOrNull()
                val shouldContinue = failed.playWhenReady
                val failedIndex = failed.currentMediaItemIndex
                if (failedIndex >= 0 && failedIndex < failed.mediaItemCount) failed.removeMediaItem(failedIndex)
                val unavailable = error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION
                val unsupportedCodec = AudioFileSupport.isUnsupportedCodecError(error.errorCode)
                if (unavailable) failedId?.let(viewModel::removeUnavailableSong)
                playbackMessage = when {
                    unavailable -> "A faixa foi removida do dispositivo e saiu da fila."
                    unsupportedCodec -> "O formato ou codec deste arquivo não é suportado; o player avançou para a próxima faixa."
                    else -> "Não foi possível reproduzir esta faixa; o player avançou para a próxima."
                }
                if (failed.mediaItemCount > 0) {
                    failed.prepare()
                    if (shouldContinue) failed.play()
                }
            }
        }
        activePlayer?.addListener(listener)
        onDispose { activePlayer?.removeListener(listener) }
    }
    val currentSong = remember(state.songs, currentSongId) { state.songs.find { it.id == currentSongId } }
    BackHandler(enabled = queueOpen) { queueOpen = false }
    BackHandler(enabled = feature != null) { feature = null }
    BackHandler(enabled = nowPlaying && !queueOpen && feature == null) { nowPlaying = false }

    MusicPlayerTheme(state.settings) {
        Surface(Modifier.fillMaxSize(), color = AppBackground) {
            Box(Modifier.fillMaxSize()) {
              when {
                !viewModel.introShown -> JukeIntroScreen(viewModel::dismissIntro)
                state.isLoading && state.songs.isEmpty() -> LibraryLoadingScreen()
                state.loadError != null && state.songs.isEmpty() -> LibraryErrorScreen(state.loadError) { rescanRevision++ }
                feature == FeatureRoute.EQUALIZER -> EqualizerScreen { feature = null }
                feature == FeatureRoute.SLEEP_TIMER -> SleepTimerScreen { feature = null }
                feature == FeatureRoute.LYRICS -> LyricsScreen(currentSong, position, state.settings.animationsEnabled) { feature = null }
                feature == FeatureRoute.SETTINGS -> SettingsScreen(
                    state.settings, viewModel::updateSettings, { rescanRevision++ }, { feature = null }
                )
                queueOpen -> QueueScreen(player, state.songs, queueRevision) { queueOpen = false }
                nowPlaying -> NowPlayingScreen(
                    song = currentSong,
                    player = player,
                    position = position,
                    duration = duration,
                    playing = playing,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    favorite = currentSongId in state.favoriteIds,
                    toggleFavorite = { currentSongId?.let(viewModel::toggleFavorite) },
                    openQueue = { queueOpen = true },
                    addToQueue = {
                        currentSong?.let { song ->
                            addToQueue(player, song)
                            playbackMessage = "Adicionada à fila de reprodução"
                        }
                    },
                    addToPlaylist = {
                        currentSong?.let { nowPlayingPlaylistSong = it }
                    },
                    openEqualizer = { feature = FeatureRoute.EQUALIZER },
                    openLyrics = { feature = FeatureRoute.LYRICS },
                    requestConversion = { song, shareAfter -> conversionRequest = ConversionRequest(song, shareAfter) },
                    back = { nowPlaying = false }
                )
                else -> HomeShell(
                    state = state,
                    player = player,
                    currentSong = currentSong,
                    playing = playing,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    destination = destination,
                    onDestination = { destination = it },
                    route = route,
                    onRoute = { route = it },
                    searching = searching,
                    onSearching = { searching = it },
                    openQueue = { queueOpen = true },
                    openFeature = { feature = it },
                    onSort = viewModel::setSort,
                    onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                    onCreatePlaylist = viewModel::createPlaylist,
                    onCreatePlaylistWithSong = viewModel::createPlaylistWithSong,
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onAddSongToPlaylist = viewModel::addSongToPlaylist,
                    onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                    onMovePlaylistSong = viewModel::movePlaylistSong,
                    onPlaySong = { song, queue ->
                        currentSongId = song.id
                        viewModel.recordPlay(song.id)
                        playSong(player, queue, song)
                    },
                    onConvertSong = { song, shareAfter -> conversionRequest = ConversionRequest(song, shareAfter) },
                    onAddFolder = { folderLauncher.launch(null) },
                    onAddSong = { songLauncher.launch(arrayOf("audio/*")) },
                    onRefreshLibrary = {
                        playbackMessage = "Biblioteca atualizada"
                        rescanRevision++
                    },
                    openNowPlaying = { if (currentSongId != null) nowPlaying = true }
                )
              }
              playbackMessage?.let { message ->
                  Snackbar(
                      modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 110.dp),
                      action = { TextButton({ playbackMessage = null }) { Text("Fechar") } }
                  ) { Text(message) }
              }
              ConversionDialogs(
                  request = conversionRequest,
                  state = conversionState,
                  onDismissRequest = { conversionRequest = null },
                  onConfirm = { request ->
                      conversionRequest = null
                      AudioConversionManager.start(context, request.song, request.shareAfter)
                  },
                  onCancel = AudioConversionManager::cancel,
                  onDismissState = AudioConversionManager::dismiss,
                  onCompleted = { uri ->
                      importScope.launch {
                          withContext(Dispatchers.IO) { safMusicRepository.addSong(uri) }
                          rescanRevision++
                      }
                  }
              )
              nowPlayingPlaylistSong?.let { song ->
                  PlaylistPickerDialog(
                      song = song,
                      playlists = state.playlists,
                      onDismiss = { nowPlayingPlaylistSong = null },
                      onAdd = { playlistId ->
                          viewModel.addSongToPlaylist(playlistId, song.id)
                          nowPlayingPlaylistSong = null
                          playbackMessage = "Adicionada à playlist"
                      },
                      onCreateAndAdd = { name ->
                          viewModel.createPlaylistWithSong(name, song.id)
                          nowPlayingPlaylistSong = null
                          playbackMessage = "Playlist criada e música adicionada"
                      }
                  )
              }
            }
        }
    }
}

@Composable
private fun HomeShell(
    state: LibraryUiState,
    player: Player?,
    currentSong: Song?,
    playing: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    destination: Destination,
    onDestination: (Destination) -> Unit,
    route: LibraryRoute,
    onRoute: (LibraryRoute) -> Unit,
    searching: Boolean,
    onSearching: (Boolean) -> Unit,
    openQueue: () -> Unit,
    openFeature: (FeatureRoute) -> Unit,
    onSort: (SongSort) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistWithSong: (String, Long) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (String, Long) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onMovePlaylistSong: (String, Int, Int) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onConvertSong: (Song, Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onAddSong: () -> Unit,
    onRefreshLibrary: () -> Unit,
    openNowPlaying: () -> Unit
) {
    val albums = remember(state.songs) { state.songs.asAlbums() }
    val artists = remember(state.songs) { state.songs.asArtists() }
    val genres = remember(state.songs) { state.songs.asGenres() }
    val songsById = remember(state.songs) { state.songs.associateBy { it.id } }
    val sortedSongs = remember(state.songs, state.sort) { state.songs.sortedByOption(state.sort) }
    val favoriteSongs = remember(songsById, state.favoriteIds) { state.favoriteIds.mapNotNull(songsById::get) }
    val recentSongs = remember(songsById, state.recentIds) { state.recentIds.mapNotNull(songsById::get) }
    var playlistSong by remember { mutableStateOf<Song?>(null) }
    var returnRoot by remember { mutableStateOf(LibraryRoute.Root()) }

    fun openLibraryRoute(target: LibraryRoute) {
        (route as? LibraryRoute.Root)?.let { returnRoot = it }
        onDestination(Destination.HOME)
        onSearching(false)
        onRoute(target)
    }

    fun openArtist(name: String) {
        openLibraryRoute(LibraryRoute.Artist(name))
    }
    fun openAlbum(song: Song) {
        albums.firstOrNull { album -> album.songs.any { it.id == song.id } }?.let {
            openLibraryRoute(LibraryRoute.Album(it.key))
        }
    }
    fun playSongNext(song: Song) = playNext(player, song)
    fun appendSong(song: Song) = addToQueue(player, song)

    BackHandler(enabled = searching || route !is LibraryRoute.Root || destination != Destination.HOME) {
        when {
            searching -> onSearching(false)
            route is LibraryRoute.Artist -> onRoute(LibraryRoute.Root(LibraryTab.ARTISTS))
            route is LibraryRoute.Album -> onRoute(LibraryRoute.Root(LibraryTab.ALBUMS))
            route is LibraryRoute.Playlist -> onRoute(LibraryRoute.Root(LibraryTab.PLAYLISTS))
            route is LibraryRoute.SmartPlaylistDetail -> onRoute(LibraryRoute.Root(LibraryTab.PLAYLISTS))
            route is LibraryRoute.Folder -> onRoute(route.parentFolderRoute())
            route is LibraryRoute.Genre -> onRoute(LibraryRoute.Genres)
            route == LibraryRoute.Genres -> onRoute(returnRoot)
            destination != Destination.HOME -> onDestination(Destination.HOME)
        }
    }

    CompositionLocalProvider(LocalConversionRequest provides onConvertSong) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when {
                searching -> SearchScreen(
                    songs = state.songs,
                    currentSongId = currentSong?.id,
                    favoriteIds = state.favoriteIds,
                    onBack = { onSearching(false) },
                    onSong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onArtist = ::openArtist,
                    onAlbum = ::openAlbum,
                    onPlayNext = ::playSongNext,
                    onAddToQueue = ::appendSong,
                    onAddToPlaylist = { playlistSong = it }
                )
                destination == Destination.FAVORITES -> CollectionScreen(
                    title = "Favoritos",
                    songs = favoriteSongs,
                    emptyMessage = "Marque músicas com o coração para encontrá-las aqui.",
                    icon = Icons.Default.Favorite,
                    currentSongId = currentSong?.id,
                    favoriteIds = state.favoriteIds,
                    onSearch = { onSearching(true) },
                    onSong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onArtist = ::openArtist,
                    onAlbum = ::openAlbum,
                    onPlayNext = ::playSongNext,
                    onAddToQueue = ::appendSong,
                    onAddToPlaylist = { playlistSong = it }
                )
                destination == Destination.RECENT -> CollectionScreen(
                    title = "Recentes",
                    songs = recentSongs,
                    emptyMessage = "As músicas reproduzidas recentemente aparecerão aqui.",
                    icon = Icons.Default.History,
                    currentSongId = currentSong?.id,
                    favoriteIds = state.favoriteIds,
                    onSearch = { onSearching(true) },
                    onSong = onPlaySong,
                    onToggleFavorite = onToggleFavorite,
                    onArtist = ::openArtist,
                    onAlbum = ::openAlbum,
                    onPlayNext = ::playSongNext,
                    onAddToQueue = ::appendSong,
                    onAddToPlaylist = { playlistSong = it }
                )
                else -> when (val currentRoute = route) {
                    is LibraryRoute.Root -> LibraryRootScreen(
                        songs = sortedSongs,
                        tab = currentRoute.tab,
                        sort = state.sort,
                        shuffleEnabled = shuffleEnabled,
                        currentSongId = currentSong?.id,
                        favoriteIds = state.favoriteIds,
                        onTab = { onRoute(LibraryRoute.Root(it)) },
                        onSort = onSort,
                        onShuffle = {
                            val activePlayer = player ?: return@LibraryRootScreen
                            val enableShuffle = !activePlayer.shuffleModeEnabled
                            activePlayer.shuffleModeEnabled = enableShuffle
                            if (enableShuffle) sortedSongs.randomOrNull()?.let { onPlaySong(it, sortedSongs) }
                        },
                        onSearch = { onSearching(true) },
                        onSong = onPlaySong,
                        onToggleFavorite = onToggleFavorite,
                        onArtist = ::openArtist,
                        onAlbum = ::openAlbum,
                        onPlayNext = ::playSongNext,
                        onAddToQueue = ::appendSong,
                        onAddToPlaylist = { playlistSong = it },
                        onOpenFolder = { openLibraryRoute(LibraryRoute.Folder(it)) },
                        onGenres = { openLibraryRoute(LibraryRoute.Genres) },
                        onQueue = openQueue,
                        onEqualizer = { openFeature(FeatureRoute.EQUALIZER) },
                        onSleepTimer = { openFeature(FeatureRoute.SLEEP_TIMER) },
                        onSettings = { openFeature(FeatureRoute.SETTINGS) },
                        onAddFolder = onAddFolder,
                        onAddSong = onAddSong,
                        onRefreshLibrary = onRefreshLibrary,
                        playlistContent = {
                            PlaylistsScreen(
                                state.playlists,
                                state.songs,
                                state.favoriteIds,
                                state.recentIds,
                                state.playCounts,
                                onCreatePlaylist,
                                { onRoute(LibraryRoute.Playlist(it)) },
                                { onRoute(LibraryRoute.SmartPlaylistDetail(it)) },
                                onRenamePlaylist,
                                onDeletePlaylist
                            )
                        }
                    )
                    is LibraryRoute.Artist -> artists.firstOrNull { it.name == currentRoute.name }?.let { artist ->
                        ArtistDetailScreen(
                            artist, currentSong?.id, state.favoriteIds,
                            onBack = { onRoute(LibraryRoute.Root(LibraryTab.ARTISTS)) },
                            onSong = onPlaySong,
                            onToggleFavorite = onToggleFavorite,
                            onAlbum = ::openAlbum,
                            onPlayNext = ::playSongNext,
                            onAddToQueue = ::appendSong,
                            onAddToPlaylist = { playlistSong = it }
                        )
                    } ?: EmptyLibraryPage("Artista", "Este artista não está mais na biblioteca.", Icons.AutoMirrored.Filled.QueueMusic)
                    is LibraryRoute.Album -> albums.firstOrNull { it.key == currentRoute.key }?.let { album ->
                        AlbumDetailScreen(
                            album, currentSong?.id, state.favoriteIds,
                            onBack = { onRoute(LibraryRoute.Root(LibraryTab.ALBUMS)) },
                            onSong = onPlaySong,
                            onToggleFavorite = onToggleFavorite,
                            onArtist = ::openArtist,
                            onPlayNext = ::playSongNext,
                            onAddToQueue = ::appendSong,
                            onAddToPlaylist = { playlistSong = it }
                        )
                    } ?: EmptyLibraryPage("Álbum", "Este álbum não está mais na biblioteca.", Icons.AutoMirrored.Filled.QueueMusic)
                    is LibraryRoute.Playlist -> state.playlists.firstOrNull { it.id == currentRoute.id }?.let { playlist ->
                        val playlistIsPlaying = playing && currentSong?.id in playlist.songIds
                        PlaylistDetailScreen(
                            playlist = playlist,
                            songs = state.songs,
                            currentSongId = currentSong?.id,
                            shuffleEnabled = shuffleEnabled,
                            repeatEnabled = repeatMode == Player.REPEAT_MODE_ALL,
                            onBack = { onRoute(LibraryRoute.Root(LibraryTab.PLAYLISTS)) },
                            onToggleShuffle = { songs ->
                                val activePlayer = player ?: return@PlaylistDetailScreen
                                val enableShuffle = !activePlayer.shuffleModeEnabled
                                activePlayer.shuffleModeEnabled = enableShuffle
                                if (enableShuffle && !playlistIsPlaying) {
                                    songs.randomOrNull()?.let { onPlaySong(it, songs) }
                                }
                            },
                            onToggleRepeat = {
                                player?.let { activePlayer ->
                                    activePlayer.repeatMode = if (activePlayer.repeatMode == Player.REPEAT_MODE_ALL) {
                                        Player.REPEAT_MODE_OFF
                                    } else {
                                        Player.REPEAT_MODE_ALL
                                    }
                                }
                            },
                            onPlaySong = onPlaySong,
                            onRemove = { onRemoveSongFromPlaylist(playlist.id, it) },
                            onMove = { from, to -> onMovePlaylistSong(playlist.id, from, to) }
                        )
                    } ?: EmptyLibraryPage("Playlist", "Esta playlist não existe mais.", Icons.AutoMirrored.Filled.QueueMusic)
                    is LibraryRoute.SmartPlaylistDetail -> SmartPlaylistDetailScreen(
                        playlist = currentRoute.playlist,
                        songs = state.smartPlaylistSongs(currentRoute.playlist),
                        currentSongId = currentSong?.id,
                        favoriteIds = state.favoriteIds,
                        onBack = { onRoute(LibraryRoute.Root(LibraryTab.PLAYLISTS)) },
                        onPlaySong = onPlaySong,
                        onToggleFavorite = onToggleFavorite,
                        onArtist = ::openArtist,
                        onAlbum = ::openAlbum,
                        onPlayNext = ::playSongNext,
                        onAddToQueue = ::appendSong,
                        onAddToPlaylist = { playlistSong = it }
                    )
                    is LibraryRoute.Folder -> FolderBrowserScreen(
                        songs = state.songs,
                        currentPath = currentRoute.path,
                        currentSongId = currentSong?.id,
                        favoriteIds = state.favoriteIds,
                        onBack = { onRoute(currentRoute.parentFolderRoute()) },
                        onOpenFolder = { onRoute(LibraryRoute.Folder(it)) },
                        onSong = onPlaySong,
                        onFavorite = onToggleFavorite,
                        onArtist = ::openArtist,
                        onAlbum = ::openAlbum,
                        onPlayNext = ::playSongNext,
                        onAddQueue = ::appendSong,
                        onAddPlaylist = { playlistSong = it }
                    )
                    LibraryRoute.Genres -> GenresScreen(genres, { onRoute(returnRoot) }) { onRoute(LibraryRoute.Genre(it)) }
                    is LibraryRoute.Genre -> genres.firstOrNull { it.name == currentRoute.name }?.let { genre ->
                        GenreDetailScreen(
                            genre, currentSong?.id, state.favoriteIds,
                            onBack = { onRoute(LibraryRoute.Genres) },
                            onSong = onPlaySong,
                            onFavorite = onToggleFavorite,
                            onArtist = ::openArtist,
                            onAlbum = ::openAlbum,
                            onPlayNext = ::playSongNext,
                            onAddQueue = ::appendSong,
                            onAddPlaylist = { playlistSong = it }
                        )
                    } ?: EmptyLibraryPage("Gênero", "Este gênero não está mais disponível.", Icons.AutoMirrored.Filled.QueueMusic)
                }
            }
        }
        if (state.settings.showMiniPlayer) MiniPlayer(currentSong, player, playing, openNowPlaying)
        AppBottomBar(destination) {
            onDestination(it)
            onSearching(false)
            if (it == Destination.HOME && route !is LibraryRoute.Root) onRoute(LibraryRoute.Root())
        }
    }
    }
    playlistSong?.let { song ->
        PlaylistPickerDialog(
            song = song,
            playlists = state.playlists,
            onDismiss = { playlistSong = null },
            onAdd = { playlistId -> onAddSongToPlaylist(playlistId, song.id); playlistSong = null },
            onCreateAndAdd = { name -> onCreatePlaylistWithSong(name, song.id); playlistSong = null }
        )
    }
}

@Composable
private fun LibraryLoadingScreen() = Column(
    Modifier.fillMaxSize().padding(32.dp),
    Arrangement.Center,
    Alignment.CenterHorizontally
) {
    JukeCircularLogo(Modifier.size(96.dp))
    Spacer(Modifier.height(14.dp))
    CircularProgressIndicator(color = PrimaryBlue)
    Spacer(Modifier.height(18.dp))
    Text("Carregando sua biblioteca…", color = TextMuted)
}

@Composable
private fun LibraryErrorScreen(message: String, retry: () -> Unit) = Column(
    Modifier.fillMaxSize().padding(32.dp),
    Arrangement.Center,
    Alignment.CenterHorizontally
) {
    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = PrimaryBlue)
    Spacer(Modifier.height(18.dp))
    Text("Não foi possível carregar as músicas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(message, color = TextMuted)
    Spacer(Modifier.height(22.dp))
    Button(retry) { Text("Tentar novamente") }
}

private fun LibraryRoute.Folder.parentFolderRoute(): LibraryRoute {
    val parent = path.trim().trimEnd('/').substringBeforeLast('/', missingDelimiterValue = "")
    return parent.takeIf(String::isNotBlank)?.let(LibraryRoute::Folder) ?: LibraryRoute.Root(LibraryTab.FOLDERS)
}

private fun LibraryUiState.smartPlaylistSongs(playlist: SmartPlaylist): List<Song> = when (playlist) {
    SmartPlaylist.FAVORITES -> favoriteSongs
    SmartPlaylist.LAST_ADDED -> songs.sortedByDescending(Song::dateAdded)
    SmartPlaylist.RECENT_PLAYS -> recentSongs
    SmartPlaylist.MOST_PLAYED -> mostPlayedSongs
}

private fun Player.toSnapshot(): PlaybackSnapshot = PlaybackSnapshot(
    queueIds = (0 until mediaItemCount).mapNotNull { getMediaItemAt(it).mediaId.toLongOrNull() },
    currentIndex = currentMediaItemIndex.coerceAtLeast(0),
    positionMs = currentPosition.coerceAtLeast(0),
    repeatMode = repeatMode,
    shuffleEnabled = shuffleModeEnabled,
    wasPlaying = isPlaying
)

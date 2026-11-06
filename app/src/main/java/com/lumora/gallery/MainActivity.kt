package com.lumora.gallery

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.lumora.gallery.data.model.Album
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.model.ThemeMode
import com.lumora.gallery.ui.albums.AlbumDetailScreen
import com.lumora.gallery.ui.albums.AlbumsScreen
import com.lumora.gallery.ui.albums.AlbumsViewModel
import com.lumora.gallery.ui.navigation.FloatingNavBar
import com.lumora.gallery.ui.navigation.NavTab
import com.lumora.gallery.ui.settings.PhotosSourcesScreen
import com.lumora.gallery.ui.settings.SettingsScreen
import com.lumora.gallery.ui.settings.SettingsViewModel
import com.lumora.gallery.ui.theme.AccentGold
import com.lumora.gallery.ui.theme.LumoraTheme
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary
import com.lumora.gallery.ui.timeline.TimelineScreen
import com.lumora.gallery.ui.timeline.TimelineViewModel
import com.lumora.gallery.ui.cleanup.CleanupScreen
import com.lumora.gallery.ui.editor.PhotoEditorScreen
import com.lumora.gallery.ui.navigation.NavMode
import com.lumora.gallery.ui.navigation.TimeScale
import com.lumora.gallery.ui.search.SearchScreen
import com.lumora.gallery.ui.trash.RecentlyDeletedScreen
import com.lumora.gallery.ui.vault.PrivateVaultScreen
import com.lumora.gallery.ui.viewer.MediaViewerScreen
import kotlinx.coroutines.launch

enum class ActiveScreen {
    MAIN_TABS,
    ALBUM_DETAIL,
    SETTINGS,
    PHOTOS_SOURCES,
    VIEWER,
    EDITOR,
    SEARCH,
    PRIVATE_VAULT,
    CLEANUP,
    RECENTLY_DELETED
}

class MainActivity : ComponentActivity() {

    private lateinit var timelineViewModel: TimelineViewModel
    private lateinit var albumsViewModel: AlbumsViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    private var hasPermissions by mutableStateOf(false)
    private var externalMediaList by mutableStateOf<List<MediaItem>>(emptyList())
    private var isExternalViewIntent by mutableStateOf(false)
    private var isPickerMode by mutableStateOf(false)
    private var currentViewingItem by mutableStateOf<MediaItem?>(null)

    // Scoped storage delete launcher for Android 11+ (API 30+)
    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // MediaStore deleted the item; ContentObserver will refresh automatically
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        hasPermissions = granted
        if (granted) {
            timelineViewModel.loadAlbums()
            albumsViewModel.loadAlbums()
            handleIncomingIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LumoraApplication
        timelineViewModel = ViewModelProvider(
            this,
            TimelineViewModel.provideFactory(app.mediaRepository, app.preferencesRepository)
        )[TimelineViewModel::class.java]

        albumsViewModel = ViewModelProvider(
            this,
            AlbumsViewModel.provideFactory(app.mediaRepository, app.preferencesRepository)
        )[AlbumsViewModel::class.java]

        settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.provideFactory(app.preferencesRepository, app.mediaRepository)
        )[SettingsViewModel::class.java]

        checkInitialPermissions()
        handleIncomingIntent(intent)

        setContent {
            val themeMode by app.preferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.AMOLED_DARK)

            LumoraTheme(themeMode = themeMode) {
                if (hasPermissions || isExternalViewIntent) {
                    MainNavigationHost(
                        timelineViewModel = timelineViewModel,
                        albumsViewModel = albumsViewModel,
                        settingsViewModel = settingsViewModel,
                        app = app,
                        externalMediaList = externalMediaList,
                        isExternalViewIntent = isExternalViewIntent,
                        isPickerMode = isPickerMode,
                        onMediaSelectedInPicker = { selectedItem ->
                            val resultIntent = Intent().apply {
                                data = selectedItem.uri
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        },
                        onDeleteMediaItem = { itemToDelete ->
                            requestDeleteMedia(itemToDelete)
                        },
                        onCurrentViewingItemChanged = { item ->
                            currentViewingItem = item
                        },
                        onFinishActivity = { finish() }
                    )
                } else {
                    PermissionRequestScreen(onRequestPermission = { requestMediaPermissions() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val action = incomingIntent.action ?: return

        when (action) {
            Intent.ACTION_VIEW -> {
                val uri = incomingIntent.data
                if (uri != null) {
                    isExternalViewIntent = true
                    lifecycleScope.launch {
                        val app = application as LumoraApplication
                        val singleItem = app.mediaRepository.getMediaItemFromUri(uri)
                        if (singleItem != null) {
                            val neighbors = app.mediaRepository.getNeighboringMedia(singleItem)
                            externalMediaList = neighbors
                            currentViewingItem = singleItem
                        }
                    }
                }
            }
            Intent.ACTION_PICK, Intent.ACTION_GET_CONTENT -> {
                isPickerMode = true
            }
        }
    }

    fun requestDeleteMedia(item: MediaItem) {
        val app = application as LumoraApplication
        lifecycleScope.launch {
            // First save to the 30-day Recently Deleted vault so it can be restored anytime
            app.trashRepository.trashMedia(item)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(item.uri))
                    deleteRequestLauncher.launch(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    )
                } catch (_: Exception) {}
            } else {
                try {
                    contentResolver.delete(item.uri, null, null)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter Picture-in-Picture mode if viewing a video
        val item = currentViewingItem
        if (item != null && item.isVideo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val aspectRatio = if (item.width > 0 && item.height > 0) {
                    val safeWidth = item.width.coerceIn(100, 4000)
                    val safeHeight = item.height.coerceIn(100, 4000)
                    Rational(safeWidth, safeHeight)
                } else {
                    Rational(16, 9)
                }
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                enterPictureInPictureMode(pipParams)
            } catch (_: Exception) {}
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkInitialPermissions() {
        val permissions = getRequiredPermissions()
        hasPermissions = permissions.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestMediaPermissions() {
        requestPermissionLauncher.launch(getRequiredPermissions())
    }
}

@Composable
fun MainNavigationHost(
    timelineViewModel: TimelineViewModel,
    albumsViewModel: AlbumsViewModel,
    settingsViewModel: SettingsViewModel,
    app: LumoraApplication,
    externalMediaList: List<MediaItem>,
    isExternalViewIntent: Boolean,
    isPickerMode: Boolean,
    onMediaSelectedInPicker: (MediaItem) -> Unit,
    onDeleteMediaItem: (MediaItem) -> Unit,
    onCurrentViewingItemChanged: (MediaItem?) -> Unit,
    onFinishActivity: () -> Unit
) {
    var activeScreen by remember {
        mutableStateOf(if (isExternalViewIntent && externalMediaList.isNotEmpty()) ActiveScreen.VIEWER else ActiveScreen.MAIN_TABS)
    }
    var currentTab by remember { mutableStateOf(NavTab.PHOTOS) }
    var selectedAlbumForDetail by remember { mutableStateOf<Album?>(null) }

    var viewerMediaList by remember { mutableStateOf<List<MediaItem>>(externalMediaList) }
    var viewerInitialIndex by remember { mutableStateOf(0) }
    var selectedMediaForEdit by remember { mutableStateOf<MediaItem?>(null) }

    val eraseExifOnShare by app.preferencesRepository.eraseExifOnShareFlow.collectAsState(initial = true)
    val autoplayVideosMuted by app.preferencesRepository.autoplayVideosMutedFlow.collectAsState(initial = true)
    val currentTimeScale by timelineViewModel.timeScale.collectAsState()
    val isTimeScaleMode by timelineViewModel.isTimeScaleMode.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val tabsPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    var showCustomArrangeSheet by remember { mutableStateOf(false) }
    var showNewAlbumDialog by remember { mutableStateOf(false) }

    // Sync tabsPagerState and currentTab
    LaunchedEffect(tabsPagerState.currentPage) {
        currentTab = if (tabsPagerState.currentPage == 0) NavTab.PHOTOS else NavTab.ALBUMS
    }

    // Trigger viewer if external media list resolved after launch
    LaunchedEffect(externalMediaList) {
        if (isExternalViewIntent && externalMediaList.isNotEmpty()) {
            viewerMediaList = externalMediaList
            viewerInitialIndex = 0
            activeScreen = ActiveScreen.VIEWER
        }
    }

    BackHandler(enabled = activeScreen != ActiveScreen.MAIN_TABS || isTimeScaleMode) {
        if (isTimeScaleMode) {
            timelineViewModel.exitTimeScaleMode()
            return@BackHandler
        }
        when (activeScreen) {
            ActiveScreen.VIEWER -> {
                if (isExternalViewIntent) {
                    onFinishActivity()
                } else if (selectedAlbumForDetail != null) {
                    activeScreen = ActiveScreen.ALBUM_DETAIL
                } else {
                    activeScreen = ActiveScreen.MAIN_TABS
                }
            }
            ActiveScreen.EDITOR -> activeScreen = ActiveScreen.VIEWER
            ActiveScreen.PHOTOS_SOURCES -> activeScreen = ActiveScreen.SETTINGS
            ActiveScreen.SETTINGS -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.ALBUM_DETAIL -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.SEARCH -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.PRIVATE_VAULT -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.CLEANUP -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.RECENTLY_DELETED -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.MAIN_TABS -> { /* Default system back */ }
        }
    }

    when (activeScreen) {
        ActiveScreen.MAIN_TABS -> {
            onCurrentViewingItemChanged(null)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = PureBlack,
                bottomBar = {
                    if (!isPickerMode) {
                        FloatingNavBar(
                            navMode = if (isTimeScaleMode) NavMode.TIME_SCALE else NavMode.TABS,
                            currentTab = currentTab,
                            currentTimeScale = currentTimeScale,
                            onTabSelected = { tab ->
                                currentTab = tab
                                coroutineScope.launch {
                                    tabsPagerState.animateScrollToPage(if (tab == NavTab.PHOTOS) 0 else 1)
                                }
                            },
                            onTimeScaleSelected = { timelineViewModel.setTimeScale(it) },
                            onToggleTimeScaleMode = { timelineViewModel.toggleTimeScaleMode() },
                            onCustomArrangeClick = { showCustomArrangeSheet = true },
                            onRightActionClick = {
                                if (currentTab == NavTab.PHOTOS) {
                                    timelineViewModel.openAlbumPicker()
                                } else {
                                    showNewAlbumDialog = true
                                }
                            }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    HorizontalPager(
                        state = tabsPagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isTimeScaleMode
                    ) { page ->
                        if (page == 0) {
                            TimelineScreen(
                                viewModel = timelineViewModel,
                                onOpenSettings = { activeScreen = ActiveScreen.SETTINGS },
                                onOpenSearch = { activeScreen = ActiveScreen.SEARCH },
                                onItemClick = { item, allItems ->
                                    if (isPickerMode) {
                                        onMediaSelectedInPicker(item)
                                    } else {
                                        viewerMediaList = allItems
                                        viewerInitialIndex = allItems.indexOf(item).coerceAtLeast(0)
                                        activeScreen = ActiveScreen.VIEWER
                                    }
                                }
                            )
                        } else {
                            AlbumsScreen(
                                viewModel = albumsViewModel,
                                onAlbumClick = { album ->
                                    selectedAlbumForDetail = album
                                    activeScreen = ActiveScreen.ALBUM_DETAIL
                                },
                                onOpenSearch = { activeScreen = ActiveScreen.SEARCH },
                                onOpenSettings = { activeScreen = ActiveScreen.SETTINGS },
                                onOpenPrivateVault = { activeScreen = ActiveScreen.PRIVATE_VAULT },
                                onOpenCleanup = { activeScreen = ActiveScreen.CLEANUP },
                                onOpenRecentlyDeleted = { activeScreen = ActiveScreen.RECENTLY_DELETED },
                                showCustomArrangeSheet = showCustomArrangeSheet,
                                onDismissCustomArrangeSheet = { showCustomArrangeSheet = false },
                                showNewAlbumDialog = showNewAlbumDialog,
                                onDismissNewAlbumDialog = { showNewAlbumDialog = false }
                            )
                        }
                    }
                }
            }
        }

        ActiveScreen.ALBUM_DETAIL -> {
            onCurrentViewingItemChanged(null)
            selectedAlbumForDetail?.let { album ->
                AlbumDetailScreen(
                    album = album,
                    mediaRepository = app.mediaRepository,
                    onBackClick = { activeScreen = ActiveScreen.MAIN_TABS },
                    onItemClick = { item, allAlbumItems ->
                        if (isPickerMode) {
                            onMediaSelectedInPicker(item)
                        } else {
                            viewerMediaList = allAlbumItems
                            viewerInitialIndex = allAlbumItems.indexOf(item).coerceAtLeast(0)
                            activeScreen = ActiveScreen.VIEWER
                        }
                    }
                )
            }
        }

        ActiveScreen.VIEWER -> {
            MediaViewerScreen(
                mediaList = viewerMediaList,
                initialIndex = viewerInitialIndex,
                onBackClick = {
                    if (isExternalViewIntent) {
                        onFinishActivity()
                    } else if (selectedAlbumForDetail != null) {
                        activeScreen = ActiveScreen.ALBUM_DETAIL
                    } else {
                        activeScreen = ActiveScreen.MAIN_TABS
                    }
                },
                onDeleteMedia = { item ->
                    val currentIdx = viewerMediaList.indexOfFirst { it.id == item.id }
                    val nextList = viewerMediaList.filter { it.id != item.id }
                    if (nextList.isEmpty()) {
                        activeScreen = if (selectedAlbumForDetail != null) ActiveScreen.ALBUM_DETAIL else ActiveScreen.MAIN_TABS
                    } else {
                        viewerInitialIndex = currentIdx.coerceIn(0, nextList.size - 1)
                        viewerMediaList = nextList
                    }
                    onDeleteMediaItem(item)
                },
                onEditMedia = { item ->
                    selectedMediaForEdit = item
                    activeScreen = ActiveScreen.EDITOR
                },
                eraseExifOnShare = eraseExifOnShare,
                autoplayVideosMuted = autoplayVideosMuted
            )
        }

        ActiveScreen.EDITOR -> {
            selectedMediaForEdit?.let { mediaItem ->
                PhotoEditorScreen(
                    mediaItem = mediaItem,
                    onDismiss = { activeScreen = ActiveScreen.VIEWER },
                    onSaved = { _ ->
                        activeScreen = ActiveScreen.VIEWER
                    }
                )
            }
        }

        ActiveScreen.SETTINGS -> {
            onCurrentViewingItemChanged(null)
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { activeScreen = ActiveScreen.MAIN_TABS },
                onOpenPhotosSources = { activeScreen = ActiveScreen.PHOTOS_SOURCES }
            )
        }

        ActiveScreen.PHOTOS_SOURCES -> {
            onCurrentViewingItemChanged(null)
            PhotosSourcesScreen(
                viewModel = settingsViewModel,
                preferencesRepository = app.preferencesRepository,
                onBackClick = { activeScreen = ActiveScreen.SETTINGS }
            )
        }

        ActiveScreen.SEARCH -> {
            onCurrentViewingItemChanged(null)
            SearchScreen(
                mediaRepository = app.mediaRepository,
                onBackClick = { activeScreen = ActiveScreen.MAIN_TABS },
                onItemClick = { item, allResults ->
                    viewerMediaList = allResults
                    viewerInitialIndex = allResults.indexOf(item).coerceAtLeast(0)
                    activeScreen = ActiveScreen.VIEWER
                }
            )
        }

        ActiveScreen.PRIVATE_VAULT -> {
            onCurrentViewingItemChanged(null)
            PrivateVaultScreen(
                onBackClick = { activeScreen = ActiveScreen.MAIN_TABS }
            )
        }

        ActiveScreen.CLEANUP -> {
            onCurrentViewingItemChanged(null)
            CleanupScreen(
                mediaRepository = app.mediaRepository,
                onBackClick = { activeScreen = ActiveScreen.MAIN_TABS },
                onDeleteMedia = { item -> onDeleteMediaItem(item) }
            )
        }

        ActiveScreen.RECENTLY_DELETED -> {
            onCurrentViewingItemChanged(null)
            RecentlyDeletedScreen(
                onBackClick = { activeScreen = ActiveScreen.MAIN_TABS }
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.permission_required_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.permission_required_desc),
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = PureBlack),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_grant_permission),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

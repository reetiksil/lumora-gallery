package com.lumora.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.lumora.gallery.data.model.Album
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

enum class ActiveScreen {
    MAIN_TABS,
    ALBUM_DETAIL,
    SETTINGS,
    PHOTOS_SOURCES
}

class MainActivity : ComponentActivity() {

    private lateinit var timelineViewModel: TimelineViewModel
    private lateinit var albumsViewModel: AlbumsViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    private var hasPermissions by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        hasPermissions = granted
        if (granted) {
            timelineViewModel.loadAlbums()
            albumsViewModel.loadAlbums()
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

        setContent {
            LumoraTheme {
                if (hasPermissions) {
                    MainNavigationHost(
                        timelineViewModel = timelineViewModel,
                        albumsViewModel = albumsViewModel,
                        settingsViewModel = settingsViewModel,
                        app = app
                    )
                } else {
                    PermissionRequestScreen(onRequestPermission = { requestMediaPermissions() })
                }
            }
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
    app: LumoraApplication
) {
    var activeScreen by remember { mutableStateOf(ActiveScreen.MAIN_TABS) }
    var currentTab by remember { mutableStateOf(NavTab.PHOTOS) }
    var selectedAlbumForDetail by remember { mutableStateOf<Album?>(null) }

    BackHandler(enabled = activeScreen != ActiveScreen.MAIN_TABS) {
        when (activeScreen) {
            ActiveScreen.PHOTOS_SOURCES -> activeScreen = ActiveScreen.SETTINGS
            ActiveScreen.SETTINGS -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.ALBUM_DETAIL -> activeScreen = ActiveScreen.MAIN_TABS
            ActiveScreen.MAIN_TABS -> { /* Default system back */ }
        }
    }

    when (activeScreen) {
        ActiveScreen.MAIN_TABS -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = PureBlack,
                bottomBar = {
                    FloatingNavBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onToggleGridDensity = { timelineViewModel.cycleGridColumns() },
                        onOpenFilter = { timelineViewModel.openAlbumPicker() }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (currentTab == NavTab.PHOTOS) {
                        TimelineScreen(
                            viewModel = timelineViewModel,
                            onOpenSettings = { activeScreen = ActiveScreen.SETTINGS }
                        )
                    } else {
                        AlbumsScreen(
                            viewModel = albumsViewModel,
                            onAlbumClick = { album ->
                                selectedAlbumForDetail = album
                                activeScreen = ActiveScreen.ALBUM_DETAIL
                            }
                        )
                    }
                }
            }
        }

        ActiveScreen.ALBUM_DETAIL -> {
            selectedAlbumForDetail?.let { album ->
                AlbumDetailScreen(
                    album = album,
                    mediaRepository = app.mediaRepository,
                    onBackClick = { activeScreen = ActiveScreen.MAIN_TABS },
                    onItemClick = { /* Viewer opens in milestone c */ }
                )
            }
        }

        ActiveScreen.SETTINGS -> {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { activeScreen = ActiveScreen.MAIN_TABS },
                onOpenPhotosSources = { activeScreen = ActiveScreen.PHOTOS_SOURCES }
            )
        }

        ActiveScreen.PHOTOS_SOURCES -> {
            PhotosSourcesScreen(
                viewModel = settingsViewModel,
                preferencesRepository = app.preferencesRepository,
                onBackClick = { activeScreen = ActiveScreen.SETTINGS }
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

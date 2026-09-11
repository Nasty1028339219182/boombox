package app.tonica.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tonica.TonicaApp

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("library", "Библиотека", Icons.Filled.LibraryMusic),
    Tab("search", "Поиск", Icons.Filled.Search),
    Tab("downloads", "Скачано", Icons.Filled.Download),
    Tab("settings", "Ещё", Icons.Filled.Settings),
)

@Composable
fun AppRoot(app: TonicaApp) {
    val session by app.sessionStore.session.collectAsState(initial = null)
    var hydrated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        app.sessionStore.current()
        hydrated = true
    }
    if (!hydrated) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }
    val current = session
    if (current == null) {
        LoginScreen(app)
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "library"
    val showTabs = tabs.any { it.route == route }
    val showMiniPref by app.sessionStore.showMiniPlayer.collectAsState(initial = true)
    val nowPlaying by app.player.current.collectAsState()
    val showMini = showMiniPref && nowPlaying != null

    val notifyPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(current.username) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifyPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(showMiniPref) {
        if (!showMiniPref) app.player.stop()
    }

    fun openArtist(id: String) = nav.navigate("artist/${Uri.encode(id)}")
    fun openAlbum(id: String) {
        app.lockPlay(900)
        nav.navigate("album/${Uri.encode(id)}")
    }
    fun openGenre(name: String) = nav.navigate("genre/${Uri.encode(name)}")

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column {
                if (showMini) {
                    MiniPlayer(app, current, onDismiss = { app.player.stop() })
                }
                if (showTabs) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "library",
            modifier = Modifier.padding(padding),
        ) {
            composable("library") {
                LibraryScreen(app, current, onArtist = ::openArtist, onAlbum = ::openAlbum, onGenre = ::openGenre)
            }
            composable("search") {
                SearchScreen(app, current, onArtist = ::openArtist, onAlbum = ::openAlbum, onGenre = ::openGenre)
            }
            composable("downloads") { DownloadsScreen(app, current) }
            composable("settings") { SettingsScreen(app, current) }
            composable("artist/{id}") { entry ->
                val id = Uri.decode(entry.arguments?.getString("id") ?: return@composable)
                ArtistScreen(app, current, id, onBack = { nav.popBackStack() }, onAlbum = ::openAlbum)
            }
            composable("album/{id}") { entry ->
                val id = Uri.decode(entry.arguments?.getString("id") ?: return@composable)
                AlbumScreen(app, current, id, onBack = { nav.popBackStack() })
            }
            composable("genre/{name}") { entry ->
                val name = Uri.decode(entry.arguments?.getString("name") ?: return@composable)
                GenreScreen(app, current, name, onBack = { nav.popBackStack() }, onAlbum = ::openAlbum)
            }
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.tonica.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tonica.TonicaApp
import app.tonica.data.Album
import app.tonica.data.Artist
import app.tonica.data.ArtistIndex
import app.tonica.data.DownloadedTrack
import app.tonica.data.NdSession
import app.tonica.data.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(app: TonicaApp) {
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) {
                    app.client.login(server, username, password, rememberMe)
                }
                app.sessionStore.save(session)
            } catch (e: Exception) {
                error = e.message ?: "Не удалось войти"
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("B", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Boombox",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Скачивайте музыку с Navidrome. Исполнители, альбомы и офлайн-библиотека.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Сервер") },
            placeholder = { Text("https://music.example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Имя пользователя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(
                        if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPass) "Скрыть пароль" else "Показать пароль",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            shape = RoundedCornerShape(16.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("Запомнить вход", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { submit() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Войти")
        }
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = {
                server = "https://demo.navidrome.org"
                username = "demo"
                password = "demo"
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Демо-сервер Navidrome") }
    }
}

@Composable
fun LibraryScreen(app: TonicaApp, session: NdSession, onArtist: (String) -> Unit, onAlbum: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var indexes by remember { mutableStateOf<List<ArtistIndex>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session.username, session.serverUrl) {
        loading = true
        error = null
        try {
            withContext(Dispatchers.IO) {
                indexes = app.client.artists(session)
                albums = app.client.albums(session)
            }
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить библиотеку"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Библиотека",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Исполнители") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Альбомы") })
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> EmptyState("Нет связи", error!!)
            tab == 0 -> {
                val artists = indexes.flatMap { it.artists }
                if (artists.isEmpty()) EmptyState("Пусто", "На сервере пока нет исполнителей")
                else LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    indexes.forEach { index ->
                        item(key = "idx-${index.name}") {
                            Text(
                                index.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        items(index.artists, key = { it.id }) { artist ->
                            ArtistRow(app, session, artist) { onArtist(artist.id) }
                        }
                    }
                }
            }
            else -> {
                if (albums.isEmpty()) EmptyState("Пусто", "На сервере пока нет альбомов")
                else LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(albums, key = { it.id }) { album ->
                        AlbumRow(app, session, album) { onAlbum(album.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistRow(app: TonicaApp, session: NdSession, artist: Artist, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(app.client.coverUrl(session, artist.coverArt, 120), 56.dp, 18.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                artist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(albumsLabel(artist.albumCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ArtistScreen(app: TonicaApp, session: NdSession, id: String, onBack: () -> Unit, onAlbum: (String) -> Unit) {
    var artist by remember { mutableStateOf<Artist?>(null) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val tracks by app.downloads.tracks.collectAsState()

    LaunchedEffect(id) {
        loading = true
        try {
            withContext(Dispatchers.IO) {
                val pair = app.client.artist(session, id)
                artist = pair.first
                albums = pair.second
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
            val a = artist
            if (a != null) {
                CoverArt(app.client.coverUrl(session, a.coverArt, 120), 48.dp, 12.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        a.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(albumsLabel(albums.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("Исполнитель", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> EmptyState("Ошибка", error!!)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(albums, key = { it.id }) { album ->
                    val localCount = tracks.count { it.albumId == album.id }
                    val subtitle = buildString {
                        if (album.year > 0) append(album.year)
                        if (album.songCount > 0) {
                            if (isNotEmpty()) append(" · ")
                            append(tracksLabel(album.songCount))
                        }
                        if (localCount > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("скачано $localCount")
                        }
                    }
                    AlbumRow(app, session, album, subtitle = subtitle.ifBlank { null }) { onAlbum(album.id) }
                }
            }
        }
    }
}

@Composable
fun AlbumScreen(app: TonicaApp, session: NdSession, id: String, onBack: () -> Unit) {
    var album by remember { mutableStateOf<Album?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    val tracks by app.downloads.tracks.collectAsState()
    val jobs by app.downloads.jobs.collectAsState()

    LaunchedEffect(id) {
        loading = true
        ready = false
        try {
            album = withContext(Dispatchers.IO) { app.client.album(session, id) }
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
        delay(400)
        ready = true
    }

    val songs = album?.songs.orEmpty()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
            val a = album
            if (a != null) {
                CoverArt(app.client.coverUrl(session, a.coverArt, 160), 52.dp, 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        a.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(a.artist.takeIf { it.isNotBlank() }, a.year.takeIf { it > 0 }?.toString()).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text("Альбом", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> EmptyState("Ошибка", error!!)
            else -> {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { app.play(session, songs, 0) },
                        enabled = ready && songs.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Слушать")
                    }
                    FilledTonalButton(
                        onClick = { app.download(session, songs.filter { t -> tracks.none { it.id == t.id } }) },
                        enabled = ready,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Download, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Скачать")
                    }
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(songs, key = { it.id }) { song ->
                        val job = jobs.find { it.id == song.id }
                        TrackRow(
                            song = song,
                            session = session,
                            app = app,
                            downloaded = tracks.any { it.id == song.id },
                            downloading = job != null && job.error == null,
                            progress = job?.progress ?: 0f,
                            enabled = ready,
                            onPlay = { app.play(session, songs, songs.indexOf(song)) },
                            onDownload = { app.download(session, listOf(song)) },
                        )
                        if (job?.error != null) {
                            Text(job.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 76.dp, bottom = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(app: TonicaApp, session: NdSession, onArtist: (String) -> Unit, onAlbum: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(SearchResult()) }
    var loading by remember { mutableStateOf(false) }
    val tracks by app.downloads.tracks.collectAsState()
    val jobs by app.downloads.jobs.collectAsState()

    LaunchedEffect(query) {
        if (query.isBlank()) {
            result = SearchResult()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(280)
        try {
            result = withContext(Dispatchers.IO) { app.client.search(session, query) }
        } catch (_: Exception) {
            result = SearchResult()
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Поиск",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Альбомы, треки, исполнители") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
        )
        when {
            query.isBlank() -> EmptyState("Найдите музыку", "Ищите по альбомам, трекам и исполнителям")
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            result.artists.isEmpty() && result.albums.isEmpty() && result.songs.isEmpty() ->
                EmptyState("Ничего не нашлось", "Попробуйте другой запрос")
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp, top = 12.dp)) {
                if (result.artists.isNotEmpty()) {
                    item { Text("Исполнители", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                    items(result.artists, key = { "a-${it.id}" }) { artist ->
                        ArtistRow(app, session, artist) { onArtist(artist.id) }
                    }
                }
                if (result.albums.isNotEmpty()) {
                    item { Text("Альбомы", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                    items(result.albums, key = { "al-${it.id}" }) { album ->
                        AlbumRow(app, session, album) { onAlbum(album.id) }
                    }
                }
                if (result.songs.isNotEmpty()) {
                    item { Text("Треки", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                    items(result.songs, key = { "s-${it.id}" }) { song ->
                        val job = jobs.find { it.id == song.id }
                        TrackRow(
                            song = song,
                            session = session,
                            app = app,
                            downloaded = tracks.any { it.id == song.id },
                            downloading = job != null && job.error == null,
                            progress = job?.progress ?: 0f,
                            onPlay = { app.play(session, result.songs, result.songs.indexOf(song)) },
                            onDownload = { app.download(session, listOf(song)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen(app: TonicaApp, session: NdSession) {
    val tracks by app.downloads.tracks.collectAsState()
    val jobs by app.downloads.jobs.collectAsState()
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val grouped = tracks.groupBy { it.artistId to it.artist }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Скачано",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (tracks.isNotEmpty()) {
                TextButton(onClick = { confirm = "Удалить всю офлайн-библиотеку?" to { app.downloads.removeAll() } }) {
                    Text("Очистить")
                }
            }
        }
        if (jobs.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                jobs.forEach { job ->
                    Text(if (job.error != null) job.error!! else "Скачивается: ${job.title}", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { job.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                }
            }
        }
        if (tracks.isEmpty() && jobs.isEmpty()) {
            EmptyState("Пока пусто", "Скачайте альбом или трек — он появится здесь")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                grouped.forEach { (key, artistTracks) ->
                    val (artistId, artistName) = key
                    item(key = "ar-$artistId") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(artistName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                confirm = "Удалить все треки $artistName?" to { app.downloads.removeArtist(artistId) }
                            }) { Icon(Icons.Filled.Delete, "Удалить исполнителя") }
                        }
                    }
                    artistTracks.groupBy { it.albumId to it.album }.forEach { (albumKey, albumTracks) ->
                        val (albumId, albumName) = albumKey
                        item(key = "al-$albumId") {
                            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(albumName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    confirm = "Удалить альбом $albumName?" to { app.downloads.removeAlbum(albumId) }
                                }) { Icon(Icons.Filled.Delete, "Удалить альбом") }
                            }
                        }
                        items(albumTracks, key = { it.id }) { track ->
                            DownloadedRow(app, session, track) {
                                confirm = "Удалить «${track.title}»?" to { app.downloads.removeTrack(track.id) }
                            }
                        }
                    }
                }
            }
        }
    }

    confirm?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Удалить?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { action(); confirm = null }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun DownloadedRow(app: TonicaApp, session: NdSession, track: DownloadedTrack, onDelete: () -> Unit) {
    val songs = app.downloads.all().map { app.downloads.toSong(it) }
    Row(
        Modifier.fillMaxWidth().clickable {
            val start = songs.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            app.play(session, songs, start)
        }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(app.client.coverUrl(session, track.coverArt, 80), 48.dp, 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            Text(formatDuration(track.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Удалить трек") }
    }
}

@Composable
fun SettingsScreen(app: TonicaApp, session: NdSession) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folder by app.sessionStore.folderUri.collectAsState(initial = null)
    val showMini by app.sessionStore.showMiniPlayer.collectAsState(initial = true)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            scope.launch { app.sessionStore.setFolder(uri.toString()) }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(20.dp))
        Text("Аккаунт", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(session.name.ifBlank { session.username }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(session.serverUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Плеер", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Мини-плеер", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Если выключить — панель снизу скрыта всегда. Её также можно смахнуть вниз или закрыть крестиком до следующего трека.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = showMini, onCheckedChange = { scope.launch { app.sessionStore.setShowMiniPlayer(it) } })
        }
        Spacer(Modifier.height(24.dp))
        Text("Папка загрузок", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            if (folder.isNullOrBlank()) "Файлы сохраняются во внутреннюю папку приложения. Можно выбрать свою."
            else folder!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = { picker.launch(null) }, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Filled.Folder, null)
            Spacer(Modifier.width(8.dp))
            Text(if (folder.isNullOrBlank()) "Выбрать папку" else "Сменить папку")
        }
        if (!folder.isNullOrBlank()) {
            TextButton(onClick = { scope.launch { app.sessionStore.setFolder(null) } }) { Text("Сбросить папку") }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { scope.launch { app.sessionStore.clear() } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Выйти") }
    }
}

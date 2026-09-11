package app.tonica.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

class DownloadStore(private val context: Context) {
    private val metaFile get() = File(context.filesDir, "downloads.json")
    private val _tracks = MutableStateFlow(readAll())
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val tracks = _tracks.asStateFlow()
    val jobs = _jobs.asStateFlow()
    private val pending = ConcurrentLinkedQueue<Song>()

    @Synchronized
    private fun readAll(): List<DownloadedTrack> {
        if (!metaFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(metaFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DownloadedTrack(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    album = o.getString("album"),
                    albumId = o.getString("albumId"),
                    artist = o.getString("artist"),
                    artistId = o.getString("artistId"),
                    coverArt = o.optString("coverArt").ifBlank { null },
                    track = o.optInt("track"),
                    duration = o.optInt("duration"),
                    size = o.optLong("size"),
                    suffix = o.optString("suffix", "mp3"),
                    savedAt = o.optLong("savedAt"),
                    path = o.getString("path"),
                    genre = o.optString("genre"),
                )
            }.sortedWith(compareBy({ it.artist }, { it.album }, { it.track }))
        }.getOrDefault(emptyList())
    }

    fun all(): List<DownloadedTrack> = _tracks.value

    fun isDownloaded(id: String) = all().any { it.id == id }

    @Synchronized
    private fun writeAll(items: List<DownloadedTrack>) {
        val arr = JSONArray()
        items.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("album", t.album)
                    .put("albumId", t.albumId)
                    .put("artist", t.artist)
                    .put("artistId", t.artistId)
                    .put("coverArt", t.coverArt ?: "")
                    .put("track", t.track)
                    .put("duration", t.duration)
                    .put("size", t.size)
                    .put("suffix", t.suffix)
                    .put("savedAt", t.savedAt)
                    .put("path", t.path)
                    .put("genre", t.genre),
            )
        }
        metaFile.writeText(arr.toString())
        _tracks.value = items.sortedWith(compareBy({ it.artist }, { it.album }, { it.track }))
    }

    fun enqueue(songs: List<Song>): Int {
        var added = 0
        songs.forEach { song ->
            if (!isDownloaded(song.id) && pending.none { it.id == song.id } && _jobs.value.none { it.id == song.id && it.error == null }) {
                pending.add(song)
                _jobs.value = _jobs.value.filter { it.id != song.id } + DownloadJob(song.id, song.title, 0f)
                added++
            }
        }
        return added
    }

    fun pendingCount() = pending.size + _jobs.value.count { it.error == null && it.progress > 0f }

    fun takeNext(): Song? = pending.poll()

    fun saveSong(
        song: Song,
        bytes: ByteArray,
        folderUri: String?,
        layout: FolderLayout,
        fileStyle: FileNameStyle,
    ): DownloadedTrack {
        val rel = FolderPaths.relative(song, layout, fileStyle)
        val path = writeBytes(rel, bytes, folderUri)
        val track = DownloadedTrack(
            id = song.id,
            title = song.title,
            album = song.album.ifBlank { "Неизвестный альбом" },
            albumId = song.albumId.ifBlank { song.id },
            artist = song.artist.ifBlank { "Неизвестный исполнитель" },
            artistId = song.artistId.ifBlank { "unknown" },
            coverArt = song.coverArt,
            track = song.track,
            duration = song.duration,
            size = bytes.size.toLong(),
            suffix = song.suffix,
            savedAt = System.currentTimeMillis(),
            path = path,
            genre = song.genre,
        )
        writeAll(all().filter { it.id != song.id && it.path != path } + track)
        return track
    }

    fun localUri(id: String): String? {
        val track = all().find { it.id == id } ?: return null
        return if (track.path.startsWith("content:")) {
            track.path
        } else {
            val file = File(track.path)
            if (file.exists()) file.toURI().toString() else null
        }
    }

    fun toSong(track: DownloadedTrack) = Song(
        id = track.id,
        title = track.title,
        album = track.album,
        albumId = track.albumId,
        artist = track.artist,
        artistId = track.artistId,
        coverArt = track.coverArt,
        track = track.track,
        duration = track.duration,
        size = track.size,
        suffix = track.suffix,
        genre = track.genre,
    )

    private fun deletePath(path: String) {
        runCatching {
            if (path.startsWith("content:")) {
                DocumentFile.fromSingleUri(context, Uri.parse(path))?.delete()
            } else {
                File(path).delete()
            }
        }
    }

    fun removeTrack(id: String) {
        val track = all().find { it.id == id } ?: return
        deletePath(track.path)
        writeAll(all().filter { it.id != id })
    }

    fun removeAlbum(albumId: String) {
        all().filter { it.albumId == albumId }.forEach { deletePath(it.path) }
        writeAll(all().filter { it.albumId != albumId })
    }

    fun removeArtist(artistId: String) {
        all().filter { it.artistId == artistId }.forEach { deletePath(it.path) }
        writeAll(all().filter { it.artistId != artistId })
    }

    fun removeAll() {
        all().forEach { deletePath(it.path) }
        writeAll(emptyList())
    }

    fun beginJob(song: Song): Boolean {
        if (isDownloaded(song.id)) {
            _jobs.value = _jobs.value.filter { it.id != song.id }
            return false
        }
        if (_jobs.value.none { it.id == song.id }) {
            _jobs.value = _jobs.value + DownloadJob(song.id, song.title, 0f)
        }
        return true
    }

    fun updateJob(id: String, progress: Float, error: String? = null) {
        _jobs.value = _jobs.value.map { job ->
            if (job.id == id) job.copy(progress = progress, error = error) else job
        }
    }

    fun finishJob(id: String) {
        _jobs.value = _jobs.value.filter { it.id != id }
    }

    suspend fun runDownload(
        session: NdSession,
        client: NavidromeClient,
        song: Song,
        folderUri: String?,
        layout: FolderLayout,
        fileStyle: FileNameStyle,
    ) {
        if (!beginJob(song)) return
        try {
            val bytes = client.downloadBytes(session, song.id) { p -> updateJob(song.id, p) }
            saveSong(song, bytes, folderUri, layout, fileStyle)
            finishJob(song.id)
        } catch (e: Exception) {
            updateJob(song.id, 0f, e.message ?: "Не удалось скачать")
        }
    }

    fun importExisting(folderUri: String?): Int {
        val found = mutableListOf<ImportedFile>()
        if (!folderUri.isNullOrBlank()) {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            if (tree != null) walkTree(tree, emptyList(), found)
        }
        val fallback = File(context.getExternalFilesDir(null) ?: context.filesDir, "Boombox")
        if (fallback.exists()) walkFiles(fallback, emptyList(), found)
        if (found.isEmpty()) return 0
        val existing = all()
        val byPath = existing.associateBy { it.path }
        val byKey = existing.associateBy { keyOf(it.artist, it.album, it.title) }
        val merged = existing.toMutableList()
        var added = 0
        found.forEach { file ->
            if (byPath.containsKey(file.path)) return@forEach
            val meta = readMeta(file)
            val key = keyOf(meta.artist, meta.album, meta.title)
            if (byKey.containsKey(key)) return@forEach
            val id = "local-${file.path.hashCode().toUInt().toString(16)}"
            if (merged.any { it.id == id }) return@forEach
            merged += DownloadedTrack(
                id = id,
                title = meta.title,
                album = meta.album,
                albumId = "local-album-${meta.artist}-${meta.album}".hashCode().toString(),
                artist = meta.artist,
                artistId = "local-artist-${meta.artist}".hashCode().toString(),
                coverArt = null,
                track = meta.track,
                duration = meta.duration,
                size = meta.size,
                suffix = meta.suffix,
                savedAt = System.currentTimeMillis(),
                path = file.path,
                genre = meta.genre,
            )
            added++
        }
        if (added > 0) writeAll(merged)
        return added
    }

    private data class ImportedFile(val path: String, val name: String, val parents: List<String>, val size: Long)
    private data class Meta(
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val track: Int,
        val duration: Int,
        val size: Long,
        val suffix: String,
    )

    private fun walkTree(dir: DocumentFile, parents: List<String>, out: MutableList<ImportedFile>) {
        dir.listFiles().forEach { file ->
            val name = file.name ?: return@forEach
            if (file.isDirectory) walkTree(file, parents + name, out)
            else if (isAudio(name)) {
                out += ImportedFile(file.uri.toString(), name, parents, file.length())
            }
        }
    }

    private fun walkFiles(dir: File, parents: List<String>, out: MutableList<ImportedFile>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) walkFiles(file, parents + file.name, out)
            else if (isAudio(file.name)) {
                out += ImportedFile(file.absolutePath, file.name, parents, file.length())
            }
        }
    }

    private fun isAudio(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("mp3", "flac", "ogg", "opus", "m4a", "aac", "wav", "wma", "aiff")
    }

    private fun readMeta(file: ImportedFile): Meta {
        val suffix = file.name.substringAfterLast('.', "mp3").lowercase()
        val base = file.name.substringBeforeLast('.')
        val fromName = Regex("^(\\d{1,3})\\s*[-.]\\s*(.+)$").find(base)
        val trackHint = fromName?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val titleHint = fromName?.groupValues?.getOrNull(2) ?: base
        val albumHint = file.parents.lastOrNull()?.takeIf { it.isNotBlank() } ?: "Неизвестный альбом"
        val artistHint = file.parents.getOrNull(file.parents.lastIndex - 1)
            ?: file.parents.firstOrNull()
            ?: "Неизвестный исполнитель"
        var title = titleHint
        var artist = artistHint
        var album = albumHint
        var genre = ""
        var track = trackHint
        var duration = 0
        val retriever = MediaMetadataRetriever()
        try {
            if (file.path.startsWith("content:")) retriever.setDataSource(context, Uri.parse(file.path))
            else retriever.setDataSource(file.path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()?.takeIf { it.isNotBlank() }?.let { genre = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull()?.let { track = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { duration = (it / 1000L).toInt() }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }
        return Meta(title, artist, album, genre, track, duration, file.size, suffix)
    }

    private fun keyOf(artist: String, album: String, title: String) =
        "${artist.trim().lowercase()}|${album.trim().lowercase()}|${title.trim().lowercase()}"

    private fun writeBytes(rel: String, bytes: ByteArray, folderUri: String?): String {
        val parts = rel.split('/').filter { it.isNotBlank() }
        if (!folderUri.isNullOrBlank()) {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            if (tree != null) {
                var dir: DocumentFile = tree
                parts.dropLast(1).forEach { name ->
                    dir = dir.findFile(name) ?: dir.createDirectory(name) ?: dir
                }
                val existing = dir.findFile(parts.last())
                existing?.delete()
                val file = dir.createFile("audio/*", parts.last()) ?: error("Не удалось создать файл")
                context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
                    ?: error("Нет доступа к папке")
                return file.uri.toString()
            }
        }
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val dest = File(root, "Boombox/$rel")
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { it.write(bytes) }
        return dest.absolutePath
    }
}

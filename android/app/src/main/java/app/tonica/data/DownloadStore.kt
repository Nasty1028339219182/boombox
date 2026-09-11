package app.tonica.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class DownloadStore(private val context: Context) {
    private val metaFile get() = File(context.filesDir, "downloads.json")
    private val _tracks = MutableStateFlow(readAll())
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val tracks = _tracks.asStateFlow()
    val jobs = _jobs.asStateFlow()

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
                    .put("path", t.path),
            )
        }
        metaFile.writeText(arr.toString())
        _tracks.value = items.sortedWith(compareBy({ it.artist }, { it.album }, { it.track }))
    }

    fun saveSong(song: Song, bytes: ByteArray, folderUri: String?): DownloadedTrack {
        val artist = song.artist.ifBlank { "Неизвестный исполнитель" }
        val album = song.album.ifBlank { "Неизвестный альбом" }
        val fileName = "%02d - %s.%s".format(song.track, sanitize(song.title), song.suffix)
        val rel = "${sanitize(artist)}/${sanitize(album)}/$fileName"
        val path = writeBytes(rel, bytes, folderUri)
        val track = DownloadedTrack(
            id = song.id,
            title = song.title,
            album = album,
            albumId = song.albumId.ifBlank { song.id },
            artist = artist,
            artistId = song.artistId.ifBlank { "unknown" },
            coverArt = song.coverArt,
            track = song.track,
            duration = song.duration,
            size = bytes.size.toLong(),
            suffix = song.suffix,
            savedAt = System.currentTimeMillis(),
            path = path,
        )
        writeAll(all().filter { it.id != song.id } + track)
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
        if (isDownloaded(song.id)) return false
        if (_jobs.value.any { it.id == song.id }) return false
        _jobs.value = _jobs.value + DownloadJob(song.id, song.title, 0f)
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
    ) {
        if (!beginJob(song)) return
        try {
            val bytes = client.downloadBytes(session, song.id) { p -> updateJob(song.id, p) }
            saveSong(song, bytes, folderUri)
            finishJob(song.id)
        } catch (e: Exception) {
            updateJob(song.id, 0f, e.message ?: "Не удалось скачать")
        }
    }

    private fun writeBytes(rel: String, bytes: ByteArray, folderUri: String?): String {
        val parts = rel.split('/')
        if (!folderUri.isNullOrBlank()) {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            if (tree != null) {
                var dir = tree
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
        val dest = File(root, "Tonica/$rel")
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { it.write(bytes) }
        return dest.absolutePath
    }

    private fun sanitize(name: String): String {
        val cleaned = name.replace(Regex("[<>:\"/\\\\|?*]"), "_").trim()
        return cleaned.ifBlank { "untitled" }
    }
}

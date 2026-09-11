package app.tonica.data

data class NdSession(
    val serverUrl: String,
    val username: String,
    val name: String,
    val salt: String,
    val token: String,
    val remember: Boolean,
)

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArt: String? = null,
)

data class ArtistIndex(
    val name: String,
    val artists: List<Artist>,
)

data class Album(
    val id: String,
    val name: String,
    val artist: String = "",
    val artistId: String = "",
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val songs: List<Song> = emptyList(),
)

data class Song(
    val id: String,
    val title: String,
    val album: String = "",
    val albumId: String = "",
    val artist: String = "",
    val artistId: String = "",
    val coverArt: String? = null,
    val track: Int = 0,
    val duration: Int = 0,
    val size: Long = 0,
    val suffix: String = "mp3",
    val genre: String = "",
)

data class Genre(
    val name: String,
    val songCount: Int = 0,
    val albumCount: Int = 0,
)

data class SearchResult(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
    val genres: List<Genre> = emptyList(),
)

data class DownloadedTrack(
    val id: String,
    val title: String,
    val album: String,
    val albumId: String,
    val artist: String,
    val artistId: String,
    val coverArt: String?,
    val track: Int,
    val duration: Int,
    val size: Long,
    val suffix: String,
    val savedAt: Long,
    val path: String,
    val genre: String = "",
)

data class DownloadJob(
    val id: String,
    val title: String,
    val progress: Float,
    val error: String? = null,
)

enum class FolderLayout(val key: String, val label: String) {
    ARTIST_ALBUM("artist_album", "Исполнитель / альбом"),
    GENRE_ARTIST_ALBUM("genre_artist_album", "Жанр / исполнитель / альбом"),
    GENRE_ALBUM("genre_album", "Жанр / альбом"),
    ARTIST("artist", "Только исполнитель"),
    ALBUM("album", "Только альбом"),
    FLAT("flat", "Все файлы вместе"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.find { it.key == key } ?: ARTIST_ALBUM
    }
}

enum class FileNameStyle(val key: String, val label: String) {
    TRACK_TITLE("track_title", "01 - Название"),
    TITLE("title", "Название"),
    ARTIST_TITLE("artist_title", "Исполнитель - Название"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.find { it.key == key } ?: TRACK_TITLE
    }
}

object FolderPaths {
    fun relative(song: Song, layout: FolderLayout, fileStyle: FileNameStyle): String {
        val artist = sanitize(song.artist.ifBlank { "Неизвестный исполнитель" })
        val album = sanitize(song.album.ifBlank { "Неизвестный альбом" })
        val genre = sanitize(song.genre.ifBlank { "Без жанра" })
        val title = sanitize(song.title)
        val track = "%02d".format(if (song.track > 0) song.track else 1)
        val file = when (fileStyle) {
            FileNameStyle.TRACK_TITLE -> "$track - $title.${song.suffix}"
            FileNameStyle.TITLE -> "$title.${song.suffix}"
            FileNameStyle.ARTIST_TITLE -> "$artist - $title.${song.suffix}"
        }
        val dir = when (layout) {
            FolderLayout.ARTIST_ALBUM -> "$artist/$album"
            FolderLayout.GENRE_ARTIST_ALBUM -> "$genre/$artist/$album"
            FolderLayout.GENRE_ALBUM -> "$genre/$album"
            FolderLayout.ARTIST -> artist
            FolderLayout.ALBUM -> album
            FolderLayout.FLAT -> ""
        }
        return if (dir.isEmpty()) file else "$dir/$file"
    }

    fun sanitize(name: String): String {
        val cleaned = name.replace(Regex("[<>:\"/\\\\|?*]"), "_").trim().trim('.')
        return cleaned.ifBlank { "untitled" }
    }
}

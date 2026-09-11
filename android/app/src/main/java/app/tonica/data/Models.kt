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
)

data class SearchResult(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
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
)

data class DownloadJob(
    val id: String,
    val title: String,
    val progress: Float,
    val error: String? = null,
)

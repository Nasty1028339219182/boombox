package app.tonica

import android.app.Application
import android.os.SystemClock
import android.widget.Toast
import app.tonica.data.DownloadStore
import app.tonica.data.NavidromeClient
import app.tonica.data.NdSession
import app.tonica.data.SessionStore
import app.tonica.data.Song
import app.tonica.download.DownloadService
import app.tonica.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TonicaApp : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var client: NavidromeClient
        private set
    lateinit var downloads: DownloadStore
        private set
    lateinit var player: PlayerController
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var playLockedUntil = 0L

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        client = NavidromeClient()
        downloads = DownloadStore(this)
        player = PlayerController(this)
        appScope.launch(Dispatchers.IO) {
            runCatching { downloads.importExisting(sessionStore.folderUri.first()) }
        }
    }

    fun uriFor(session: NdSession, song: Song): String {
        return downloads.localUri(song.id) ?: client.streamUrl(session, song.id)
    }

    fun lockPlay(ms: Long = 900L) {
        playLockedUntil = SystemClock.elapsedRealtime() + ms
    }

    fun play(session: NdSession, songs: List<Song>, start: Int) {
        if (SystemClock.elapsedRealtime() < playLockedUntil) return
        player.play(songs, start) { song -> uriFor(session, song) }
    }

    fun download(session: NdSession, songs: List<Song>) {
        val added = downloads.enqueue(songs)
        if (added <= 0) return
        DownloadService.start(this)
        Toast.makeText(this, "В очередь: $added", Toast.LENGTH_SHORT).show()
    }

    fun downloadArtist(session: NdSession, artistId: String) {
        appScope.launch {
            try {
                val songs = withContext(Dispatchers.IO) {
                    val (_, albums) = client.artist(session, artistId)
                    albums.flatMap { album -> client.album(session, album.id).songs }
                }
                download(session, songs)
            } catch (e: Exception) {
                Toast.makeText(this@TonicaApp, e.message ?: "Не удалось скачать дискографию", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun importNow(): Int = withContext(Dispatchers.IO) {
        downloads.importExisting(sessionStore.folderUri.first())
    }
}

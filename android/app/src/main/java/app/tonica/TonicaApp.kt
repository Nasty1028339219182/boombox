package app.tonica

import android.app.Application
import app.tonica.data.DownloadStore
import app.tonica.data.NavidromeClient
import app.tonica.data.NdSession
import app.tonica.data.SessionStore
import app.tonica.data.Song
import app.tonica.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val downloadMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        client = NavidromeClient()
        downloads = DownloadStore(this)
        player = PlayerController(this)
    }

    fun uriFor(session: NdSession, song: Song): String {
        return downloads.localUri(song.id) ?: client.streamUrl(session, song.id)
    }

    fun play(session: NdSession, songs: List<Song>, start: Int) {
        player.play(songs, start) { song -> uriFor(session, song) }
    }

    fun download(session: NdSession, songs: List<Song>) {
        appScope.launch {
            downloadMutex.withLock {
                val folder = sessionStore.folderUri.first()
                songs.forEach { song ->
                    withContext(Dispatchers.IO) {
                        downloads.runDownload(session, client, song, folder)
                    }
                }
            }
        }
    }
}

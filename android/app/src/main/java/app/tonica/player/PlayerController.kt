package app.tonica.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.tonica.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerController(context: Context) {
    private val player = ExoPlayer.Builder(context).build()
    private var uriFor: ((Song) -> String)? = null
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private val _index = MutableStateFlow(0)
    private val _playing = MutableStateFlow(false)
    private val _current = MutableStateFlow<Song?>(null)
    val queue = _queue.asStateFlow()
    val index = _index.asStateFlow()
    val playing = _playing.asStateFlow()
    val current = _current.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playing.value = isPlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) next()
            }
        })
    }

    fun play(songs: List<Song>, start: Int, uriFor: (Song) -> String) {
        if (songs.isEmpty()) return
        this.uriFor = uriFor
        _queue.value = songs
        _index.value = start.coerceIn(0, songs.lastIndex)
        load(songs[_index.value])
        player.play()
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return
        _index.value = (_index.value + 1) % q.size
        load(q[_index.value])
        player.play()
    }

    fun prev() {
        val q = _queue.value
        if (q.isEmpty()) return
        if (player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }
        _index.value = (_index.value - 1 + q.size) % q.size
        load(q[_index.value])
        player.play()
    }

    private fun load(song: Song) {
        _current.value = song
        val uri = uriFor?.invoke(song) ?: return
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun release() = player.release()
}

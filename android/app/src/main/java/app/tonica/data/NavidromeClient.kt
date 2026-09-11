package app.tonica.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NavidromeClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun normalize(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        val withProto = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
        return withProto
    }

    fun login(serverRaw: String, username: String, password: String, remember: Boolean): NdSession {
        val server = normalize(serverRaw)
        val body = JSONObject()
            .put("username", username.trim())
            .put("password", password)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url("$server/auth/login")
            .post(body)
            .header("Accept", "application/json")
            .build()
        http.newCall(req).execute().use { res ->
            if (res.code == 401 || res.code == 403) throw IllegalStateException("Неверный логин или пароль")
            if (!res.isSuccessful) throw IllegalStateException("Сервер отклонил вход (${res.code})")
            val json = JSONObject(res.body?.string() ?: "{}")
            val salt = json.optString("subsonicSalt")
            val token = json.optString("subsonicToken")
            if (salt.isBlank() || token.isBlank()) throw IllegalStateException("Сервер не вернул Subsonic-токен")
            val session = NdSession(
                serverUrl = server,
                username = json.optString("username", username.trim()),
                name = json.optString("name", username.trim()),
                salt = salt,
                token = token,
                remember = remember,
            )
            ping(session)
            return session
        }
    }

    fun restUrl(session: NdSession, endpoint: String, extra: Map<String, String> = emptyMap()): String {
        val builder = "${session.serverUrl}/rest/$endpoint.view".toHttpUrl().newBuilder()
            .addQueryParameter("u", session.username)
            .addQueryParameter("t", session.token)
            .addQueryParameter("s", session.salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "Boombox")
            .addQueryParameter("f", "json")
        extra.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build().toString()
    }

    fun coverUrl(session: NdSession, id: String?, size: Int = 300): String? {
        if (id.isNullOrBlank()) return null
        return restUrl(session, "getCoverArt", mapOf("id" to id, "size" to size.toString()))
    }

    fun streamUrl(session: NdSession, id: String): String =
        restUrl(session, "stream", mapOf("id" to id))

    fun downloadUrl(session: NdSession, id: String): String =
        restUrl(session, "download", mapOf("id" to id))

    private fun call(session: NdSession, endpoint: String, extra: Map<String, String> = emptyMap()): JSONObject {
        val req = Request.Builder()
            .url(restUrl(session, endpoint, extra))
            .header("Accept", "application/json")
            .get()
            .build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("Сервер недоступен (${res.code})")
            val root = JSONObject(res.body?.string() ?: "{}")
            val body = root.optJSONObject("subsonic-response")
                ?: throw IllegalStateException("Пустой ответ сервера")
            if (body.optString("status") == "failed") {
                val err = body.optJSONObject("error")
                throw IllegalStateException(err?.optString("message") ?: "Ошибка Subsonic API")
            }
            return body
        }
    }

    fun ping(session: NdSession) {
        call(session, "ping")
    }

    fun artists(session: NdSession): List<ArtistIndex> {
        val body = call(session, "getArtists")
        val artists = body.optJSONObject("artists") ?: return emptyList()
        return jsonArray(artists.opt("index")).map { item ->
            ArtistIndex(
                name = item.optString("name"),
                artists = jsonArray(item.opt("artist")).map { parseArtist(it) },
            )
        }
    }

    fun artist(session: NdSession, id: String): Pair<Artist, List<Album>> {
        val body = call(session, "getArtist", mapOf("id" to id))
        val obj = body.optJSONObject("artist") ?: throw IllegalStateException("Исполнитель не найден")
        val artist = parseArtist(obj)
        val albums = jsonArray(obj.opt("album")).map { parseAlbum(it) }
        return artist to albums
    }

    fun album(session: NdSession, id: String): Album {
        val body = call(session, "getAlbum", mapOf("id" to id))
        val obj = body.optJSONObject("album") ?: throw IllegalStateException("Альбом не найден")
        return parseAlbum(obj).copy(songs = jsonArray(obj.opt("song")).map { parseSong(it) })
    }

    fun albums(session: NdSession): List<Album> {
        val out = mutableListOf<Album>()
        var offset = 0
        val page = 500
        while (offset < 4000) {
            val body = call(
                session,
                "getAlbumList2",
                mapOf("type" to "alphabeticalByName", "size" to page.toString(), "offset" to offset.toString()),
            )
            val list = jsonArray(body.optJSONObject("albumList2")?.opt("album")).map { parseAlbum(it) }
            out += list
            if (list.size < page) break
            offset += page
        }
        return out
    }

    fun albumsByGenre(session: NdSession, genre: String): List<Album> {
        val body = call(
            session,
            "getAlbumList2",
            mapOf("type" to "byGenre", "genre" to genre, "size" to "500"),
        )
        return jsonArray(body.optJSONObject("albumList2")?.opt("album")).map { parseAlbum(it) }
    }

    fun songsByGenre(session: NdSession, genre: String): List<Song> {
        val body = call(
            session,
            "getSongsByGenre",
            mapOf("genre" to genre, "count" to "500", "offset" to "0"),
        )
        return jsonArray(body.optJSONObject("songsByGenre")?.opt("song")).map { parseSong(it) }
    }

    fun genres(session: NdSession): List<Genre> {
        val body = call(session, "getGenres")
        val raw = body.optJSONObject("genres")?.opt("genre") ?: body.opt("genre")
        return jsonArray(raw).map { item ->
            Genre(
                name = item.optString("value").ifBlank { item.optString("name") },
                songCount = item.optInt("songCount"),
                albumCount = item.optInt("albumCount"),
            )
        }.filter { it.name.isNotBlank() }.sortedBy { it.name.lowercase() }
    }

    fun search(session: NdSession, query: String): SearchResult {
        if (query.isBlank()) return SearchResult()
        val body = call(
            session,
            "search3",
            mapOf("query" to query, "artistCount" to "12", "albumCount" to "12", "songCount" to "24"),
        )
        val sr = body.optJSONObject("searchResult3")
        val matched = runCatching { genres(session) }.getOrDefault(emptyList())
            .filter { it.name.contains(query, ignoreCase = true) }
            .take(16)
        val extraSongs = if (matched.size == 1) {
            runCatching { songsByGenre(session, matched.first().name).take(24) }.getOrDefault(emptyList())
        } else emptyList()
        return SearchResult(
            artists = jsonArray(sr?.opt("artist")).map { parseArtist(it) },
            albums = jsonArray(sr?.opt("album")).map { parseAlbum(it) },
            songs = (jsonArray(sr?.opt("song")).map { parseSong(it) } + extraSongs).distinctBy { it.id },
            genres = matched,
        )
    }

    fun downloadBytes(session: NdSession, songId: String, onProgress: (Float) -> Unit): ByteArray {
        val req = Request.Builder().url(downloadUrl(session, songId)).get().build()
        http.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("Не удалось скачать трек")
            val body = res.body ?: throw IllegalStateException("Пустой файл")
            val total = body.contentLength()
            val stream = body.byteStream()
            val chunks = ArrayList<ByteArray>()
            var received = 0L
            val buf = ByteArray(16 * 1024)
            while (true) {
                val n = stream.read(buf)
                if (n <= 0) break
                chunks.add(buf.copyOf(n))
                received += n
                if (total > 0) onProgress((received.toFloat() / total).coerceAtMost(0.99f))
            }
            onProgress(1f)
            val out = ByteArray(received.toInt())
            var offset = 0
            for (c in chunks) {
                System.arraycopy(c, 0, out, offset, c.size)
                offset += c.size
            }
            return out
        }
    }

    private fun parseArtist(obj: JSONObject) = Artist(
        id = obj.optString("id"),
        name = obj.optString("name"),
        albumCount = obj.optInt("albumCount"),
        coverArt = obj.optString("coverArt").ifBlank { null },
    )

    private fun parseAlbum(obj: JSONObject) = Album(
        id = obj.optString("id"),
        name = obj.optString("name"),
        artist = obj.optString("artist"),
        artistId = obj.optString("artistId"),
        coverArt = obj.optString("coverArt").ifBlank { null },
        songCount = obj.optInt("songCount"),
        duration = obj.optInt("duration"),
        year = obj.optInt("year"),
        genre = obj.optString("genre"),
    )

    private fun parseSong(obj: JSONObject) = Song(
        id = obj.optString("id"),
        title = obj.optString("title"),
        album = obj.optString("album"),
        albumId = obj.optString("albumId"),
        artist = obj.optString("artist"),
        artistId = obj.optString("artistId"),
        coverArt = obj.optString("coverArt").ifBlank { null },
        track = obj.optInt("track"),
        duration = obj.optInt("duration"),
        size = obj.optLong("size"),
        suffix = obj.optString("suffix", "mp3").ifBlank { "mp3" },
        genre = obj.optString("genre"),
    )

    private fun jsonArray(value: Any?): List<JSONObject> = when (value) {
        is JSONArray -> (0 until value.length()).mapNotNull { i -> value.optJSONObject(i) }
        is JSONObject -> listOf(value)
        else -> emptyList()
    }
}

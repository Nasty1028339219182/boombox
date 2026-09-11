package app.tonica.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("tonica")

class SessionStore(private val context: Context) {
    private val kServer = stringPreferencesKey("server")
    private val kUser = stringPreferencesKey("username")
    private val kName = stringPreferencesKey("name")
    private val kSalt = stringPreferencesKey("salt")
    private val kToken = stringPreferencesKey("token")
    private val kRemember = booleanPreferencesKey("remember")
    private val kFolder = stringPreferencesKey("folder")
    private val kMini = booleanPreferencesKey("mini_player")
    private val ephemeral = MutableStateFlow<NdSession?>(null)

    private val persisted: Flow<NdSession?> = context.dataStore.data.map { prefs ->
        val server = prefs[kServer] ?: return@map null
        val user = prefs[kUser] ?: return@map null
        val token = prefs[kToken] ?: return@map null
        val salt = prefs[kSalt] ?: return@map null
        NdSession(
            serverUrl = server,
            username = user,
            name = prefs[kName] ?: user,
            salt = salt,
            token = token,
            remember = prefs[kRemember] ?: true,
        )
    }

    val session: Flow<NdSession?> = combine(persisted, ephemeral) { stored, live -> live ?: stored }

    val folderUri: Flow<String?> = context.dataStore.data.map { it[kFolder] }

    val showMiniPlayer: Flow<Boolean> = context.dataStore.data.map { it[kMini] != false }

    suspend fun current(): NdSession? = session.first()

    suspend fun save(session: NdSession) {
        ephemeral.value = session
        if (!session.remember) return
        context.dataStore.edit { prefs ->
            prefs[kServer] = session.serverUrl
            prefs[kUser] = session.username
            prefs[kName] = session.name
            prefs[kSalt] = session.salt
            prefs[kToken] = session.token
            prefs[kRemember] = true
        }
    }

    suspend fun clear() {
        ephemeral.value = null
        context.dataStore.edit { prefs ->
            prefs.remove(kServer)
            prefs.remove(kUser)
            prefs.remove(kName)
            prefs.remove(kSalt)
            prefs.remove(kToken)
            prefs.remove(kRemember)
        }
    }

    suspend fun setFolder(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(kFolder) else prefs[kFolder] = uri
        }
    }

    suspend fun setShowMiniPlayer(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[kMini] = value }
    }
}

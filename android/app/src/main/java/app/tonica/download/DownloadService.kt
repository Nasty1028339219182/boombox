package app.tonica.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.tonica.MainActivity
import app.tonica.TonicaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DownloadService : Service() {
    companion object {
        private const val CHANNEL = "boombox_downloads"
        private const val CHANNEL_SILENT = "boombox_downloads_silent"
        private const val NOTIF_ID = 42

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var showNotify = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannels()
        startAsForeground("Boombox", "Готовим загрузку…")
        scope.launch {
            showNotify = (application as TonicaApp).sessionStore.currentNotify()
            startAsForeground("Boombox", "Готовим загрузку…")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            mutex.withLock { processQueue() }
        }
        return START_STICKY
    }

    private suspend fun processQueue() {
        val app = application as TonicaApp
        try {
            showNotify = app.sessionStore.currentNotify()
            val session = app.sessionStore.current() ?: return
            val layout = app.sessionStore.currentLayout()
            val style = app.sessionStore.currentFileStyle()
            val folder = app.sessionStore.folderUri.first()
            while (true) {
                val song = app.downloads.takeNext() ?: break
                val left = (app.downloads.pendingCount() + 1).coerceAtLeast(1)
                startAsForeground(
                    "Скачивается: ${song.title}",
                    if (left > 1) "В очереди ещё ${left - 1}" else "Последний трек в очереди",
                )
                app.downloads.runDownload(session, app.client, song, folder, layout, style)
            }
        } catch (_: Exception) {
        } finally {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startAsForeground(title: String, text: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, notification(title, text), type)
    }

    private fun notification(title: String, text: String): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, if (showNotify) CHANNEL else CHANNEL_SILENT)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(launch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(!showNotify)
            .setPriority(if (showNotify) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Загрузки Boombox", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Ход скачивания музыки"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENT, "Загрузки (тихо)", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Служебное уведомление, пока идёт фоновая загрузка"
                setShowBadge(false)
            },
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

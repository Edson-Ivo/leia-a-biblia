package com.edsonivo.m633.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.edsonivo.m633.MainActivity
import com.edsonivo.m633.R
import com.edsonivo.m633.app
import com.edsonivo.m633.core.DayStatus
import com.edsonivo.m633.system.Apps
import com.edsonivo.m633.system.Permissions
import com.edsonivo.m633.system.Usage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Vigia em primeiro plano. Só consulta o app da frente enquanto a tela está ligada,
 * o dia está pendente e não há silêncio valendo. Fora disso dorme e só reavalia
 * de tempos em tempos, quando a tela liga ou quando o relógio muda.
 */
class GuardService : LifecycleService(), SavedStateRegistryOwner {

    private val savedState = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private lateinit var overlay: Overlay
    private lateinit var tracker: Usage.ForegroundTracker
    private var protectedCache: Set<String> = emptySet()
    private var protectedAt = 0L
    private var lastNotice: String? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wake()
        }
    }

    override fun onCreate() {
        savedState.performAttach()
        savedState.performRestore(null)
        super.onCreate()
        overlay = Overlay(this)
        tracker = Usage.ForegroundTracker(this)
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTICE_ID,
            notice(getString(R.string.app_name), "O lembrete de hoje está ativo."),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        running = this
        lifecycleScope.launch { loop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        wake()
        return START_STICKY
    }

    override fun onDestroy() {
        running = null
        overlay.hide()
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }

    private suspend fun loop() {
        val power = getSystemService(PowerManager::class.java)
        while (lifecycleScope.isActive) {
            val store = app.store
            val state = store.fresh()
            if (!state.onboarded) {
                overlay.hide()
                stopSelf()
                return
            }
            val status = store.status()
            val permsOk = Permissions.required(this)
            updateNotice(status, permsOk, state.silenceUntil)

            val interactive = power.isInteractive
            val watching = interactive && permsOk && (status == DayStatus.OPEN || overlay.showing)
            val wait: Long = if (watching) {
                val fg = tracker.poll()
                val blocked = fg != null && fg in state.blocked && fg !in protectedPackages(state.biblePackage)
                if (status == DayStatus.OPEN && blocked) {
                    overlay.show()
                } else if (fg != null && !blocked && overlay.showing) {
                    overlay.hide()
                }
                POLL_MS
            } else {
                val untilSilenceEnd = state.silenceUntil?.let { it - System.currentTimeMillis() } ?: Long.MAX_VALUE
                minOf(IDLE_MS, maxOf(1_000L, untilSilenceEnd))
            }
            withTimeoutOrNull(wait) { wakeups.receive() }
        }
    }

    private fun protectedPackages(bible: String?): Set<String> {
        val now = System.currentTimeMillis()
        if (now - protectedAt > 60_000L) {
            protectedCache = Apps.protectedReasons(this, bible).keys
            protectedAt = now
        }
        return protectedCache
    }

    private fun updateNotice(status: DayStatus, permsOk: Boolean, silenceUntil: Long?) {
        val text = when {
            !permsOk -> "O lembrete não está ativo. Falta uma permissão."
            status == DayStatus.DONE -> "Hoje você já leu. Até a meia-noite não perguntamos de novo."
            status == DayStatus.SILENT -> "Silenciado até ${HOUR.format(Instant.ofEpochMilli(silenceUntil ?: 0).atZone(ZoneId.systemDefault()))}. Não passa da meia-noite."
            else -> "O lembrete de hoje está ativo."
        }
        if (text == lastNotice) return
        lastNotice = text
        getSystemService(NotificationManager::class.java)
            .notify(NOTICE_ID, notice(getString(R.string.app_name), text))
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL, "Lembrete do dia", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Aviso fixo enquanto o lembrete está trabalhando."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notice(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notice)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL = "lembrete"
        private const val NOTICE_ID = 633
        private const val POLL_MS = 700L
        private const val IDLE_MS = 60_000L
        private val HOUR = DateTimeFormatter.ofPattern("HH:mm")
        private val wakeups = Channel<Unit>(Channel.CONFLATED)

        @Volatile
        var running: GuardService? = null
            private set

        fun wake() {
            wakeups.trySend(Unit)
        }

        fun start(context: Context) {
            if (!context.app.store.state.value.onboarded) return
            if (running != null) {
                wake()
                return
            }
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, GuardService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GuardService::class.java))
        }
    }
}

class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GuardService.start(context)
    }
}

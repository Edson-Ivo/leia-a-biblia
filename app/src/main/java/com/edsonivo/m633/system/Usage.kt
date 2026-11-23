package com.edsonivo.m633.system

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Usage {

    private fun manager(context: Context) =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * Tempo real em primeiro plano do pacote desde a meia-noite local de hoje, em milissegundos.
     * Conta pelos eventos de atividade, para não depender dos baldes diários do sistema,
     * que não começam na meia-noite local.
     */
    fun foregroundTodayMs(context: Context, pkg: String, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val start = LocalDate.ofInstant(now, zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = now.toEpochMilli()
        val events = runCatching { manager(context).queryEvents(start, end) }.getOrNull() ?: return 0L
        val open = mutableSetOf<String>()
        var since = -1L
        var total = 0L
        var sawAny = false
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.packageName != pkg) continue
            val cls = e.className ?: ""
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (open.isEmpty()) since = e.timeStamp
                    open += cls
                    sawAny = true
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (!sawAny && open.isEmpty()) {
                        total += e.timeStamp - start
                    } else if (open.remove(cls) && open.isEmpty() && since >= 0) {
                        total += e.timeStamp - since
                        since = -1
                    }
                    sawAny = true
                }
            }
        }
        if (open.isNotEmpty() && since >= 0) total += end - since
        return total
    }

    /** Acompanha qual app veio para a frente por último, lendo só os eventos novos a cada consulta. */
    class ForegroundTracker(private val context: Context) {
        private var lastQuery = 0L
        var current: String? = null
            private set

        fun poll(): String? {
            val now = System.currentTimeMillis()
            val from = if (lastQuery == 0L) now - 60 * 60_000L else lastQuery - 2_000L
            val events = runCatching { manager(context).queryEvents(from, now) }.getOrNull() ?: return current
            val e = UsageEvents.Event()
            var latestTs = -1L
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED && e.timeStamp >= latestTs) {
                    latestTs = e.timeStamp
                    current = e.packageName
                }
            }
            lastQuery = now
            return current
        }
    }
}

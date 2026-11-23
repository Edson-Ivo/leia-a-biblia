package com.edsonivo.m633.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Tudo fica neste aparelho, no armazenamento privado do app. */
class Store(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("m633", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun zone(): ZoneId = ZoneId.systemDefault()
    fun now(): Instant = Instant.now()
    fun today(): LocalDate = LocalDate.ofInstant(now(), zone())

    /** Lê de novo a data local e aplica as regras do dia antes de devolver. */
    @Synchronized
    fun fresh(): AppState {
        val reconciled = DayRules.reconcile(_state.value, today())
        if (reconciled != _state.value) write(reconciled)
        return _state.value
    }

    fun status(): DayStatus = DayRules.status(fresh(), today(), now().toEpochMilli())

    @Synchronized
    fun update(block: (AppState) -> AppState) {
        val base = DayRules.reconcile(_state.value, today())
        write(block(base))
    }

    private fun write(s: AppState) {
        prefs.edit()
            .putBoolean("onboarded", s.onboarded)
            .putString("biblePackage", s.biblePackage)
            .putString("bibleLabel", s.bibleLabel)
            .putStringSet("blocked", s.blocked)
            .putString("theme", s.theme.name)
            .putInt("minReadMinutes", s.minReadMinutes)
            .putString("lastCompleted", s.lastCompleted?.toString())
            .putInt("streak", s.streak)
            .putInt("best", s.best)
            .putLong("silenceUntil", s.silenceUntil ?: -1L)
            .putString("silenceDate", s.silenceDate?.toString())
            .putInt("lastCustomMinutes", s.lastCustomMinutes ?: -1)
            .putInt("lastSeenStage", s.lastSeenStage)
            .putInt("talkIndex", s.talkIndex)
            .putInt("pendingCelebration", s.pendingCelebration)
            .commit()
        _state.value = s
    }

    private fun read(): AppState = AppState(
        onboarded = prefs.getBoolean("onboarded", false),
        biblePackage = prefs.getString("biblePackage", null),
        bibleLabel = prefs.getString("bibleLabel", null),
        blocked = prefs.getStringSet("blocked", emptySet())!!.toSet(),
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }
            .getOrDefault(ThemeMode.SYSTEM),
        minReadMinutes = prefs.getInt("minReadMinutes", 5),
        lastCompleted = prefs.getString("lastCompleted", null)?.let(LocalDate::parse),
        streak = prefs.getInt("streak", 0),
        best = prefs.getInt("best", 0),
        silenceUntil = prefs.getLong("silenceUntil", -1L).takeIf { it > 0 },
        silenceDate = prefs.getString("silenceDate", null)?.let(LocalDate::parse),
        lastCustomMinutes = prefs.getInt("lastCustomMinutes", -1).takeIf { it > 0 },
        lastSeenStage = prefs.getInt("lastSeenStage", 0),
        talkIndex = prefs.getInt("talkIndex", 0),
        pendingCelebration = prefs.getInt("pendingCelebration", 0),
    )
}

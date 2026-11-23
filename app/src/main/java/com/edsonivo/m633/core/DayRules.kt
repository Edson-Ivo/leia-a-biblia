package com.edsonivo.m633.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class DayStatus { OPEN, SILENT, DONE }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppState(
    val onboarded: Boolean = false,
    val biblePackage: String? = null,
    val bibleLabel: String? = null,
    val blocked: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val minReadMinutes: Int = 5,
    val lastCompleted: LocalDate? = null,
    val streak: Int = 0,
    val best: Int = 0,
    val silenceUntil: Long? = null,
    val silenceDate: LocalDate? = null,
    val lastCustomMinutes: Int? = null,
    val lastSeenStage: Int = 0,
    val talkIndex: Int = 0,
    val pendingCelebration: Int = 0,
)

data class SilenceChoice(val minutes: Int?, val kind: Kind) {
    enum class Kind { LAST, PRESET, CUSTOM }
}

/** Regras do dia de calendário local. Nada aqui depende de Android. */
object DayRules {

    fun nextMidnight(now: Instant, zone: ZoneId): Instant =
        LocalDate.ofInstant(now, zone).plusDays(1).atStartOfDay(zone).toInstant()

    fun maxSilenceMinutes(now: Instant, zone: ZoneId): Int =
        ((nextMidnight(now, zone).toEpochMilli() - now.toEpochMilli()) / 60_000L).toInt()

    /** Zera a sequência quando um dia ficou sem leitura e mata o silêncio de outra data. */
    fun reconcile(state: AppState, today: LocalDate): AppState {
        var next = state
        val last = state.lastCompleted
        if (last != null && last != today && last != today.minusDays(1) && state.streak != 0) {
            next = next.copy(streak = 0)
        }
        if (state.silenceDate != null && state.silenceDate != today) {
            next = next.copy(silenceUntil = null, silenceDate = null)
        }
        return next
    }

    fun silenceActive(state: AppState, today: LocalDate, nowMs: Long): Boolean {
        val until = state.silenceUntil ?: return false
        return state.silenceDate == today && nowMs < until
    }

    fun status(state: AppState, today: LocalDate, nowMs: Long): DayStatus = when {
        state.lastCompleted == today -> DayStatus.DONE
        silenceActive(state, today, nowMs) -> DayStatus.SILENT
        else -> DayStatus.OPEN
    }

    /** Sequência visível hoje: só conta se o último dia concluído foi hoje ou ontem. */
    fun currentStreak(state: AppState, today: LocalDate): Int {
        val last = state.lastCompleted ?: return 0
        return if (last == today || last == today.minusDays(1)) state.streak else 0
    }

    fun complete(state: AppState, today: LocalDate): AppState {
        if (state.lastCompleted == today) return state
        val streak = if (state.lastCompleted == today.minusDays(1)) state.streak + 1 else 1
        val milestone = Milestones.reachedOn(streak)
        return state.copy(
            lastCompleted = today,
            streak = streak,
            best = maxOf(state.best, streak),
            silenceUntil = null,
            silenceDate = null,
            pendingCelebration = milestone?.days ?: state.pendingCelebration,
        )
    }

    fun silenceChoices(lastCustom: Int?, maxMinutes: Int): List<SilenceChoice> {
        val out = mutableListOf<SilenceChoice>()
        if (lastCustom != null && lastCustom !in PRESETS && lastCustom in 1..maxMinutes) {
            out += SilenceChoice(lastCustom, SilenceChoice.Kind.LAST)
        }
        PRESETS.filter { it <= maxMinutes }.forEach { out += SilenceChoice(it, SilenceChoice.Kind.PRESET) }
        if (maxMinutes >= 1) out += SilenceChoice(null, SilenceChoice.Kind.CUSTOM)
        return out
    }

    /**
     * Começa (ou troca) o silêncio. Devolve null quando o pedido passaria da meia-noite.
     * [custom] indica um valor digitado, que passa a ser o último customizado.
     */
    fun startSilence(
        state: AppState,
        minutes: Int,
        custom: Boolean,
        now: Instant,
        zone: ZoneId,
    ): AppState? {
        val max = maxSilenceMinutes(now, zone)
        if (minutes < 1 || minutes > max) return null
        val today = LocalDate.ofInstant(now, zone)
        if (state.lastCompleted == today) return null
        val until = minOf(now.toEpochMilli() + minutes * 60_000L, nextMidnight(now, zone).toEpochMilli())
        return state.copy(
            silenceUntil = until,
            silenceDate = today,
            lastCustomMinutes = if (custom) minutes else state.lastCustomMinutes,
        )
    }

    fun endSilence(state: AppState): AppState = state.copy(silenceUntil = null, silenceDate = null)

    fun formatMinutes(total: Int): String {
        val h = total / 60
        val m = total % 60
        return when {
            h > 0 && m > 0 -> "$h h $m min"
            h == 1 -> "1 hora"
            h > 1 -> "$h horas"
            else -> "$m min"
        }
    }

    val PRESETS = listOf(5, 10, 30)
}

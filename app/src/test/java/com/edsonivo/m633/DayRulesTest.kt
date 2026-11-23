package com.edsonivo.m633

import com.edsonivo.m633.core.AppState
import com.edsonivo.m633.core.DayRules
import com.edsonivo.m633.core.DayStatus
import com.edsonivo.m633.core.MessageBank
import com.edsonivo.m633.core.SilenceChoice
import com.edsonivo.m633.core.Tree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayRulesTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val day1 = LocalDate.of(2026, 11, 20)
    private fun at(date: LocalDate, h: Int, m: Int) = LocalDateTime.of(date, java.time.LocalTime.of(h, m)).atZone(zone).toInstant()

    @Test
    fun completionLastsOnlyUntilLocalMidnight() {
        val done = DayRules.complete(AppState(), day1)
        assertEquals(DayStatus.DONE, DayRules.status(done, day1, at(day1, 23, 59).toEpochMilli()))
        val day2 = day1.plusDays(1)
        assertEquals(DayStatus.OPEN, DayRules.status(DayRules.reconcile(done, day2), day2, at(day2, 0, 1).toEpochMilli()))
    }

    @Test
    fun streakGrowsOnConsecutiveDaysAndResetsAfterAMissedDay() {
        var s = DayRules.complete(AppState(), day1)
        s = DayRules.complete(s, day1.plusDays(1))
        assertEquals(2, s.streak)
        val day4 = day1.plusDays(3)
        s = DayRules.reconcile(s, day4)
        assertEquals(0, s.streak)
        assertEquals(2, s.best)
        s = DayRules.complete(s, day4)
        assertEquals(1, s.streak)
        assertEquals(2, s.best)
    }

    @Test
    fun completingTwiceTheSameDayDoesNotCountTwice() {
        val s = DayRules.complete(DayRules.complete(AppState(), day1), day1)
        assertEquals(1, s.streak)
    }

    @Test
    fun silenceNeverCrossesMidnight() {
        val now = at(day1, 23, 50)
        assertEquals(10, DayRules.maxSilenceMinutes(now, zone))
        assertNull(DayRules.startSilence(AppState(), 20, false, now, zone))
        val ok = DayRules.startSilence(AppState(), 10, false, now, zone)!!
        assertEquals(DayRules.nextMidnight(now, zone).toEpochMilli(), ok.silenceUntil)
    }

    @Test
    fun threeHoursAt2140DoesNotFit() {
        val now = at(day1, 21, 40)
        assertEquals(140, DayRules.maxSilenceMinutes(now, zone))
        assertNull(DayRules.startSilence(AppState(), 180, true, now, zone))
    }

    @Test
    fun silenceFromYesterdayIsDeadToday() {
        val s = DayRules.startSilence(AppState(), 30, false, at(day1, 23, 0), zone)!!
        val day2 = day1.plusDays(1)
        val next = DayRules.reconcile(s, day2)
        assertNull(next.silenceUntil)
        assertEquals(DayStatus.OPEN, DayRules.status(next, day2, at(day2, 0, 5).toEpochMilli()))
    }

    @Test
    fun choicesHideWhatDoesNotFitAndKeepLastCustom() {
        val c = DayRules.silenceChoices(lastCustom = 75, maxMinutes = 20)
        assertEquals(listOf(5, 10, null), c.map { it.minutes })
        val d = DayRules.silenceChoices(lastCustom = 75, maxMinutes = 600)
        assertEquals(SilenceChoice.Kind.LAST, d.first().kind)
        assertEquals(75, d.first().minutes)
        val presetDoesNotOverwrite = DayRules.startSilence(AppState(lastCustomMinutes = 75), 5, false, at(day1, 10, 0), zone)!!
        assertEquals(75, presetDoesNotOverwrite.lastCustomMinutes)
    }

    @Test
    fun noSilenceInTheLastMinute() {
        val now = at(day1, 23, 59).plusSeconds(30)
        assertEquals(0, DayRules.maxSilenceMinutes(now, zone))
        assertTrue(DayRules.silenceChoices(null, 0).isEmpty())
    }

    @Test
    fun treeReachesSproutOnDaySevenAndIlluminatedAfterFiveYears() {
        assertEquals("Semente", Tree.growthFor(6).stage.name)
        assertEquals("Broto", Tree.growthFor(7).stage.name)
        assertEquals("Aurora", Tree.growthFor(1824).stage.name)
        assertEquals("Árvore Iluminada", Tree.growthFor(1825).stage.name)
        assertNull(Tree.growthFor(4000).next)
    }

    @Test
    fun messageBankHasFourThemesWithVerses() {
        val file = listOf("src/main/assets/mensagens.json", "app/src/main/assets/mensagens.json").map(::File).first { it.exists() }
        val bank = MessageBank.parse(file.readText())
        assertEquals(setOf("ler", "orar", "igreja", "comunhao"), bank.themes.keys)
        bank.themes.keys.forEach { t -> assertTrue(bank.messages.count { it.theme == t } >= 25) }
        bank.messages.forEach { m ->
            assertTrue(m.verses.isNotEmpty())
            m.verses.forEach { v -> assertTrue(v.ref, Regex("""^[1-3]? ?[A-ZÁÉÍÓÚÊÔÂa-záéíóúêôâãõç ]+ \d+:\d+(-\d+)?$""").matches(v.ref)) }
        }
        assertNotNull(bank.ofDay(20000))
    }
}

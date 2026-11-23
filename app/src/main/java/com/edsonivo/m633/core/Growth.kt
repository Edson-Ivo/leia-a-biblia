package com.edsonivo.m633.core

import androidx.annotation.DrawableRes
import com.edsonivo.m633.R

data class Stage(val index: Int, val startDay: Int, val name: String, @DrawableRes val image: Int)

data class Growth(
    val stage: Stage,
    val next: Stage?,
    /** Dias desde o início da fase atual. */
    val day: Int,
    /** Dias que a fase dura até a próxima. */
    val span: Int,
) {
    /** De 0 a 1: quanto a fase já cresceu. O desenho muda a cada dia. */
    val amount: Float get() = if (span <= 1) 1f else day.toFloat() / (span - 1)
    val daysToNext: Int? get() = next?.let { it.startDay - (stage.startDay + day) }
}

/** Da semente à Árvore Iluminada em cinco anos de sequência (1825 dias). */
object Tree {
    val STAGES = listOf(
        Stage(0, 0, "Semente", R.drawable.fase_01_semente),
        Stage(1, 7, "Broto", R.drawable.fase_02_broto),
        Stage(2, 14, "Muda", R.drawable.fase_03_muda),
        Stage(3, 30, "Arvoreta", R.drawable.fase_04_arvoreta),
        Stage(4, 60, "Botões", R.drawable.fase_05_botoes),
        Stage(5, 90, "Florada", R.drawable.fase_06_florada),
        Stage(6, 180, "Primeiros frutos", R.drawable.fase_07_primeiros_frutos),
        Stage(7, 270, "Frutífera", R.drawable.fase_08_frutifera),
        Stage(8, 365, "Frondosa", R.drawable.fase_09_frondosa),
        Stage(9, 540, "Copa dourada", R.drawable.fase_10_copa_dourada),
        Stage(10, 730, "Lamparinas", R.drawable.fase_11_lamparinas),
        Stage(11, 1095, "Aurora", R.drawable.fase_12_aurora),
        Stage(12, 1825, "Árvore Iluminada", R.drawable.fase_13_arvore_iluminada),
    )

    fun growthFor(streak: Int): Growth {
        val s = maxOf(0, streak)
        val stage = STAGES.last { s >= it.startDay }
        val next = STAGES.getOrNull(stage.index + 1)
        val span = if (next != null) next.startDay - stage.startDay else 1
        val day = (s - stage.startDay).coerceIn(0, maxOf(0, span - 1))
        return Growth(stage, next, day, span)
    }
}

data class Milestone(val days: Int, val label: String, val tier: Int)

object Milestones {
    val ALL = listOf(
        Milestone(7, "Uma semana", 0),
        Milestone(30, "Um mês", 1),
        Milestone(180, "Um semestre", 2),
        Milestone(365, "Um ano", 3),
        Milestone(1825, "Cinco anos", 4),
        Milestone(3650, "Dez anos", 5),
    )

    fun reachedOn(streak: Int): Milestone? = ALL.lastOrNull { streak == it.days }

    fun byDays(days: Int): Milestone? = ALL.firstOrNull { it.days == days }

    fun message(m: Milestone): String = when (m.days) {
        7 -> "Uma semana inteira na Palavra. Sete dias seguidos. Que bom que você ficou."
        30 -> "Um mês completo. Trinta dias meditando na Bíblia. Deus abençoe essa constância, e que a vontade dEle seja feita."
        180 -> "Seis meses sem largar a Escritura. Parabéns, de coração. Pouca coisa boa se sustenta tanto tempo. A sua se sustentou."
        365 -> "Um ano inteiro. Trezentos e sessenta e cinco dias na Bíblia. O Senhor abençoe a sua vida, a sua casa e o dia de amanhã."
        1825 -> "Cinco anos. A sua árvore está iluminada. Uma árvore assim não aparece numa semana. Glória a Deus por cada um desses dias."
        else -> "Dez anos. Dia após dia, a Palavra ficou com você. Isso é uma vida, não um esforço de semana. Parabéns."
    }
}

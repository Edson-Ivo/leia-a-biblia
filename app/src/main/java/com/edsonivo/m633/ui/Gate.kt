package com.edsonivo.m633.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edsonivo.m633.app
import com.edsonivo.m633.core.DayRules
import com.edsonivo.m633.core.Store
import com.edsonivo.m633.guard.GuardService
import com.edsonivo.m633.system.Apps
import com.edsonivo.m633.system.Usage
import com.edsonivo.m633.ui.theme.LocalExtra
import kotlinx.coroutines.delay

/** O que a pergunta precisa do mundo lá fora. O overlay e a tela do app usam a mesma. */
abstract class GateActions(protected val context: Context) {
    val store: Store get() = context.app.store

    fun bibleLabel(): String {
        val s = store.state.value
        return s.biblePackage?.let { Apps.label(context, it) } ?: s.bibleLabel ?: "app da Bíblia"
    }

    fun minMinutes(): Int = store.state.value.minReadMinutes

    fun readMs(): Long = store.state.value.biblePackage?.let { Usage.foregroundTodayMs(context, it) } ?: 0L

    fun maxSilence(): Int = DayRules.maxSilenceMinutes(store.now(), store.zone())

    fun lastCustom(): Int? = store.state.value.lastCustomMinutes

    /** Grava o dia no instante do segundo Sim, antes de qualquer outro toque. */
    fun complete() {
        store.update { DayRules.complete(it, store.today()) }
        GuardService.wake()
    }

    fun silence(minutes: Int, custom: Boolean): Boolean {
        var ok = false
        store.update { s ->
            DayRules.startSilence(s, minutes, custom, store.now(), store.zone())?.also { ok = true } ?: s
        }
        GuardService.wake()
        return ok
    }

    abstract fun openBible(): Boolean
    abstract fun chooseOtherBible()
    abstract fun close()
}

private enum class Step { ASK, NOT_READ, TRUTH, BLESS, FORGIVE, PRIORITY, SILENCE, MISSING }

@Composable
fun GateFlow(actions: GateActions, inApp: Boolean) {
    var step by remember { mutableStateOf(Step.ASK) }
    var readMinutes by remember { mutableStateOf(0L) }
    val bible = remember { actions.bibleLabel() }
    val muted = LocalExtra.current.muted

    fun read() {
        if (!actions.openBible()) step = Step.MISSING
    }

    Box(Modifier.fillMaxSize()) {
        if (inApp && step != Step.BLESS) {
            IconButton(onClick = actions::close, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Fechar")
            }
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (step) {
                Step.ASK -> {
                    Phrase("Você já leu a Bíblia hoje?")
                    Gap()
                    Primary("Sim") {
                        val ms = actions.readMs()
                        readMinutes = ms / 60_000L
                        step = if (ms >= actions.minMinutes() * 60_000L) Step.TRUTH else Step.NOT_READ
                    }
                    Secondary("Não") { step = Step.PRIORITY }
                }
                Step.NOT_READ -> {
                    Phrase("Ainda não encontramos a sua leitura de hoje.")
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No $bible: $readMinutes de ${actions.minMinutes()} min hoje.",
                        color = muted, textAlign = TextAlign.Center,
                    )
                    Text("Abrindo o $bible…", color = muted, textAlign = TextAlign.Center)
                    Gap()
                    Primary("Vou ler agora!") { read() }
                    LaunchedEffect(Unit) {
                        delay(3_500)
                        read()
                    }
                }
                Step.TRUTH -> {
                    Phrase("Você fala a verdade?")
                    Gap()
                    Primary("Sim") {
                        actions.complete()
                        step = Step.BLESS
                    }
                    Secondary("Não") { step = Step.FORGIVE }
                }
                Step.BLESS -> {
                    Phrase("Deus abençoe seu dia e que a vontade dEle seja feita!")
                    Gap()
                    Primary("Continuar") { actions.close() }
                }
                Step.FORGIVE -> {
                    Phrase("Saiba que Deus perdoa esse seu pecado e que ainda há tempo de mudar isso, então leia e mude!")
                    Gap()
                    Primary("Vou ler agora!") { read() }
                }
                Step.PRIORITY -> {
                    Phrase("Priorize a verdade da vida no seu dia, não deixe para depois!")
                    Gap()
                    Primary("Vou ler agora!") { read() }
                    if (actions.maxSilence() >= 1) {
                        Secondary("Silenciar") { step = Step.SILENCE }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "O dia acaba em menos de um minuto. O silêncio não passa da meia-noite.",
                            color = muted, textAlign = TextAlign.Center,
                        )
                    }
                }
                Step.SILENCE -> SilencePicker(
                    lastCustom = actions.lastCustom(),
                    maxMinutes = { actions.maxSilence() },
                    onPick = { minutes, custom -> if (actions.silence(minutes, custom)) actions.close() },
                    onCancel = { step = Step.PRIORITY },
                )
                Step.MISSING -> {
                    Phrase("O app da Bíblia que você escolheu não está mais neste celular.")
                    Gap()
                    Primary("Escolher outro") { actions.chooseOtherBible() }
                }
            }
        }
    }
}

@Composable
fun Phrase(text: String) {
    Text(
        text,
        fontSize = 26.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun Gap() = Spacer(Modifier.height(36.dp))

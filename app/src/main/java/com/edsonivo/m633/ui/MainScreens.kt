package com.edsonivo.m633.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edsonivo.m633.core.AppState
import com.edsonivo.m633.core.DayStatus
import com.edsonivo.m633.core.Message
import com.edsonivo.m633.core.Milestones
import com.edsonivo.m633.core.Tree
import com.edsonivo.m633.ui.theme.LocalExtra
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))

fun days(n: Int) = if (n == 1) "1 dia" else "$n dias"

@Composable
fun HomeScreen(
    state: AppState,
    status: DayStatus,
    streak: Int,
    shownStage: Int,
    today: LocalDate,
    verse: Message,
    readMinutes: Long,
    bibleLabel: String,
    missing: List<String>,
    onRead: () -> Unit,
    onAlreadyRead: () -> Unit,
    onSilence: () -> Unit,
    onEndSilence: () -> Unit,
    onFixPermissions: () -> Unit,
) {
    val muted = LocalExtra.current.muted
    val growth = Tree.growthFor(streak)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Muted("Você está na fase", size = 15)
        Text(growth.stage.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        TreeImage(streak = streak, shownStage = shownStage, size = 184.dp)
        Spacer(Modifier.height(10.dp))
        val v = verse.verses.first()
        Text(
            "“${v.text}”",
            fontSize = 15.sp, lineHeight = 21.sp, fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center, maxLines = 4,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Text(v.ref, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = muted, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(22.dp))
        Text(DATE.format(today).replaceFirstChar { it.uppercase() }, fontSize = 15.sp, color = muted)
        Spacer(Modifier.height(4.dp))
        Text(
            "Sequência: ${days(streak)}  ·  Melhor: ${days(state.best)}",
            fontSize = 15.sp, fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(22.dp))
        Primary("Ler a Palavra", onClick = onRead)
        Spacer(Modifier.height(6.dp))
        when (status) {
            DayStatus.OPEN -> {
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onSilence) { Text("Silenciar") }
                    TextButton(onClick = onAlreadyRead) { Text("Já li hoje") }
                }
                Muted("No $bibleLabel hoje: $readMinutes de ${state.minReadMinutes} min", size = 13)
            }
            DayStatus.SILENT -> {
                Muted("Silenciado até ${hourOf(state.silenceUntil ?: 0)}. Não passa da meia-noite.", size = 13)
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onSilence) { Text("Alterar") }
                    TextButton(onClick = onEndSilence) { Text("Encerrar") }
                }
            }
            DayStatus.DONE -> Muted("Hoje você já leu. Até a meia-noite não perguntamos de novo.", size = 13)
        }
        if (missing.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "O lembrete não está ativo. Falta ${missing.joinToString(" e ")}.",
                color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center,
            )
            TextButton(onClick = onFixPermissions) { Text("Resolver") }
        }
    }
}

@Composable
fun ProgressScreen(state: AppState, streak: Int, shownStage: Int) {
    val muted = LocalExtra.current.muted
    val growth = Tree.growthFor(streak)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(growth.stage.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        TreeImage(streak = streak, shownStage = shownStage, size = 150.dp)
        val next = growth.next
        if (next != null) {
            Spacer(Modifier.height(6.dp))
            NextSilhouette(next.index, 84.dp)
            Spacer(Modifier.height(6.dp))
            Muted("Próxima: ${next.name}, em ${days(growth.daysToNext ?: 0)}", size = 13)
        } else {
            Muted("Você chegou à última fase.", size = 13)
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$streak", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Muted("Sequência atual", size = 13)
            }
            Spacer(Modifier.width(48.dp))
            val accent = MaterialTheme.colorScheme.secondary
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${state.best}",
                    fontSize = 44.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.drawBehind {
                        val y = size.height - 4.dp.toPx()
                        drawLine(accent, Offset(0f, y), Offset(size.width, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    },
                )
                Spacer(Modifier.height(4.dp))
                Text("Melhor sequência", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Muted("Marcos", size = 13)
        Spacer(Modifier.height(8.dp))
        Milestones.ALL.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { m ->
                    val reached = state.best >= m.days
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                        Icon(
                            if (reached) Icons.Filled.CheckCircle else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (reached) MaterialTheme.colorScheme.primary else muted,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(m.label, fontSize = 12.sp, color = if (reached) MaterialTheme.colorScheme.onBackground else muted)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(version: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Sobre", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("M 6:33 é Mateus, capítulo 6, versículo 33.", fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Text(
            "“Mas buscai primeiro o reino de Deus, e a sua justiça, e todas estas coisas vos serão acrescentadas.”",
            fontSize = 17.sp, lineHeight = 25.sp, fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.height(4.dp))
        Muted("Mateus 6:33", align = TextAlign.Start)
        AboutBlock(
            "Primeiro a Palavra",
            "Você escolhe os apps que costumam roubar o seu tempo. Quando um deles abre e você ainda não leu a Bíblia hoje, o M 6:33 pergunta antes. Tudo recomeça à meia-noite.",
        )
        AboutBlock(
            "A leitura é conferida",
            "Quando você diz que já leu, o app confere o tempo real que o app da Bíblia escolhido ficou aberto hoje, pelo acesso de uso do Android. Sem esse tempo, a resposta não é aceita e o app da Bíblia abre.",
        )
        AboutBlock(
            "Silenciar",
            "Dá para silenciar por alguns minutos. O silêncio nunca passa da meia-noite e não conta como leitura.",
        )
        AboutBlock(
            "A árvore",
            "Cada dia lido faz a árvore crescer, da semente até a Árvore Iluminada, com cinco anos de sequência. Um dia sem leitura volta à semente. A melhor sequência fica guardada.",
        )
        AboutBlock(
            "Privacidade",
            "Sem conta, sem internet, sem anúncios. A lista de apps, as escolhas e a sequência ficam só neste celular.",
        )
        AboutBlock(
            "Limites",
            "O Android não deixa um app comum travar outro. O M 6:33 mostra uma tela por cima, em geral um instante depois de o app abrir. Se uma permissão for revogada ou o celular encerrar o app, a pergunta não aparece.",
        )
        AboutBlock(
            "Textos bíblicos",
            "As citações seguem a tradução de João Ferreira de Almeida, às vezes resumidas com […]. Confira sempre na sua Bíblia.",
        )
        Spacer(Modifier.height(24.dp))
        Muted("Versão $version", size = 12, align = TextAlign.Start)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AboutBlock(title: String, body: String) {
    SectionTitle(title)
    Text(body, fontSize = 15.sp, lineHeight = 22.sp)
}

package com.edsonivo.m633.ui

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edsonivo.m633.R
import com.edsonivo.m633.core.AppState
import com.edsonivo.m633.core.ThemeMode
import com.edsonivo.m633.system.Apps
import com.edsonivo.m633.system.Permissions
import com.edsonivo.m633.ui.theme.LocalExtra

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.fase_02_broto), contentDescription = null, modifier = Modifier.size(170.dp))
        Text("M 6:33", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text(
            "“Mas buscai primeiro o reino de Deus, e a sua justiça, e todas estas coisas vos serão acrescentadas.”",
            fontSize = 17.sp, lineHeight = 25.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
        )
        Muted("Mateus 6:33", modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(26.dp))
        Text(
            "Antes de abrir os apps que você escolher, uma pergunta: você já leu a Bíblia hoje?",
            fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Primary("Começar", onClick = onStart)
    }
}

/** Relido a cada volta das configurações: [tick] muda no onResume da activity. */
@Composable
fun PermissionsPanel(tick: Int, onRequestNotifications: () -> Unit) {
    val context = LocalContext.current
    key(tick) {
        PermissionRow(
            title = "Acesso de uso",
            body = "Para saber qual app veio para a frente e conferir o tempo no app da Bíblia. Não lemos a tela nem o que você escreve.",
            ok = Permissions.hasUsage(context),
            action = "Abrir acesso de uso",
        ) {
            runCatching { context.startActivity(Permissions.usageIntent(context)) }
                .onFailure { context.startActivity(Permissions.usageIntentFallback()) }
        }
        PermissionRow(
            title = "Exibir sobre outros apps",
            body = "Para colocar a pergunta por cima do app aberto. Isso cobre o app, não o trava.",
            ok = Permissions.hasOverlay(context),
            action = "Permitir exibir sobre outros apps",
        ) { context.startActivity(Permissions.overlayIntent(context)) }
        PermissionRow(
            title = "Notificação (opcional)",
            body = "O aviso fixo é o lembrete trabalhando. Sem ele o Android pode parar o app.",
            ok = Permissions.hasNotifications(context),
            action = "Permitir notificação",
            onClick = onRequestNotifications,
        )
        PermissionRow(
            title = "Bateria (opcional)",
            body = "Se o celular encerrar este app, a pergunta não aparece. Comum em Xiaomi, Samsung, Motorola e outros.",
            ok = Permissions.ignoresBattery(context),
            action = "Não restringir a bateria",
        ) { runCatching { context.startActivity(Permissions.batteryIntent(context)) } }
        Muted(
            "Se um botão não abrir a tela certa: Configurações › Apps › Acesso especial.",
            size = 12, align = TextAlign.Start, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun PermissionRow(title: String, body: String, ok: Boolean, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (ok) "ativo" else "ainda falta", fontSize = 13.sp, color = LocalExtra.current.muted)
        }
        Muted(body, size = 13, align = TextAlign.Start, modifier = Modifier.padding(start = 28.dp, top = 2.dp))
        if (!ok) {
            OutlinedButton(onClick = onClick, modifier = Modifier.padding(start = 28.dp, top = 6.dp)) { Text(action) }
        }
    }
}

@Composable
fun OnboardingPermissions(tick: Int, onRequestNotifications: () -> Unit, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val ready = key(tick) { Permissions.required(context) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Duas permissões", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Muted("As duas primeiras são obrigatórias e são ligadas nas configurações do Android. Depois, volte para cá.", align = TextAlign.Start)
            Spacer(Modifier.height(10.dp))
            PermissionsPanel(tick, onRequestNotifications)
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (ready) {
                Text("Tudo pronto.", fontWeight = FontWeight.SemiBold)
                Muted("Quando você abrir um app escolhido, vamos perguntar se você já leu a Bíblia hoje.", size = 13)
                Spacer(Modifier.height(10.dp))
            }
            Primary("Concluir", enabled = ready, onClick = onDone)
        }
    }
}

@Composable
fun SettingsScreen(
    state: AppState,
    tick: Int,
    debug: Boolean,
    onBack: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onPickBible: () -> Unit,
    onPickBlocked: () -> Unit,
    onMinMinutes: (Int) -> Unit,
    onRequestNotifications: () -> Unit,
    onSimulate: (Int) -> Unit,
) {
    val context = LocalContext.current
    val muted = LocalExtra.current.muted
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, start = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") }
            Text("Ajustes", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            SectionTitle("Aparência")
            val modes = listOf(ThemeMode.SYSTEM to "Como o celular", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Escuro")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = state.theme == mode,
                        onClick = { onTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    ) { Text(label, fontSize = 13.sp) }
                }
            }

            SectionTitle("App da Bíblia")
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label = state.biblePackage?.let { Apps.label(context, it) }
                Text(label ?: "${state.bibleLabel ?: "Nenhum"} (não instalado)", fontSize = 16.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onPickBible) { Text("Trocar") }
            }

            SectionTitle("Apps bloqueados")
            Row(verticalAlignment = Alignment.CenterVertically) {
                val names = state.blocked.mapNotNull { Apps.label(context, it) }.sorted()
                Text(
                    if (names.isEmpty()) "Nenhum" else names.joinToString(", "),
                    fontSize = 16.sp, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onPickBlocked) { Text("Editar") }
            }

            SectionTitle("Leitura mínima por dia")
            Muted("Conferimos o tempo real que o app da Bíblia ficou aberto hoje.", size = 13, align = TextAlign.Start)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf(1, 3, 5, 10, 15).forEach { m ->
                    FilterChip(selected = state.minReadMinutes == m, onClick = { onMinMinutes(m) }, label = { Text("$m min") })
                }
            }

            SectionTitle("Permissões")
            PermissionsPanel(tick, onRequestNotifications)

            if (debug) {
                SectionTitle("Simulação (só na versão de teste)")
                Muted("Define a sequência como se o último dia lido fosse ontem.", size = 13, align = TextAlign.Start)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(0, 6, 29, 89, 364).forEach { d ->
                        OutlinedButton(onClick = { onSimulate(d) }) { Text("$d", fontSize = 12.sp) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    listOf(729, 1094, 1824).forEach { d ->
                        OutlinedButton(onClick = { onSimulate(d) }) { Text("$d", fontSize = 12.sp) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Nada sai deste celular.", fontSize = 12.sp, color = muted)
            Spacer(Modifier.height(32.dp))
        }
    }
}

fun Intent.withNewTask(): Intent = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

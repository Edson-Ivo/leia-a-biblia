package com.edsonivo.m633.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edsonivo.m633.system.Apps
import com.edsonivo.m633.system.InstalledApp
import com.edsonivo.m633.ui.theme.LocalExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberInstalledApps(): List<InstalledApp>? {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { 40.dp.roundToPx() }
    val apps by produceState<List<InstalledApp>?>(null) {
        value = withContext(Dispatchers.IO) { Apps.launchable(context, px) }
    }
    return apps
}

@Composable
fun AppPickerScreen(
    title: String,
    subtitle: String,
    multi: Boolean,
    selected: Set<String>,
    disabled: Map<String, String>,
    buttonText: String,
    buttonEnabled: Boolean,
    onToggle: (InstalledApp) -> Unit,
    onButton: () -> Unit,
    onBack: (() -> Unit)? = null,
    suggested: Set<String> = emptySet(),
) {
    val context = LocalContext.current
    val apps = rememberInstalledApps()
    var query by rememberSaveable { mutableStateOf("") }
    val browsers = remember {
        context.packageManager
            .queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("https://exemplo.com")), 0)
            .map { it.activityInfo.packageName }.toSet()
    }
    val muted = LocalExtra.current.muted

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") }
            }
        }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(6.dp))
        Muted(subtitle, align = TextAlign.Start)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Muted(
            "Mostramos os apps instalados só para você escolher. A lista fica neste celular. Não enviamos ela para lugar nenhum.",
            size = 12, align = TextAlign.Start,
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (apps == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                val visible = apps
                    .filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
                    .sortedBy { if (it.packageName in suggested) 0 else 1 }
                if (apps.count { it.packageName != context.packageName } == 0) {
                    Muted(
                        "Não estou vendo meus apps: o Android não entregou a lista a esta versão. " +
                            "No Android comum não existe um botão “permitir listar apps” depois de instalar. " +
                            "Reinstale uma versão do M 6:33 que declare essa visibilidade.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(visible, key = { it.packageName }) { app ->
                        val reason = disabled[app.packageName]
                        val isSelected = app.packageName in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = reason == null) { onToggle(app) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (app.icon != null) {
                                Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp), alpha = if (reason == null) 1f else 0.4f)
                            } else {
                                Spacer(Modifier.size(40.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.label, fontSize = 16.sp,
                                    color = if (reason == null) MaterialTheme.colorScheme.onBackground else muted,
                                )
                                val note = reason
                                    ?: if (app.packageName in suggested) "Sugerido: app de Bíblia" else null
                                    ?: if (multi && isSelected && app.packageName in browsers) "A pergunta aparece sempre que o navegador abrir" else null
                                if (note != null) Text(note, fontSize = 12.sp, color = muted)
                            }
                            if (reason == null) {
                                if (multi) Checkbox(checked = isSelected, onCheckedChange = { onToggle(app) })
                                else RadioButton(selected = isSelected, onClick = { onToggle(app) })
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
            Primary(buttonText, enabled = buttonEnabled, onClick = onButton)
        }
    }
}

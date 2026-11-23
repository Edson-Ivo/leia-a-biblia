package com.edsonivo.m633.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edsonivo.m633.R
import com.edsonivo.m633.core.DayRules
import com.edsonivo.m633.core.DayStatus
import com.edsonivo.m633.core.Message
import com.edsonivo.m633.core.SilenceChoice
import com.edsonivo.m633.core.Tree
import com.edsonivo.m633.ui.theme.LocalExtra
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HOUR = DateTimeFormatter.ofPattern("HH:mm")

fun hourOf(ms: Long): String = HOUR.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@Composable
fun Primary(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.widthIn(min = 240.dp).height(54.dp),
        shape = RoundedCornerShape(28.dp),
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Secondary(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.widthIn(min = 240.dp).height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
    ) { Text(text, fontSize = 17.sp) }
}

/** Lista de durações do Silenciar. Nenhuma opção passa da meia-noite local. */
@Composable
fun SilencePicker(
    lastCustom: Int?,
    maxMinutes: () -> Int,
    onPick: (minutes: Int, custom: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val max = remember { maxMinutes() }
    val now = remember { System.currentTimeMillis() }
    var custom by remember { mutableStateOf(false) }
    val muted = LocalExtra.current.muted
    fun endsAt(m: Int) = hourOf(minOf(now + m * 60_000L, now + max * 60_000L))

    Text("Silenciar por", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Text("Até a meia-noite cabem ${DayRules.formatMinutes(max)}.", color = muted, fontSize = 14.sp)
    Spacer(Modifier.height(20.dp))
    if (!custom) {
        DayRules.silenceChoices(lastCustom, max).forEach { choice ->
            val label = when (choice.kind) {
                SilenceChoice.Kind.CUSTOM -> "Valor customizado"
                else -> "${DayRules.formatMinutes(choice.minutes!!)}, até ${endsAt(choice.minutes)}"
            }
            OutlinedButton(
                onClick = {
                    if (choice.kind == SilenceChoice.Kind.CUSTOM) custom = true
                    else onPick(choice.minutes!!, false)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
            ) { Text(label, fontSize = 16.sp) }
        }
        TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text("Cancelar") }
    } else {
        var hours by remember { mutableIntStateOf(0) }
        var minutes by remember { mutableIntStateOf(minOf(15, max)) }
        val total = hours * 60 + minutes
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Stepper("horas", hours, onMinus = { if (hours > 0) hours-- }, onPlus = { if ((hours + 1) * 60 <= max) { hours++; if (hours * 60 + minutes > max) minutes = max - hours * 60 } })
            Spacer(Modifier.width(24.dp))
            Stepper("min", minutes, onMinus = { if (minutes > 0) minutes-- }, onPlus = { if (minutes < 59 && total + 1 <= max) minutes++ })
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (total in 1..max) "Até ${endsAt(total)}" else "Escolha pelo menos 1 minuto",
            color = muted,
        )
        Spacer(Modifier.height(20.dp))
        Primary("Silenciar", enabled = total in 1..max) { onPick(total, true) }
        TextButton(onClick = { custom = false }, modifier = Modifier.padding(top = 8.dp)) { Text("Voltar") }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onPlus) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Mais $label") }
        Text("$value", fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = LocalExtra.current.muted, fontSize = 13.sp)
        IconButton(onClick = onMinus) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Menos $label") }
    }
}

/** A árvore atual. Balança sempre, e o balanço não recomeça com toques. */
@Composable
fun TreeImage(streak: Int, shownStage: Int, size: Dp, modifier: Modifier = Modifier) {
    val growth = Tree.growthFor(streak)
    val sway = rememberInfiniteTransition(label = "sway")
    val angle by sway.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "angle",
    )
    AnimatedContent(
        targetState = shownStage,
        transitionSpec = {
            (fadeIn(tween(900, delayMillis = 500)) + scaleIn(tween(900, delayMillis = 500), initialScale = 0.15f))
                .togetherWith(fadeOut(tween(600)) + scaleOut(tween(600), targetScale = 0.1f))
        },
        label = "stage",
        modifier = modifier,
    ) { index ->
        val stage = Tree.STAGES[index]
        val grow = if (index == growth.stage.index) 0.8f + 0.2f * growth.amount else 1f
        Image(
            painter = painterResource(stage.image),
            contentDescription = stage.name,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = grow
                    scaleY = grow
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                },
        )
    }
}

/** Prévia preta e parada da próxima fase. Só aparece no Progresso. */
@Composable
fun NextSilhouette(stageIndex: Int, size: Dp) {
    val stage = Tree.STAGES[stageIndex]
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(stage.image),
            contentDescription = "Próxima fase: ${stage.name}",
            colorFilter = ColorFilter.tint(Color.Black),
            modifier = Modifier.size(size * 0.82f),
        )
    }
}

/** O cordeiro que flutua. Ao toque, fala uma mensagem com os versículos que a sustentam. */
@Composable
fun Character(
    status: DayStatus,
    message: Message?,
    themeLabel: String,
    open: Boolean,
    onTap: () -> Unit,
    onCloseBubble: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val float = rememberInfiniteTransition(label = "float")
    val dy by float.animateFloat(
        initialValue = 0f, targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy",
    )
    val image = when (status) {
        DayStatus.DONE -> R.drawable.personagem_feliz
        DayStatus.SILENT -> R.drawable.personagem_desconfiado
        DayStatus.OPEN -> R.drawable.personagem_normal
    }
    Column(modifier, horizontalAlignment = Alignment.Start) {
        if (open && message != null) {
            Bubble(message, themeLabel, onCloseBubble)
        }
        Image(
            painter = painterResource(image),
            contentDescription = "Personagem. Toque para ouvir uma mensagem.",
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer { translationY = dy * density }
                .clip(CircleShape)
                .clickable(onClick = onTap),
        )
    }
}

@Composable
private fun Bubble(message: Message, themeLabel: String, onClose: () -> Unit) {
    val muted = LocalExtra.current.muted
    Column(horizontalAlignment = Alignment.Start) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onClose)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(themeLabel.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(message.text, fontSize = 15.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            message.verses.forEach { v ->
                Spacer(Modifier.height(8.dp))
                Text("“${v.text}”", fontSize = 13.sp, lineHeight = 18.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v.ref, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = muted)
            }
        }
        Box(
            Modifier
                .padding(start = 30.dp)
                .size(width = 16.dp, height = 10.dp)
                .graphicsLayer { rotationZ = 45f; translationY = -6.dp.toPx() }
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Int = 14, align: TextAlign = TextAlign.Center) {
    Text(text, modifier = modifier, color = LocalExtra.current.muted, fontSize = size.sp, textAlign = align, lineHeight = (size + 5).sp)
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.6.sp, modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp))
}

package com.edsonivo.m633

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.edsonivo.m633.core.DayRules
import com.edsonivo.m633.core.Milestones
import com.edsonivo.m633.core.ThemeMode
import com.edsonivo.m633.core.Tree
import com.edsonivo.m633.guard.GuardService
import com.edsonivo.m633.system.Apps
import com.edsonivo.m633.system.Permissions
import com.edsonivo.m633.system.Usage
import com.edsonivo.m633.ui.AboutScreen
import com.edsonivo.m633.ui.AppPickerScreen
import com.edsonivo.m633.ui.Character
import com.edsonivo.m633.ui.GateActions
import com.edsonivo.m633.ui.GateFlow
import com.edsonivo.m633.ui.HomeScreen
import com.edsonivo.m633.ui.OnboardingPermissions
import com.edsonivo.m633.ui.Primary
import com.edsonivo.m633.ui.ProgressScreen
import com.edsonivo.m633.ui.SettingsScreen
import com.edsonivo.m633.ui.SilencePicker
import com.edsonivo.m633.ui.WelcomeScreen
import com.edsonivo.m633.ui.theme.M633Theme
import com.edsonivo.m633.ui.theme.isDark
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)
    private var pickBibleRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tick++
                GuardService.start(this)
            }
        })
        setContent {
            val state by app.store.state.collectAsState()
            val dark = isDark(state.theme)
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            M633Theme(state.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.safeDrawingPadding()) { Root(tick, pickBibleRequest) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_PICK_BIBLE, false) == true) pickBibleRequest++
    }

    companion object {
        const val EXTRA_PICK_BIBLE = "pick_bible"
    }
}

private enum class Screen { WELCOME, PICK_BIBLE, PICK_BLOCKED, PERMISSIONS, HOME, PROGRESS, ABOUT, SETTINGS, SETTINGS_BIBLE, SETTINGS_BLOCKED }

@Composable
private fun Root(tick: Int, pickBibleRequest: Int) {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val store = activity.app.store
    val state by store.state.collectAsState()
    var screen by rememberSaveable { mutableStateOf(if (state.onboarded) Screen.HOME else Screen.WELCOME) }
    var draftBible by rememberSaveable { mutableStateOf(state.biblePackage) }
    var draftBlocked by rememberSaveable { mutableStateOf(state.blocked.toList()) }

    LaunchedEffect(pickBibleRequest) {
        if (pickBibleRequest > 0 && state.onboarded) {
            draftBible = state.biblePackage
            screen = Screen.SETTINGS_BIBLE
        }
    }

    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { activity.recreate() }
    val requestNotifications = {
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val protected = remember(draftBible, tick) { Apps.protectedReasons(activity, draftBible) }

    when (screen) {
        Screen.WELCOME -> WelcomeScreen { screen = Screen.PICK_BIBLE }
        Screen.PICK_BIBLE, Screen.SETTINGS_BIBLE -> {
            val onboarding = screen == Screen.PICK_BIBLE
            val back = { screen = if (onboarding) Screen.WELCOME else Screen.SETTINGS }
            BackHandler(onBack = back)
            AppPickerScreen(
                title = "Em qual app você lê a Bíblia?",
                subtitle = "Escolha um. É nele que vamos conferir a sua leitura de cada dia.",
                multi = false,
                selected = setOfNotNull(draftBible),
                disabled = mapOf(activity.packageName to "É este app"),
                suggested = Apps.KNOWN_BIBLES,
                buttonText = if (onboarding) "Continuar" else "Salvar",
                buttonEnabled = draftBible != null,
                onToggle = { draftBible = it.packageName },
                onButton = {
                    if (onboarding) {
                        draftBlocked = draftBlocked - draftBible!!
                        screen = Screen.PICK_BLOCKED
                    } else {
                        val pkg = draftBible!!
                        store.update { it.copy(biblePackage = pkg, bibleLabel = Apps.label(activity, pkg), blocked = it.blocked - pkg) }
                        screen = Screen.SETTINGS
                    }
                },
                onBack = back,
            )
        }
        Screen.PICK_BLOCKED, Screen.SETTINGS_BLOCKED -> {
            val onboarding = screen == Screen.PICK_BLOCKED
            val back = { screen = if (onboarding) Screen.PICK_BIBLE else Screen.SETTINGS }
            BackHandler(onBack = back)
            val selectable = draftBlocked.filter { it !in protected }
            AppPickerScreen(
                title = "Quais apps vão perguntar antes?",
                subtitle = "Por exemplo: Instagram, TikTok, YouTube. Ao abrir um deles sem ter lido hoje, aparece a pergunta.",
                multi = true,
                selected = draftBlocked.toSet(),
                disabled = protected,
                buttonText = if (onboarding) "Continuar" else "Salvar",
                buttonEnabled = selectable.isNotEmpty(),
                onToggle = { app ->
                    draftBlocked = if (app.packageName in draftBlocked) draftBlocked - app.packageName else draftBlocked + app.packageName
                },
                onButton = {
                    if (onboarding) {
                        screen = Screen.PERMISSIONS
                    } else {
                        store.update { it.copy(blocked = selectable.toSet()) }
                        screen = Screen.SETTINGS
                    }
                },
                onBack = back,
            )
        }
        Screen.PERMISSIONS -> {
            BackHandler { screen = Screen.PICK_BLOCKED }
            OnboardingPermissions(
                tick = tick,
                onRequestNotifications = requestNotifications,
                onBack = { screen = Screen.PICK_BLOCKED },
                onDone = {
                    val pkg = draftBible!!
                    store.update {
                        it.copy(
                            onboarded = true,
                            biblePackage = pkg,
                            bibleLabel = Apps.label(activity, pkg),
                            blocked = draftBlocked.filter { p -> p !in protected }.toSet(),
                        )
                    }
                    GuardService.start(activity)
                    screen = Screen.HOME
                },
            )
        }
        Screen.SETTINGS -> {
            BackHandler { screen = Screen.HOME }
            SettingsScreen(
                state = state,
                tick = tick,
                debug = BuildConfig.DEBUG,
                onBack = { screen = Screen.HOME },
                onTheme = { mode -> store.update { it.copy(theme = mode) } },
                onPickBible = { draftBible = state.biblePackage; screen = Screen.SETTINGS_BIBLE },
                onPickBlocked = { draftBlocked = state.blocked.toList(); screen = Screen.SETTINGS_BLOCKED },
                onMinMinutes = { m -> store.update { it.copy(minReadMinutes = m) } },
                onRequestNotifications = requestNotifications,
                onSimulate = { d ->
                    store.update {
                        it.copy(
                            streak = d,
                            best = maxOf(it.best, d),
                            lastCompleted = if (d == 0) null else store.today().minusDays(1),
                        )
                    }
                    GuardService.wake()
                },
            )
        }
        Screen.HOME, Screen.PROGRESS, Screen.ABOUT -> MainTabs(
            screen = screen,
            tick = tick,
            onScreen = { screen = it },
        )
    }
}

@Composable
private fun MainTabs(screen: Screen, tick: Int, onScreen: (Screen) -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val store = activity.app.store
    val bank = activity.app.messages
    val state by store.state.collectAsState()

    var clock by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            clock++
        }
    }
    val fresh = remember(state, tick, clock) { store.fresh() }
    val today = remember(tick, clock, state) { store.today() }
    val status = remember(fresh, tick, clock) { store.status() }
    val streak = DayRules.currentStreak(fresh, today)
    val current = Tree.growthFor(streak).stage.index

    var shown by remember { mutableIntStateOf(state.lastSeenStage.coerceIn(0, Tree.STAGES.lastIndex)) }
    LaunchedEffect(current) {
        if (shown != current) {
            delay(700)
            shown = current
        }
        if (store.state.value.lastSeenStage != current) store.update { it.copy(lastSeenStage = current) }
    }

    val readMinutes by produceState(0L, tick, clock, state.biblePackage) {
        value = state.biblePackage?.let { Usage.foregroundTodayMs(activity, it) / 60_000L } ?: 0L
    }
    val missing = remember(tick) {
        buildList {
            if (!Permissions.hasUsage(activity)) add("o acesso de uso")
            if (!Permissions.hasOverlay(activity)) add("exibir sobre outros apps")
        }
    }
    val bibleLabel = remember(state.biblePackage, tick) {
        state.biblePackage?.let { Apps.label(activity, it) } ?: state.bibleLabel ?: "app da Bíblia"
    }

    var gateOpen by remember { mutableStateOf(false) }
    var silenceOpen by remember { mutableStateOf(false) }
    var bubbleOpen by remember { mutableStateOf(false) }
    var talkShown by remember { mutableIntStateOf(state.talkIndex) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("M 6:33", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { onScreen(Screen.SETTINGS) }) { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                Screen.HOME -> {
                    HomeScreen(
                        state = fresh,
                        status = status,
                        streak = streak,
                        shownStage = shown,
                        today = today,
                        verse = bank.ofDay(today.toEpochDay()),
                        readMinutes = readMinutes,
                        bibleLabel = bibleLabel,
                        missing = missing,
                        onRead = {
                            val pkg = state.biblePackage
                            if (pkg == null || !Apps.open(activity, pkg)) onScreen(Screen.SETTINGS)
                        },
                        onAlreadyRead = { gateOpen = true },
                        onSilence = { silenceOpen = true },
                        onEndSilence = { store.update(DayRules::endSilence); GuardService.wake() },
                        onFixPermissions = { onScreen(Screen.SETTINGS) },
                    )
                    val message = bank.forTalk(talkShown)
                    Character(
                        status = status,
                        message = message,
                        themeLabel = bank.themeLabel(message.theme),
                        open = bubbleOpen,
                        onTap = {
                            talkShown = store.state.value.talkIndex
                            store.update { it.copy(talkIndex = it.talkIndex + 1) }
                            bubbleOpen = true
                        },
                        onCloseBubble = { bubbleOpen = false },
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 60.dp, bottom = 6.dp),
                    )
                }
                Screen.PROGRESS -> ProgressScreen(fresh, streak, shown)
                else -> AboutScreen(BuildConfig.VERSION_NAME)
            }
        }
        NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            listOf(
                Triple(Screen.HOME, "Início", Icons.Filled.Home),
                Triple(Screen.PROGRESS, "Progresso", Icons.Filled.DateRange),
                Triple(Screen.ABOUT, "Sobre", Icons.Filled.Info),
            ).forEach { (target, label, icon) ->
                NavigationBarItem(
                    selected = screen == target,
                    onClick = { bubbleOpen = false; onScreen(target) },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                )
            }
        }
    }

    if (gateOpen) {
        val actions = remember {
            object : GateActions(activity) {
                override fun openBible(): Boolean {
                    val pkg = store.state.value.biblePackage ?: return false
                    gateOpen = false
                    return Apps.open(activity, pkg)
                }
                override fun chooseOtherBible() {
                    gateOpen = false
                    onScreen(Screen.SETTINGS)
                }
                override fun close() {
                    gateOpen = false
                }
            }
        }
        FullDialog(onDismiss = { gateOpen = false }) { GateFlow(actions, inApp = true) }
    }

    if (silenceOpen) {
        FullDialog(onDismiss = { silenceOpen = false }) {
            Column(
                Modifier.align(Alignment.Center).widthIn(max = 420.dp).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SilencePicker(
                    lastCustom = state.lastCustomMinutes,
                    maxMinutes = { DayRules.maxSilenceMinutes(store.now(), store.zone()) },
                    onPick = { minutes, custom ->
                        store.update { s -> DayRules.startSilence(s, minutes, custom, store.now(), store.zone()) ?: s }
                        GuardService.wake()
                        silenceOpen = false
                    },
                    onCancel = { silenceOpen = false },
                )
            }
        }
    }

    val celebrate = Milestones.byDays(state.pendingCelebration)
    if (celebrate != null && screen == Screen.HOME && !gateOpen) {
        FullDialog(onDismiss = { store.update { it.copy(pendingCelebration = 0) } }) {
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    celebrate.label,
                    fontSize = (24 + celebrate.tier * 4).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(-1.2f * celebrate.tier),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    Milestones.message(celebrate),
                    fontSize = (17 + celebrate.tier * 2).sp,
                    lineHeight = (25 + celebrate.tier * 3).sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.rotate(-0.8f * celebrate.tier),
                )
                Spacer(Modifier.height(32.dp))
                Primary("Amém") { store.update { it.copy(pendingCelebration = 0) } }
            }
        }
    }
}

@Composable
private fun FullDialog(onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
            content = content,
        )
    }
}

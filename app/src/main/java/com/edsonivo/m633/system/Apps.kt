package com.edsonivo.m633.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import java.util.Locale

data class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap?)

object Apps {
    /** Pacotes conhecidos de apps de Bíblia, só para sugerir no topo da lista. */
    val KNOWN_BIBLES = setOf(
        "com.sirma.mobile.bible.android",
        "com.bible.app.kjv",
        "com.bibliaonline.app",
        "com.biblia.sagrada",
        "br.com.sbb.bibliasbb",
        "com.blueletterbible.blb",
        "org.logos.android",
        "com.olivetree.bibleapp",
        "net.bible.android.activity",
        "com.glowbible.android",
    )

    fun launchable(context: Context, iconPx: Int): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance(Locale("pt", "BR"))
        return pm.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { ri ->
                val icon = runCatching {
                    ri.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap()
                }.getOrNull()
                InstalledApp(ri.activityInfo.packageName, ri.loadLabel(pm).toString(), icon)
            }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun label(context: Context, pkg: String): String? = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrNull()

    fun isInstalled(context: Context, pkg: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(pkg) != null

    fun open(context: Context, pkg: String): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching { context.startActivity(launch); true }.getOrDefault(false)
    }

    fun goHome(context: Context) {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(home) }
    }

    /** Pacotes que nunca podem ser interceptados, com o motivo mostrado na lista. */
    fun protectedReasons(context: Context, biblePackage: String?): Map<String, String> {
        val out = linkedMapOf<String, String>()
        out[context.packageName] = "É este app"
        biblePackage?.let { out[it] = "Este é o app da leitura" }
        settingsPackages(context).forEach { out.putIfAbsent(it, "As configurações precisam continuar acessíveis") }
        defaultDialer(context)?.let { out.putIfAbsent(it, "O telefone não pode ser coberto") }
        Telephony.Sms.getDefaultSmsPackage(context)?.let { out.putIfAbsent(it, "As mensagens SMS não podem ser cobertas") }
        homePackages(context).forEach { out.putIfAbsent(it, "É a tela inicial do aparelho") }
        return out
    }

    private fun settingsPackages(context: Context): Set<String> =
        context.packageManager.queryIntentActivities(Intent(Settings.ACTION_SETTINGS), 0)
            .map { it.activityInfo.packageName }.toSet() + "com.android.settings"

    private fun defaultDialer(context: Context): String? =
        runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()

    fun homePackages(context: Context): Set<String> =
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        ).map { it.activityInfo.packageName }.toSet()
}

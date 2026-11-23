package com.edsonivo.m633.guard

import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.edsonivo.m633.MainActivity
import com.edsonivo.m633.app
import com.edsonivo.m633.system.Apps
import com.edsonivo.m633.ui.GateFlow
import com.edsonivo.m633.ui.GateActions
import com.edsonivo.m633.ui.theme.M633Theme

/** A pergunta do dia, numa janela por cima do app que a pessoa abriu. */
class Overlay(private val service: GuardService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private var root: View? = null

    val showing: Boolean get() = root != null

    fun show() {
        if (root != null || !Settings.canDrawOverlays(service)) return
        val compose = ComposeView(service).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeSavedStateRegistryOwner(service)
            setContent {
                val state by service.app.store.state.collectAsState()
                M633Theme(state.theme) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .safeDrawingPadding(),
                    ) {
                        GateFlow(actions = actions, inApp = false)
                    }
                }
            }
        }
        val frame = object : FrameLayout(service) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
                return super.dispatchKeyEvent(event)
            }
        }
        frame.addView(compose)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.OPAQUE,
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        runCatching {
            wm.addView(frame, params)
            root = frame
        }
    }

    fun hide() {
        root?.let { runCatching { wm.removeViewImmediate(it) } }
        root = null
    }

    private val actions = object : GateActions(service) {
        override fun openBible(): Boolean {
            val pkg = store.state.value.biblePackage ?: return false
            if (!Apps.isInstalled(service, pkg)) return false
            hide()
            return Apps.open(service, pkg)
        }

        override fun chooseOtherBible() {
            hide()
            service.startActivity(
                Intent(service, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_PICK_BIBLE, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }

        override fun close() {
            hide()
            GuardService.wake()
        }
    }
}

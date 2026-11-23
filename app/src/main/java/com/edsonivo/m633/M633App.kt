package com.edsonivo.m633

import android.app.Application
import com.edsonivo.m633.core.MessageBank
import com.edsonivo.m633.core.Store

class M633App : Application() {
    lateinit var store: Store
        private set
    val messages: MessageBank by lazy { MessageBank.load(assets.open("mensagens.json")) }

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
    }
}

val android.content.Context.app: M633App get() = applicationContext as M633App

package com.sweetsound.cardinfo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sweetsound.cardinfo.service.InitDbService

class InitBroadcastReceiver(): BroadcastReceiver() {
    companion object {
        val ACTION_INIT = "com.sweetsound.cardinfo.ACTION_INIT"
    }
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_INIT -> {
                context.startService(Intent(context, InitDbService::class.java))
            }
        }
    }
}
package com.sweetsound.cardinfo.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sweetsound.storeplan.db.DbUtil

class InitDbService(): Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DbUtil(baseContext).delete(null)

        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }
}
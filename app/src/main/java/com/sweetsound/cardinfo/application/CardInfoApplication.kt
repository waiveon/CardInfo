package com.sweetsound.cardinfo.application

import android.app.Application
import com.google.firebase.FirebaseApp
import com.sweetsound.cardinfo.constant.ConstShardPreference

class CardInfoApplication : Application() {

    companion object {
        lateinit var SHARED_PREF: ConstShardPreference
    }

    override fun onCreate() {
        super.onCreate()

        SHARED_PREF = ConstShardPreference(baseContext)
        FirebaseApp.initializeApp(this)

    }
}
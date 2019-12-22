package com.sweetsound.cardinfo.application

import android.app.Application
import com.google.firebase.FirebaseApp

class CardInfoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
    }
}
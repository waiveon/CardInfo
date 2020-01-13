package com.sweetsound.cardinfo.constant

import android.app.Activity
import android.content.Context

private const val SHARED_NAME = "SHARED_NAME"
private const val KEY_FIRST_RUNNING = "KEY_FIRST_RUNNING"
private const val KEY_EMAIL = "KEY_EMAIL"
private const val KEY_PASSWD = "KEY_PASSWD"
private const val KEY_WOORI_CARD_REACHED_USAGE_AMOUNT = "KEY_WOORI_CARD_REACHED_USAGE_AMOUNT"
private const val KEY_KB_CARD_REACHED_USAGE_AMOUNT = "KEY_KB_CARD_REACHED_USAGE_AMOUNT"
private const val KEY_HANA_CARD_REACHED_USAGE_AMOUNT = "KEY_HANA_CARD_REACHED_USAGE_AMOUNT"
private const val KEY_HYUNDAI_CARD_REACHED_USAGE_AMOUNT = "KEY_HYUNDAI_CARD_REACHED_USAGE_AMOUNT"
private const val KEY_ALERT_DIALOG_AUTO_CLOSE = "KEY_ALERT_DIALOG_AUTO_CLOSE"


class ConstShardPreference(context: Context) {
    private val mPref = context.getSharedPreferences(SHARED_NAME, Activity.MODE_PRIVATE)

    var firstRunning: Boolean
        get() = mPref.getBoolean(KEY_FIRST_RUNNING, true)
        set(value) = mPref.edit().putBoolean(KEY_FIRST_RUNNING, value).apply()

    var alertDialogAutoClose: Boolean
        get() = mPref.getBoolean(KEY_ALERT_DIALOG_AUTO_CLOSE, false)
        set(value) = mPref.edit().putBoolean(KEY_ALERT_DIALOG_AUTO_CLOSE, value).apply()

    inner class AutoLogin {
        var email: String?
            get() = mPref.getString(KEY_EMAIL, "")
            set(value) = mPref.edit().putString(KEY_EMAIL, value).apply()

        var passwd: String?
            get() = mPref.getString(KEY_PASSWD, "")
            set(value) = mPref.edit().putString(KEY_PASSWD, value).apply()

        fun clear() {
            email = ""
            passwd = ""
        }
    }

    inner class ReachedUsageAmount {
        var wooriCard: Boolean
            get() = mPref.getBoolean(KEY_WOORI_CARD_REACHED_USAGE_AMOUNT, false)
            set(value) = mPref.edit().putBoolean(KEY_WOORI_CARD_REACHED_USAGE_AMOUNT, value).apply()

        var kbCard: Boolean
            get() = mPref.getBoolean(KEY_KB_CARD_REACHED_USAGE_AMOUNT, false)
            set(value) = mPref.edit().putBoolean(KEY_KB_CARD_REACHED_USAGE_AMOUNT, value).apply()

        var hanaCard: Boolean
            get() = mPref.getBoolean(KEY_HANA_CARD_REACHED_USAGE_AMOUNT, false)
            set(value) = mPref.edit().putBoolean(KEY_HANA_CARD_REACHED_USAGE_AMOUNT, value).apply()

        var hyundaiCard: Boolean
            get() = mPref.getBoolean(KEY_HYUNDAI_CARD_REACHED_USAGE_AMOUNT, false)
            set(value) = mPref.edit().putBoolean(KEY_HYUNDAI_CARD_REACHED_USAGE_AMOUNT, value).apply()
    }
}
package com.sweetsound.cardinfo.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sweetsound.cardinfo.R
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.HANA_CARD_NAME
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.HYUNDAI_CARD_NAME
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.KB_CARD_NAME
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.WOORI_CARD_NAME
import com.sweetsound.cardinfo.constant.ConstShardPreference
import com.sweetsound.cardinfo.constant.ConstShardPreference.Companion.KEY_HANA_CARD_REACHED_USAGE_AMOUNT
import com.sweetsound.cardinfo.constant.ConstShardPreference.Companion.KEY_HYUNDAI_CARD_REACHED_USAGE_AMOUNT
import com.sweetsound.cardinfo.constant.ConstShardPreference.Companion.KEY_KB_CARD_REACHED_USAGE_AMOUNT
import com.sweetsound.cardinfo.constant.ConstShardPreference.Companion.KEY_WOORI_CARD_REACHED_USAGE_AMOUNT

class AlertDialogActivity  : AppCompatActivity() {

    companion object {
        val CARD_TYPE = "CARD_TYPE"

        fun open(context: Context, cardType: ConstCardType.CARD_TYPE) {
            val intent = Intent(context, AlertDialogActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.putExtra(CARD_TYPE, cardType.value)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intCardType = intent.getIntExtra(CARD_TYPE, -1)

        // 사용한도 도달한 카드 저장
        val sharedPref = getSharedPreferences(ConstShardPreference.SHARED_PREF_REACHED_USAGE_AMOUNT, Activity.MODE_PRIVATE)
        val sharedPrefEdit = sharedPref.edit()

        when (ConstCardType.getCardType(intCardType)) {
            ConstCardType.CARD_TYPE.WOORI -> {
                sharedPrefEdit.putBoolean(KEY_WOORI_CARD_REACHED_USAGE_AMOUNT, true)
            }

            ConstCardType.CARD_TYPE.HANA -> {
                sharedPrefEdit.putBoolean(KEY_HANA_CARD_REACHED_USAGE_AMOUNT, true)
            }

            ConstCardType.CARD_TYPE.HYUNDAI -> {
                sharedPrefEdit.putBoolean(KEY_HYUNDAI_CARD_REACHED_USAGE_AMOUNT, true)
            }

            ConstCardType.CARD_TYPE.KB -> {
                sharedPrefEdit.putBoolean(KEY_KB_CARD_REACHED_USAGE_AMOUNT, true)
            }
        }

        sharedPrefEdit.commit()

        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(getString(R.string.used_price_reach, ConstCardType.getCardName(intCardType)))
        alertDialog.setMessage(getString(R.string.use_next_card, getNotReachedCardNames(sharedPref)))
        alertDialog.setPositiveButton(android.R.string.ok, object: DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                finish()
            }
        })

        alertDialog.show()
    }

    private fun getNotReachedCardNames(shardPref: SharedPreferences):String {
        var cardNames: String = ""

        if (shardPref.getBoolean(KEY_WOORI_CARD_REACHED_USAGE_AMOUNT, false) == false) {
            cardNames += WOORI_CARD_NAME + " "
        }

        if (shardPref.getBoolean(KEY_HANA_CARD_REACHED_USAGE_AMOUNT, false) == false) {
            cardNames += HANA_CARD_NAME + " "
        }

        if (shardPref.getBoolean(KEY_HYUNDAI_CARD_REACHED_USAGE_AMOUNT, false) == false) {
            cardNames += HYUNDAI_CARD_NAME + " "
        }

        if (shardPref.getBoolean(KEY_KB_CARD_REACHED_USAGE_AMOUNT, false) == false) {
            cardNames += KB_CARD_NAME + " "
        }

        return cardNames
    }
}
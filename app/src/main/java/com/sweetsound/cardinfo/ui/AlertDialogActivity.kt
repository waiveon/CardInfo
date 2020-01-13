package com.sweetsound.cardinfo.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sweetsound.cardinfo.R
import com.sweetsound.cardinfo.application.CardInfoApplication.Companion.SHARED_PREF
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.HANA_CARD_NAME
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.HYUNDAI_CARD_NAME
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.KB_CARD_NAME
import com.sweetsound.cardinfo.constant.ConstCardType.Companion.WOORI_CARD_NAME
import com.sweetsound.cardinfo.utils.CardUtils
import com.sweetsound.cardinfo.utils.Utils
import kotlinx.android.synthetic.main.use_dialog_layout.view.*

class AlertDialogActivity : AppCompatActivity() {
    private val DISMISS_ALERTDIALOG_TIME = 10000L

    companion object {
        val CARD_TYPE = "CARD_TYPE"
        val NOTI_TITLE = "NOTI_TITLE"
        val NOTI_TEXT = "NOTI_TEXT"
        val NOTI_BIG_TEXT = "NOTI_BIG_TEXT"

        fun open(context: Context, cardType: ConstCardType.CARD_TYPE) {
            open(context, cardType, "", "", "")
        }

        fun open(context: Context, cardType: ConstCardType.CARD_TYPE, notiTitle: String, notiText: String, notiBigText: String) {
            val intent = Intent(context, AlertDialogActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.putExtra(CARD_TYPE, cardType.value)
            intent.putExtra(NOTI_TITLE, notiTitle)
            intent.putExtra(NOTI_TEXT, notiText)
            intent.putExtra(NOTI_BIG_TEXT, notiBigText)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intCardType = intent.getIntExtra(CARD_TYPE, -1)
        val notiText = intent.getStringExtra(NOTI_TEXT)
        val notiBigText = intent.getStringExtra(NOTI_BIG_TEXT)

        if (notiText != null || notiBigText != null) {
            val cardUseHistory = CardUtils.parsingNoti(this, intent.getStringExtra(NOTI_TITLE), notiText, notiBigText)

            if (cardUseHistory.cardType != ConstCardType.CARD_TYPE.UNKNOWN) {
                // Dialog로 사용한 내역 띄우기
                // 자동 사라지는 기능 추가 <- 팝업에 체크 박스로 넣자
                val alertDialog = getAlertDialogBuilder(getString(R.string.use), Utils.getNumberWithComma(cardUseHistory.price))

                val checkBoxView = LayoutInflater.from(this).inflate(R.layout.use_dialog_layout, null) as CheckBox
                checkBoxView.setText(getString(R.string.auto_close, DISMISS_ALERTDIALOG_TIME.toString()))
                checkBoxView.auto_close_checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    SHARED_PREF.alertDialogAutoClose = isChecked
                }

                alertDialog.setView(checkBoxView)
                val dialog = alertDialog.create()
                dialog.show()

                checkBoxView.postDelayed({
                    dialog.dismiss()
                }, DISMISS_ALERTDIALOG_TIME)
            }
        } else {
            // 사용한도 도달한 카드 저장
            when (ConstCardType.getCardType(intCardType)) {
                ConstCardType.CARD_TYPE.WOORI -> {
                    SHARED_PREF.ReachedUsageAmount().wooriCard = true
                }

                ConstCardType.CARD_TYPE.HANA -> {
                    SHARED_PREF.ReachedUsageAmount().hanaCard = true
                }

                ConstCardType.CARD_TYPE.HYUNDAI -> {
                    SHARED_PREF.ReachedUsageAmount().hyundaiCard = true
                }

                ConstCardType.CARD_TYPE.KB -> {
                    SHARED_PREF.ReachedUsageAmount().kbCard = true
                }
            }

            getAlertDialogBuilder(getString(R.string.used_price_reach, ConstCardType.getCardName(intCardType)),
                getString(R.string.use_next_card, getNotReachedCardNames()))
                .show()
        }
    }

    private fun getAlertDialogBuilder(title: String, message: String): AlertDialog.Builder {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton(android.R.string.ok, object: DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                finish()
            }
        })

        return alertDialog
    }

    private fun getNotReachedCardNames():String {
        var cardNames: String = ""

        if (SHARED_PREF.ReachedUsageAmount().wooriCard == false) {
            cardNames += WOORI_CARD_NAME + " "
        }

        if (SHARED_PREF.ReachedUsageAmount().hanaCard == false) {
            cardNames += HANA_CARD_NAME + " "
        }

        if (SHARED_PREF.ReachedUsageAmount().hyundaiCard == false) {
            cardNames += HYUNDAI_CARD_NAME + " "
        }

        if (SHARED_PREF.ReachedUsageAmount().kbCard == false) {
            cardNames += KB_CARD_NAME + " "
        }

        return cardNames
    }
}
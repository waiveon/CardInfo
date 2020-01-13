package com.sweetsound.cardinfo.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sweetsound.cardinfo.utils.CardUtils
import com.sweetsound.logtofile.LogToFile

class DetectedNotificationService(): NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val extras = sbn.notification.extras

        val packageName = sbn.packageName
        val title = extras.getString(Notification.EXTRA_TITLE)
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT)
        val text = extras.getString(Notification.EXTRA_TEXT)
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT)
        val infoText = extras.getString(Notification.EXTRA_INFO_TEXT)
        val summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val tickerText = sbn.notification.tickerText

        var textline: String = ""

        textLines?.forEach {
            textline += it.toString() + "\n"
         }

        if (isSkip(title, packageName) == false) {
            val logToFile = LogToFile(baseContext)
            logToFile.wirte(DetectedNotificationService::class.java.simpleName, "Notification Post -\npackageName : ${packageName}\ntitle : '${title}'\nsubText : ${subText}\ntext : ${text}\nbigText : ${bigText}\ninfoText : ${infoText}\nsummaryText : ${summaryText}\ntickerText : ${tickerText}\ntextline : ${textline}")

            CardUtils.parsingNoti(baseContext, title, text, bigText)
        }

//      AlertDialogActivity.open(baseContext, ConstCardType.CARD_TYPE.HYUNDAI)


        // 로컬 브로드 케스트를 날려 CardInfoActivity에 반영
    }

    private fun isSkip(title: String?, packageName: String): Boolean {
        return isSkipPackage(packageName) == true ||
                title == null ||
                title.indexOf("캐시워크") > -1 ||
                title.equals("Phone") == true ||
                title.indexOf("충전") > -1 ||
                title.indexOf("USB로") > -1 ||
                title.indexOf("걸음") > -1 ||
                title.equals("소프트웨어 업데이트") == true
    }

    private fun isSkipPackage(packageName: String): Boolean {
        return packageName.equals("com.kakao.talk") ||
                packageName.equals("com.Project100Pi.themusicplayer") ||
                packageName.equals("com.android.providers.downloads") ||
                packageName.equals("com.google.android.gms") ||
                packageName.equals("com.android.vending") ||
                packageName.equals("com.android.systemui") ||
                packageName.equals("ctrip.english") ||
                packageName.equals("com.skt.skaf.OA00026910") ||
                packageName.equals("com.nhn.android.band") ||
                packageName.equals("com.tmon") ||
                packageName.equals("com.lguplus.appstore") ||
                packageName.equals("com.lottemembers.android") ||
                packageName.equals("com.ahnlab.v3mobileplus") ||
                packageName.equals("android") ||
                packageName.equals("com.google.android.youtube") ||
                packageName.equals("com.google.android.calendar") ||
                packageName.equals("com.facebook.katana") ||
                packageName.equals("com.towneers.www") ||
                packageName.equals("com.samsung.android.app.smartcapture") ||
                packageName.equals("com.yuanta.tradarm") ||
                packageName.equals("kr.co.ivlog.mobile.app.cjonecard") ||
                packageName.equals("com.pearlabyss.blackdesertm") ||
                packageName.equals("kr.co.smartstudy.timestablesiap_android_googlemarket") ||
                packageName.equals("com.sec.android.app.shealth") ||
                packageName.equals("com.samsung.android.net.wifi.wifiguider") ||
                packageName.equals("com.gsr.gs25") ||
                packageName.equals("uplus.membership") ||
                packageName.equals("com.samsung.android.calendar") ||
                packageName.equals("com.samsung.android.dialer") ||
                packageName.equals("com.cashslide") ||
                packageName.equals("kr.co.aladin.third_shop") ||
                packageName.equals("com.nicedayapps.iss_free") ||
                packageName.equals("com.sec.android.appshealth") ||
                packageName.equals("com.sec.android.app.sbrowser") ||
                packageName.equals("com.samsung.android.app.omcagent") ||
                packageName.equals("com.sec.android.app.samsungapps") ||
                packageName.equals("com.sec.android.app.clockpackage") ||
                packageName.equals("com.android.settings")
    }
}
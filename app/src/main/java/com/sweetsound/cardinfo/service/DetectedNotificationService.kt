package com.sweetsound.cardinfo.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.constant.ConstDate
import com.sweetsound.cardinfo.data.CardUseHistory
import com.sweetsound.logtofile.LogToFile
import com.sweetsound.storeplan.db.DbUtil

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

            title?.let {
                // 하나카드
                if (it.equals("정상승인") == true) {
                    text?.let {
                        DbUtil(baseContext).insert(getHanaCardUseHistory(text))
                    }
                } else if (it.equals(ConstCardType.HYUNDAI_CARD_NAME) == true) { // 현대카드
                    // text : 이*석님, 현대카드 블루멤버스 승인 5,500원 일시불 12/16 09:31\n휘닉스벤딩서비스
                    text?.let {
                        val texts = it.split(" ")

                        var price = getPriceToLong(texts[4])

                        if (texts[3].equals("승인") == false) {
                            price = price * -1
                        }

                        DbUtil(baseContext).insert(CardUseHistory("", ConstCardType.CARD_TYPE.HYUNDAI, price, parseDate(texts[6], texts[7])))
                    }

                } else if (it.indexOf("승인") > -1) { // KB Card or BC 카드 : 승인 12,650원 - BC 카드 어플 에서 뿌리는 Noti라 동일하다.
                    text?.let {
                        // text : 국민BC(3045)승인 이*석님 12,650원 일시불 12/13 10:57 olleh-139770
                        // text : 우리BC(8842)승인 이*석님 12,650원 일시불 12/13 10:57 롯데...
                        val texts = it.split(" ")

                        texts[0]
                        texts[6] // olleh-139770 or 사용한 업체명이 오지 않을까 싶다.

                        var cardType: ConstCardType.CARD_TYPE = ConstCardType.CARD_TYPE.UNKNOWN
                        var cardNum: String = ""

                        if (texts[0].indexOf("국민") > -1) {
                            cardType = ConstCardType.CARD_TYPE.KB
                        } else if (texts[0].indexOf("우리") > -1) {
                            cardType = ConstCardType.CARD_TYPE.WOORI
                            cardNum = getCardNum(texts[0])
                        }

                        if (texts[6].lastIndexOf("9770") < 0) { // 통신료가 아니라면 저장한다.
                            val cardUseHistory = CardUseHistory(cardNum, cardType, getPriceToLong(texts[2]), parseDate(texts[4], texts[5]))
                            DbUtil(baseContext).insert(cardUseHistory)
                        }
                    }
                } else {
                    val address = it.replace("-", "")

                    if (address.equals(ConstCardType.HYUNDAI_CARD_ADDRESS)) {
                        Log.e("TAG", "LJS== 1111 : " + text)
                        logToFile.wirte("Debugging", "Hyundai address " + address)
                        logToFile.wirte("Debugging", "Hyundai text 1 " + text)
                            // text : [Web발신] \n 현대카드 블루멤버스 승인 \n 이*석 \n 120원 일시불 \n 12/10 13ㅣ35 \n 스마일페이 \n...
                        text?.let {
                            Log.e("TAG", "LJS== 222 : " + text)
                            Log.e("TAG", "LJS== 333 : " + it)
                            logToFile.wirte("Debugging", "Hyundai text 2 " + text)
                            logToFile.wirte("Debugging", "Hyundai text 3 " + it)
                            DbUtil(baseContext).insert(getHyundaiCardUseHistory(text))
                        }

                        Log.e("TAG", "LJS== 444 ==")
                        logToFile.wirte("Debugging", "Hyundai text 444 ==")
                    } else if (address.equals(ConstCardType.WOORI_CARD_ADDRESS)) {
                        // text : [Web발신] \n 우리카드(7493)매출접수 \n 이*석님 \n 13,000원 \n 12월11일기준 \n 교통-지하철15건
                        // text : [Web발신] \n 우리카드(7493)승인 \n 이*석님 \n 13,000원 일시불 \n 12/15 20:40 \n 베라힐즈...
                        // text : [Web발신] \n [취소완료] \n 우리카드(7493) \n 이*석님 \n 13,000원 \n 12월18일 기준 \n 롯데몰...

                        text?.let {
                            val texts = it.split("\n")
                            texts[1] // 우리카드(7493)매출접수
                            texts[3] // 13,000원

                            val prices = texts[3].split(" ")
                            val dates = texts[4].split(" ")

                            // 카드를 직접 사용하지 않은 항목 중 통신요금은 제외 시킨다.
                            if (texts[1].indexOf("매출접수") > -1 && texts[5].indexOf("통신요금") < 0) {
                                val cardUseHistory = CardUseHistory(
                                    getCardNum(texts[1]),
                                    ConstCardType.CARD_TYPE.WOORI,
                                    getPriceToLong(prices[0]),
                                    ConstDate.RECEIPT_OF_SALES
                                )

                                DbUtil(baseContext).insert(cardUseHistory)
                            } else if (texts[1].indexOf("취소") > -1) {
                                val date = texts[5].substring(0, 5).replace("월", "/")
                                val cardUseHistory = CardUseHistory(
                                    getCardNum(texts[2]),
                                    ConstCardType.CARD_TYPE.WOORI,
                                    getPriceToLong(texts[4]) * -1,
                                    parseDate(date, "00:00")
                                )

                                DbUtil(baseContext).insert(cardUseHistory)
                            } else if (texts[1].indexOf("승인") > -1) {
                                val cardUseHistory = CardUseHistory(
                                    getCardNum(texts[1]),
                                    ConstCardType.CARD_TYPE.WOORI,
                                    getPriceToLong(prices[0]),
                                    parseDate(dates[0], dates[1])
                                )

                                DbUtil(baseContext).insert(cardUseHistory)
                            } else {

                            }
                        }
                    } else if (address.equals(ConstCardType.KB_CARD_ADDRESS)) {
                        //
                    } else {

                    }
                }
            }
        }

//      AlertDialogActivity.open(baseContext, ConstCardType.CARD_TYPE.HYUNDAI)


        // 로컬 브로드 케스트를 날려 CardInfoActivity에 반영
    }

    private fun getHanaCardUseHistory(text: String): CardUseHistory {
        // text : (7*2*)이*석님 12/19 14:29/해외승인/USD 1.00/agoda.com             Singapore    S
        // text : (7*2*)이*석님 12/07 12:53/일시불/승인/30,000원/보들이족발/누적이용....
        val splitTexts = text.split("/")
//        splitTexts[0] // (7*2*)이*석님 12
//        splitTexts[1] // 07 12:53
//        splitTexts[3] // 승인 or 뭘까.. 취소를 안해 봐서.. ;;;
//        splitTexts[4] // 30,000원 - 사용한 가격
//        splitTexts[5] // 사용한 장소 이건 아직 저장 안함.

        // 날짜 계산
        val splitDate = "${splitTexts[0]}/${splitTexts[1]}".split(" ")
//        splitDate[0] // (7*2*)이*석님
//        splitDate[1] // 12/07
//        splitDate[2] // 12:53

        val dateMillis = parseDate(splitDate[1], splitDate[2])

        var price = 0L

        if (splitTexts[2].indexOf("해외") > -1) {
            splitTexts[3] // USD 1.00

            val usd = splitTexts[3].split(" ")[1].toBigDecimal().toDouble()
            price = (usd * 1200).toLong()

            if (splitTexts[2].contains("승인") == false) {
                price = price * -1
            }
        } else {
            price = getPriceToLong(splitTexts[4])

            if (splitTexts[3].contains("승인") == false) {
                price = price * -1
            }
        }

        return CardUseHistory("", ConstCardType.CARD_TYPE.HANA, price, dateMillis)
    }

    private fun getHyundaiCardUseHistory(text: String): CardUseHistory {
        // text : [Web발신]\n" +
        //                "현대카드 블루멤버스 승인\n" +
        //                "이*석\n" +
        //                "19,500원 일시불\n" +
        //                "12/19 15:42\n" +
        //                "브리꼴라쥬\n" +
        //                "누적1,239,717원
        val texts = text.split("\n")

        texts[1] // 현대카드 블루멤버스 승인
        var price = getPriceToLong(texts[3].split(" ")[0]) // 120원 일시불
        Log.e("TAG", "LJS== 1 ==")

        if (texts[1].lastIndexOf("승인") < 0) {
            price = price * -1
        }
        Log.e("TAG", "LJS== 2 ==")

        val dateSplit = texts[4].split(" ") // 12/10 13:35
        Log.e("TAG", "LJS== 3 ==")
        val dateMillis = parseDate(dateSplit[0], dateSplit[1])
        Log.e("TAG", "LJS== 4 ==")
        texts[5] // 스마일페이 - 사용한 장소
        Log.e("TAG", "LJS== 5 ==")

        return CardUseHistory("", ConstCardType.CARD_TYPE.HYUNDAI, price, dateMillis)
    }

    private fun parseDate(dateStr: String, timeStr: String): Long {
        val date = dateStr.split("/")
        val time = timeStr.split(":")

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.MONTH, date[0].toInt() -1)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, date[1].toInt())
        calendar.set(java.util.Calendar.HOUR_OF_DAY, time[0].toInt())
        calendar.set(java.util.Calendar.MINUTE, time[1].toInt())
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    private fun getCardNum(text: String): String {
        val startIndex = text.indexOf("(")
        return text.substring(startIndex, startIndex + 6)
    }

    private fun getPriceToLong(price: String): Long =
        price.substring(0, price.length -1).replace(",", "").toLong()

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
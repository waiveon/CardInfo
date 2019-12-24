package com.sweetsound.cardinfo.utils

import android.content.Context
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.constant.ConstDate
import com.sweetsound.cardinfo.data.CardUseHistory
import com.sweetsound.storeplan.db.DbUtil

class CardUtils {
    companion object {
        fun getPriceToLong(price: String): Long =
            price.substring(0, price.length -1).replace(",", "").toLong()

        fun getDateToMillis(dateStr: String, timeStr: String): Long {
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

        fun saveHyundaiCardUseHistory(context: Context, text: String): CardUseHistory {
            // text : [Web발신]\n" +
            //                "현대카드 블루멤버스 승인\n" +
            //                "이*석\n" +
            //                "19,500원 일시불\n" +
            //                "12/19 15:42\n" +
            //                "브리꼴라쥬\n" +
            //                "누적1,239,717원
            val texts = text.split("\n")

            var price = getPriceToLong(texts[3].split(" ")[0]) // 120원 일시불

            if (texts[1].lastIndexOf("승인") < 0) {
                price = price * -1
            }

            val dateSplit = texts[4].split(" ") // 12/10 13:35
            val dateMillis = getDateToMillis(dateSplit[0], dateSplit[1])

            val cardUseHistory = CardUseHistory("", ConstCardType.CARD_TYPE.HYUNDAI, price, dateMillis, texts[5])
            DbUtil(context).insert(cardUseHistory)

            return cardUseHistory
        }

        fun saveWooriCardUseHistory(context: Context, text: String): CardUseHistory {
            // text : [Web발신] \n 우리카드(7493)매출접수 \n 이*석님 \n 13,000원 \n 12월11일기준 \n 교통-지하철15건
            // text : [Web발신] \n 우리카드(7493)승인 \n 이*석님 \n 13,000원 일시불 \n 12/15 20:40 \n 베라힐즈...
            // text : [Web발신] \n [취소완료] \n 우리카드(7493) \n 이*석님 \n 13,000원 \n 12월18일 기준 \n 롯데몰...
            var cardUseHistory: CardUseHistory = CardUseHistory(ConstCardType.CARD_TYPE.WOORI)

            val texts = text.split("\n")

            if (texts.size > 5) {
                val prices = texts[3].split(" ")
                val dates = texts[4].split(" ")

                // 카드를 직접 사용하지 않은 항목
                if (texts[1].indexOf("매출접수") > -1) {
                    // 통신요금은 제외 시킨다.
                    if (texts[5].indexOf("통신요금") < 0) {
                        cardUseHistory = CardUseHistory(
                            getCardNum(texts[1]),
                            ConstCardType.CARD_TYPE.WOORI,
                            CardUtils.getPriceToLong(prices[0]),
                            ConstDate.RECEIPT_OF_SALES,
                            texts[5]
                        )
                    }

                } else if (texts[1].indexOf("취소") > -1) {
                    val date = texts[5].substring(0, 5).replace("월", "/")
                    cardUseHistory = CardUseHistory(
                        getCardNum(texts[2]),
                        ConstCardType.CARD_TYPE.WOORI,
                        CardUtils.getPriceToLong(texts[4]) * -1,
                        CardUtils.getDateToMillis(date, "00:00"),
                        texts[6]
                    )


                } else { // 승인
                    cardUseHistory = CardUseHistory(
                        getCardNum(texts[1]),
                        ConstCardType.CARD_TYPE.WOORI,
                        CardUtils.getPriceToLong(prices[0]),
                        CardUtils.getDateToMillis(dates[0], dates[1]),
                        texts[5]
                    )
                }

                if (cardUseHistory.price > 0) {
                    DbUtil(context).insert(cardUseHistory)
                }
            }

            return cardUseHistory
        }

        fun parsingSms(context: Context, cardType: ConstCardType.CARD_TYPE, smsBody: String): CardUseHistory {
            var cardUseHistory: CardUseHistory = CardUseHistory(cardType)

            when (cardType) {
                ConstCardType.CARD_TYPE.HYUNDAI -> {
                    cardUseHistory = saveHyundaiCardUseHistory(context, smsBody)
                }

                ConstCardType.CARD_TYPE.WOORI -> {
                    cardUseHistory = saveWooriCardUseHistory(context, smsBody)
                }

                ConstCardType.CARD_TYPE.KB -> {

                }
            }

            return cardUseHistory
        }
    }
}
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
                            getPriceToLong(prices[0]),
                            ConstDate.RECEIPT_OF_SALES,
                            texts[5]
                        )
                    }

                } else if (texts[1].indexOf("취소") > -1) {
                    val date = texts[5].substring(0, 5).replace("월", "/")
                    cardUseHistory = CardUseHistory(
                        getCardNum(texts[2]),
                        ConstCardType.CARD_TYPE.WOORI,
                        getPriceToLong(texts[4]) * -1,
                        getDateToMillis(date, "00:00"),
                        texts[6]
                    )


                } else { // 승인
                    cardUseHistory = CardUseHistory(
                        getCardNum(texts[1]),
                        ConstCardType.CARD_TYPE.WOORI,
                        getPriceToLong(prices[0]),
                        getDateToMillis(dates[0], dates[1]),
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

        fun parsingNoti(context: Context, title: String?, text: String?, bigText: String?): CardUseHistory {
            var cardUseHistory = CardUseHistory(ConstCardType.CARD_TYPE.UNKNOWN)

            title?.let {
                // 하나카드
                if (it.equals("정상승인") == true) {
                    text?.let {
                        cardUseHistory = getHanaCardUseHistory(text)
                        DbUtil(context).insert(cardUseHistory)
                    }
                } else if (it.equals(ConstCardType.HYUNDAI_CARD_NAME) == true) { // 현대카드
                    // text : 이*석님, 현대카드 블루멤버스 승인 5,500원 일시불 12/16 09:31\n휘닉스벤딩서비스
                    // bigText : 이*석님, 현대카드 블루멤버스 승인 16,000원 일시불 01/02 17:09\n스마일페이\n누적835,689원
                    bigText?.let {
                        val texts = it.split(" ")

                        var price = getPriceToLong(texts[4])

                        if (texts[3].equals("승인") == false) {
                            price = price * -1
                        }

                        val splitText = texts[7].split("\n")

                        cardUseHistory = CardUseHistory("", ConstCardType.CARD_TYPE.HYUNDAI, price, getDateToMillis(texts[6], splitText[0]), splitText[1])
                        DbUtil(context).insert(cardUseHistory)
                    }

                } else if (it.indexOf("승인") > -1) { // KB Card or BC 카드 : 승인 12,650원 - BC 카드 어플 에서 뿌리는 Noti라 동일하다.
                    text?.let {
                        // text : 국민BC(3045)승인 이*석님 12,650원 일시불 12/13 10:57 olleh-139770
                        // text : 우리BC(8842)승인 이*석님 12,650원 일시불 12/13 10:57 롯데...
                        // text : 국민BC(3045)승인취소 이*석님 12,650원 일시불 12/13 10:57 롯데...
                        // text : 국민BC(3045)승인취소 이*석님 2,600원 12/31 10:16 (주)휘닉스벤딩서비스
                        // text : 우리BC(8842)승인 이*석님 245,000원 일시불 01/05 13:48 (주) 나무다움
                        val texts = it.split(" ")

                        var cardType: ConstCardType.CARD_TYPE = ConstCardType.CARD_TYPE.UNKNOWN
                        var cardNum: String = ""

                        if (texts[0].indexOf("국민") > -1) {
                            cardType = ConstCardType.CARD_TYPE.KB
                        } else if (texts[0].indexOf("우리") > -1) {
                            cardType = ConstCardType.CARD_TYPE.WOORI
                            cardNum = getCardNum(texts[0])
                        }

                        var price = getPriceToLong(texts[2])
                        if (texts[0].lastIndexOf("취소") > -1) {
                            price = price * -1
                        }

                        val timeAndPlace = getTimeAndPlace(texts.asReversed())

                        if (timeAndPlace[2].lastIndexOf("9770") < 0) { // 통신료가 아니라면 저장한다.
                            cardUseHistory = CardUseHistory(cardNum, cardType, price, getDateToMillis(timeAndPlace[0], timeAndPlace[1]), timeAndPlace[2])
                            DbUtil(context).insert(cardUseHistory)
                        }
                    }
                } else {
                    val address = it.replace("-", "")

                    if (address.equals(ConstCardType.HYUNDAI_CARD_ADDRESS)) {
                        // text : [Web발신] \n 현대카드 블루멤버스 승인 \n 이*석 \n 120원 일시불 \n 12/10 13ㅣ35 \n 스마일페이 \n...
                        text?.let {
                            cardUseHistory = saveHyundaiCardUseHistory(context, it)
                        }
                    } else if (address.equals(ConstCardType.WOORI_CARD_ADDRESS)) {
                        // text : [Web발신] \n 우리카드(7493)매출접수 \n 이*석님 \n 13,000원 \n 12월11일기준 \n 교통-지하철15건
                        // text : [Web발신] \n 우리카드(7493)승인 \n 이*석님 \n 13,000원 일시불 \n 12/15 20:40 \n 베라힐즈...
                        // text : [Web발신] \n [취소완료] \n 우리카드(7493) \n 이*석님 \n 13,000원 \n 12월18일 기준 \n 롯데몰...

                        text?.let {
                            cardUseHistory = saveWooriCardUseHistory(context, it)
                        }
                    } else if (address.equals(ConstCardType.KB_CARD_ADDRESS)) {
                        //
                    } else {

                    }
                }
            }

            return cardUseHistory
        }

        private fun getTimeAndPlace(texts: List<String>): List<String> {
            var place: String = ""
            var results = arrayListOf<String>()

            run loop@ {
                texts.forEachIndexed { index, string ->
                    if (string.indexOf(":") < 0) {
                        place = string + place
                    } else {
                        results.add(texts[index +1])
                        results.add(string)
                        results.add(place)

                        return@loop;
                    }
                }
            }

            return results
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

            val dateMillis = getDateToMillis(splitDate[1], splitDate[2])

            var price = 0L

            if (splitTexts[2].indexOf("해외") > -1) {
                splitTexts[3] // USD 1.00

                val priceInfos = splitTexts[3].split(" ")

                val usd = priceInfos[1].toBigDecimal().toDouble()

                if (priceInfos[0].toLowerCase().equals("usd")) {
                    price = (usd * 1200).toLong()
                } else {
                    price = usd.toLong()
                }

                if (splitTexts[2].contains("승인") == false) {
                    price = price * -1
                }
            } else {
                price = getPriceToLong(splitTexts[4])

                if (splitTexts[3].contains("승인") == false) {
                    price = price * -1
                }
            }

            return CardUseHistory("", ConstCardType.CARD_TYPE.HANA, price, dateMillis, splitTexts[5])
        }
    }
}
package com.sweetsound.cardinfo.data

import android.content.ContentValues
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.storeplan.db.DbUtil

data class CardUseHistory(var cardNum: String = "", // 우리카드의 경우 카드 넘버로 가족카드를 구분 한다.
                          val cardType: ConstCardType.CARD_TYPE,
                          var price: Long = 0, // 합산을 위해 Long Type이 편함
                          val date: Long = 0) { // Calendar에 다시 넣어야 하기 때문에 Long Type이 편함

    constructor(cardType: ConstCardType.CARD_TYPE,
                date: Long): this("", cardType, 0, date) {

    }

    fun parseSms(sms: String) {
        var isApproval = false

        when (cardType) {
            ConstCardType.CARD_TYPE.HYUNDAI -> {

                val smsSplits = sms.split("\n")
                smsSplits.forEach {
                    if (it.lastIndexOf("승인") > -1) {
                        isApproval = true
                    }

                    val index = it.lastIndexOf("원 일시불")

                    if (index > -1) {
                        price = it.substring(0, index).replace(",", "").toLong()
                    }
                }
            }

            ConstCardType.CARD_TYPE.WOORI -> {
                val smsSplits = sms.split("\n")
                smsSplits.forEach {

                    // 우리(7493) 승인
                    // 우리(7493) 승인취소
                    // xx,xxx원 일시불
                    if (it.lastIndexOf("취소") > -1) {
                        isApproval = false
                    }else if (it.lastIndexOf("승인") > -1) {
                        isApproval = true
                    }

                    val index = it.lastIndexOf("원 일시불")

                    if (index > -1) {
                        price = it.substring(0, index).replace(",", "").toLong()
                    }

                    if (it.indexOf("통신요금") > 0) {
                        price = 0
                    }
                }
            }

            ConstCardType.CARD_TYPE.KB -> {

            }

        }

        // 취소건은 - 로 저장하여 나중에 합산 할 때 뺄 수 있도록 한다.
        if (isApproval == false) {
            price = -price
        }
    }

    fun getValueForDb(): ContentValues {
        val values = ContentValues()
        values.put(DbUtil.COLUMN_CARD_NUMBER, cardNum)
        values.put(DbUtil.COLUMN_CARD_TYPE, ConstCardType.getIntValue(cardType))
        values.put(DbUtil.COLUMN_DATE, date)
        values.put(DbUtil.COLUMN_PRICE, price)

        return values
    }

    fun toMap() {
    }
}
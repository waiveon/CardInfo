package com.sweetsound.cardinfo.data

import android.content.ContentValues
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.storeplan.db.DbUtil

data class CardUseHistory(var cardNum: String = "", // 우리카드의 경우 카드 넘버로 가족카드를 구분 한다.
                          val cardType: ConstCardType.CARD_TYPE,
                          var price: Long = 0, // 합산을 위해 Long Type이 편함
                          var date: Long = 0, // Calendar에 다시 넣어야 하기 때문에 Long Type이 편함
                          var place: String = "") {

    constructor(cardType: ConstCardType.CARD_TYPE): this("", cardType, 0, 0, "") {

    }

    fun getValueForDb(): ContentValues {
        val values = ContentValues()
        values.put(DbUtil.COLUMN_CARD_NUMBER, cardNum)
        values.put(DbUtil.COLUMN_CARD_TYPE, ConstCardType.getIntValue(cardType))
        values.put(DbUtil.COLUMN_DATE, date)
        values.put(DbUtil.COLUMN_PRICE, price)
        values.put(DbUtil.COLUMN_PLACE, place)

        return values
    }

    fun toMap() {
    }
}
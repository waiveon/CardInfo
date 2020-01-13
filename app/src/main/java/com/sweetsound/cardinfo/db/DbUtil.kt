package com.sweetsound.storeplan.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.data.CardUseHistory
import com.sweetsound.logtofile.LogToFile

class DbUtil(val context: Context) {
    private val db: SQLiteDatabase

    init {
        db = DbHelper(context, DB_NAME).writableDatabase
    }

    companion object {
        val DB_VERSION = 1
        val DB_NAME = "card_info.db"

        val TABLE_NAME = "card_info"

        val COLUMN_DATE = "date"
        val COLUMN_CARD_TYPE = "card_type"
        val COLUMN_PRICE = "price"
        val COLUMN_CARD_NUMBER = "card_number" // 우리카드의 경우 카드 넘버로 가족카드를 구분 한다.
        val COLUMN_PLACE = "place"
    }

    val COLUMNS = arrayOf(COLUMN_DATE, COLUMN_CARD_TYPE, COLUMN_PRICE, COLUMN_CARD_NUMBER, COLUMN_PLACE)
    val COLUMN_INDEX_DATE = 0
    val COLUMN_INDEX_CARD_TYPE = 1
    val COLUMN_INDEX_PROCE = 2
    val COLUMN_INDEX_CARD_NUMBER = 3
    val COLUMN_INDEX_PLACE = 4

    val DEFAULT_ORDER_BY = "${COLUMN_CARD_TYPE} DESC, ${COLUMN_DATE} DESC"

    fun insert(cardUseHistory: CardUseHistory): Long {
        val logToFile = LogToFile(context)
        logToFile.wirte(DbUtil::class.java.simpleName, cardUseHistory.toString())

        return db.insert(TABLE_NAME, null, cardUseHistory.getValueForDb())
    }

    fun select(): MutableMap<ConstCardType.CARD_TYPE, MutableList<CardUseHistory>> = select(null)

    fun select(intCardType: Int): MutableMap<ConstCardType.CARD_TYPE, MutableList<CardUseHistory>> {
        var cardUseHistory: CardUseHistory
        var cardUseHistorys: MutableList<CardUseHistory>
        val cardUseHistoryMap = mutableMapOf<ConstCardType.CARD_TYPE, MutableList<CardUseHistory>>()

        var where: String? = null

        if (intCardType != ConstCardType.CARD_TYPE.UNKNOWN.value) {
            where = "${COLUMN_CARD_TYPE} = ${intCardType}"
        }

        val cursor = db.query(TABLE_NAME, COLUMNS, where, null, null, null, DEFAULT_ORDER_BY)

        if (cursor != null) {
            while (cursor.moveToNext() == true) {
                cardUseHistory = CardUseHistory(cursor.getString(COLUMN_INDEX_CARD_NUMBER),
                    ConstCardType.getCardType(cursor.getInt(COLUMN_INDEX_CARD_TYPE)),
                    cursor.getLong(COLUMN_INDEX_PROCE),
                    cursor.getLong(COLUMN_INDEX_DATE),
                    cursor.getString(COLUMN_INDEX_PLACE))

                if (cardUseHistoryMap.containsKey(cardUseHistory.cardType) == true) {
                    cardUseHistorys = cardUseHistoryMap.get(cardUseHistory.cardType)!!
                } else {
                    cardUseHistorys = mutableListOf<CardUseHistory>()
                    cardUseHistoryMap.put(cardUseHistory.cardType, cardUseHistorys)
                }

                cardUseHistorys.add(cardUseHistory)
            }

            cursor.close()
        }

        return cardUseHistoryMap
    }

    fun select(cardType: ConstCardType.CARD_TYPE?): MutableMap<ConstCardType.CARD_TYPE, MutableList<CardUseHistory>> {
        var intCardType = ConstCardType.CARD_TYPE.UNKNOWN.value

        cardType?.let {
            intCardType = ConstCardType.getIntValue(cardType)
        }

        return select(intCardType)
    }

    fun selectTotalPrice(intCardType: Int): Long {
        var totalPrice = 0L

        val cursor = db.query(TABLE_NAME, COLUMNS, "${COLUMN_CARD_TYPE} = ${intCardType}", null, null, null, DEFAULT_ORDER_BY)

        if (cursor != null) {
            while (cursor.moveToNext() == true) {
                totalPrice += cursor.getLong(COLUMN_INDEX_PROCE)
            }

            cursor.close()
        }

        return totalPrice
    }

    fun selectTotalPriceByManual(intCardType: Int, cardNum: String): Long {
        var totalPrice = 0L

        val cursor = db.query(TABLE_NAME, COLUMNS,
            cardNum.let {
                var where = "${COLUMN_CARD_TYPE} = ${intCardType} AND ${COLUMN_DATE} = 0"
                if (it.isEmpty() == false) {
                    where = "${COLUMN_CARD_NUMBER} = ${it} AND ${where}"
                }

                where
            },
            null, null, null, DEFAULT_ORDER_BY)

        if (cursor != null) {
            while (cursor.moveToNext() == true) {
                totalPrice += cursor.getLong(COLUMN_INDEX_PROCE)
            }

            cursor.close()
        }

        return totalPrice
    }

    fun selectCardType(): MutableList<String> {
        val cardTypes = mutableListOf<String>()

        val cursor = db.query(TABLE_NAME, arrayOf(COLUMN_CARD_TYPE, COLUMN_CARD_NUMBER), null, null, null, null, DEFAULT_ORDER_BY)

        val CARD_TYPE_INDEX = 0
        val CARD_NUM_INDEX = 1

        cursor?.let {
            while (it.moveToNext() == true) {
                val intCardType = it.getInt(CARD_TYPE_INDEX)
                val cardType = ConstCardType.getCardName(intCardType) + " " + it.getString(CARD_NUM_INDEX)

                if (cardTypes.contains(cardType) == false) {
                    cardTypes.add(cardType)
                }
            }

            it.close()
        }

        return cardTypes
    }

    fun updatePriceByManual(cardUseHistory: CardUseHistory): Int {
        return update(cardUseHistory, "${COLUMN_CARD_TYPE} = ${cardUseHistory.cardType.value} AND ${COLUMN_DATE} = 0")
    }

    fun update(cardUseHistory: CardUseHistory, where: String): Int {
        return db.update(TABLE_NAME, cardUseHistory.getValueForDb(), where, null)
    }

    fun deletePriceByManual(intCardType: Int): Int {
        return delete("${COLUMN_CARD_TYPE} = ${intCardType} AND ${COLUMN_DATE} = 0")
    }

    fun delete(where: String?): Int {
        return db.delete(TABLE_NAME, where, null)
    }
}


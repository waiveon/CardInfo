package com.sweetsound.storeplan.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sweetsound.storeplan.db.DbUtil.Companion.COLUMN_CARD_NUMBER
import com.sweetsound.storeplan.db.DbUtil.Companion.COLUMN_CARD_TYPE
import com.sweetsound.storeplan.db.DbUtil.Companion.COLUMN_DATE
import com.sweetsound.storeplan.db.DbUtil.Companion.COLUMN_PRICE
import com.sweetsound.storeplan.db.DbUtil.Companion.DB_VERSION

class DbHelper(context: Context,
               val dbName: String): SQLiteOpenHelper(context, dbName, null, DB_VERSION) {

    val CREATE_TABLE_CARD_INFO: String = "CREATE TABLE ${DbUtil.TABLE_NAME} (" +
            "${COLUMN_DATE} INTEGER," +
            "${COLUMN_CARD_TYPE} INTEGER," +
            "${COLUMN_PRICE} INTEGER," +
            "${COLUMN_CARD_NUMBER} TEXT," +
            "PRIMARY KEY (${COLUMN_DATE}, ${COLUMN_CARD_TYPE})" +
            ")"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_CARD_INFO)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        when(oldVersion) {
            1 -> {

            }
        }
    }
}
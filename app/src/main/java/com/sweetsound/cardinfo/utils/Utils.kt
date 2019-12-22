package com.sweetsound.cardinfo.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class Utils {
    companion object {
        fun getNumberWithComma(number: Long): String {
            return  NumberFormat.getInstance().format(number)
        }

        fun stringToHex(target: String): String {
            var key = ""

            target.forEach {
                key += Integer.toHexString(it.toInt())
            }

            return key
        }

        fun millisToDate(millis: Long): String {
            val formatter = SimpleDateFormat("yyyy.MM.dd hh:mm")
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = millis

            return formatter.format(calendar.time)
        }
    }
}
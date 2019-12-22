package com.sweetsound.cardinfo.firbase

import com.sweetsound.cardinfo.utils.Utils

class EmailAddress(emailAddress: String,
                   val cardAddresses: List<CardAddress>) {

    var emailAddress: String = emailAddress
    set(value) {
        field = Utils.stringToHex(value)
    }
}
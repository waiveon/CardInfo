package com.sweetsound.cardinfo.constant

class ConstCardType {
    companion object {
        val WOORI_CARD_ADDRESS = "15889955"
        val HYUNDAI_CARD_ADDRESS = "15776200"
        val KB_CARD_ADDRESS = "15881688"
        val HANA_CARD_ADDRESS = ""

        val WOORI_CARD_NAME = "우리카드"
        val HYUNDAI_CARD_NAME = "현대카드"
        val KB_CARD_NAME = "KB국민카드"
        val HANA_CARD_NAME = "하나카드"

        fun getCardType(address: String): CARD_TYPE =
            when (address) {
                WOORI_CARD_ADDRESS -> CARD_TYPE.WOORI

                HYUNDAI_CARD_ADDRESS -> CARD_TYPE.HYUNDAI

                KB_CARD_ADDRESS -> CARD_TYPE.KB

                HANA_CARD_ADDRESS -> CARD_TYPE.HANA

                else -> CARD_TYPE.UNKNOWN
            }

        fun getCardType(intCardType: Int): CARD_TYPE {
            var cardType: CARD_TYPE = CARD_TYPE.UNKNOWN

            CARD_TYPE.values().forEach {
                if (it.value == intCardType) {
                    cardType = it
                }
            }

            return cardType
        }

        fun getCardName(intCardType: Int): String {
            val cardType = getCardType(intCardType)

            when (cardType) {
                CARD_TYPE.WOORI -> return WOORI_CARD_NAME

                CARD_TYPE.HYUNDAI -> return HYUNDAI_CARD_NAME

                CARD_TYPE.HANA -> return HANA_CARD_NAME

                CARD_TYPE.KB -> return KB_CARD_NAME

                else -> return ""
            }
        }

        fun getIntValue(cardType: CARD_TYPE): Int =
            cardType.value

        fun getIntValue(cardName: String) =
            when (cardName) {
                WOORI_CARD_NAME -> CARD_TYPE.WOORI.value

                HYUNDAI_CARD_NAME -> CARD_TYPE.HYUNDAI.value

                HANA_CARD_NAME -> CARD_TYPE.HANA.value

                KB_CARD_NAME -> CARD_TYPE.KB.value

                else -> CARD_TYPE.UNKNOWN.value
            }
    }

    enum class CARD_TYPE(val value: Int) {
        UNKNOWN(0),
        WOORI(1),
        HYUNDAI(2),
        HANA(3),
        KB(4)
    }
}
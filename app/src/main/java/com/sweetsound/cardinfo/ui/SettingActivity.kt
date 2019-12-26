package com.sweetsound.cardinfo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sweetsound.cardinfo.R
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.constant.ConstDate
import com.sweetsound.cardinfo.data.CardUseHistory
import com.sweetsound.cardinfo.utils.Utils
import com.sweetsound.storeplan.db.DbUtil
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity() : AppCompatActivity() {
    private val CARD_INDEX_CHOICE = 0
    private val CARDS_INDEX_WOORI = 1
    private val CARDS_INDEX_HYUNDAI = 2
    private val CARDS_INDEX_KB = 3
    private val CARDS_INDEX_HANA = 4

    private var mIntCardType = ConstCardType.CARD_TYPE.UNKNOWN.value

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        setCardItems(getCardItemsFromDb())

        val CARDS = arrayListOf<String>(
            getString(R.string.choice),
            ConstCardType.WOORI_CARD_NAME,
            ConstCardType.HYUNDAI_CARD_NAME,
            ConstCardType.KB_CARD_NAME,
            ConstCardType.HANA_CARD_NAME
        )

        manual_input_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == false) {
                setCardItems(getCardItemsFromDb())
            } else {
                setCardItems(CARDS)
            }
        }

        apply_button.setOnClickListener {
            if (TextUtils.isEmpty(added_price_textview.text) == false && TextUtils.isEmpty(add_price_exittext.text) == false) {
                var addPrice = 0L
                val dbUtils = DbUtil(baseContext)

                if (add_price_exittext.text.toString().equals("0") == true) {
                    val delcaount = dbUtils.deletePriceByManual(mIntCardType)
                } else {
                    var cardNum = card_num_exittext.text.toString()

                    if (TextUtils.isEmpty(cardNum) == false) {
                        cardNum = "(" + cardNum + ")"
                    }

                    addPrice = added_price_textview.text.toString().replace(",", "").toLong() + add_price_exittext.text.toString().toLong()
                    val cardUseHistory = CardUseHistory(cardNum, ConstCardType.getCardType(mIntCardType), addPrice, ConstDate.MANUAL_INPUT, getString(R.string.manual_input))

                    if (dbUtils.insert(cardUseHistory) == -1L) {
                        // update 해야 함
                        dbUtils.updatePriceByManual(cardUseHistory)
                    }
                }

                added_price_textview.text = Utils.getNumberWithComma(addPrice)

                setTotalPrice()
            } else {
                Toast.makeText(baseContext, R.string.input_card_and_price, Toast.LENGTH_SHORT).show()
            }
        }

        close_button.setOnClickListener {
            finish()
        }
    }

    private fun setTotalPrice() {
        total_price_textview.text = Utils.getNumberWithComma(DbUtil(baseContext).selectTotalPrice(mIntCardType))
    }

    private fun setCardItems(cards: List<String>) {
        val cardNameAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cards)

        card_name_spinner.adapter = cardNameAdapter
        card_name_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (CARD_INDEX_CHOICE != position) {
                    val itemSplit = (card_name_spinner.selectedItem as String).split(" ")
//                    itemSplit[0] // 카드 이름
//                    itemSplit[1] // 카드 번호

                    mIntCardType = ConstCardType.getIntValue(itemSplit[0])

                    if (mIntCardType != ConstCardType.CARD_TYPE.UNKNOWN.value) {
                        var cardNum: String = ""

                        // 카드 번호가 있다면 표시하고 수정하지 못하도록 설정
                        if (itemSplit.size > 1) {
                            cardNum = itemSplit[1]

                            editable(card_num_exittext, false)
                            card_num_exittext.setText(cardNum.substring(1, cardNum.length - 1))
                        } else {
                            // 카드 번호가 없는데 우리카드면 카드 번호를 수정할 수 있도록 설정
                            if (mIntCardType == ConstCardType.CARD_TYPE.WOORI.value) {
                                editable(card_num_exittext, true)
                                card_num_exittext.setText("")
                            } else { // 카드 번호가 없는데 우리카드도 아니면 카드 번호를 수정 ㅎ
                                editable(card_num_exittext, false)
                                card_num_exittext.setText(getString(R.string.no_type_card_number))
                            }
                        }

                        added_price_textview.text =
                            Utils.getNumberWithComma(DbUtil(baseContext).selectTotalPriceByManual(mIntCardType, cardNum))

                        add_price_exittext.requestFocus()
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                        imm.showSoftInput(add_price_exittext, InputMethodManager.SHOW_IMPLICIT)

                        setTotalPrice()
                    }
                }
            }
        }
    }

    private fun getCardItemsFromDb(): MutableList<String> {
        val cards = DbUtil(baseContext).selectCardType()
        cards.add(0, getString(R.string.choice))

        return cards
    }

    private fun editable(view: EditText, isAble: Boolean) {
        view.isClickable = isAble
        view.isCursorVisible = isAble
        view.isFocusable = isAble
        view.isFocusableInTouchMode = isAble
    }
}
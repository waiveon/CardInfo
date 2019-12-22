package com.sweetsound.cardinfo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.sweetsound.cardinfo.R
import com.sweetsound.cardinfo.adapter.CardUseListAdapter
import com.sweetsound.cardinfo.constant.ConstCardType
import com.sweetsound.cardinfo.data.CardUseHistory
import com.sweetsound.storeplan.db.DbUtil
import kotlinx.android.synthetic.main.activity_card_use_list.*

class CardUseListActivity() : AppCompatActivity() {
    companion object {
        val CARD_TYPE = "CARD_TYPE"

        fun open(context: Context, intCardType: Int) {
            val intent = Intent(context, CardUseListActivity::class.java)
            intent.putExtra(CARD_TYPE, intCardType)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_use_list)

        val intCardType = intent.getIntExtra(CARD_TYPE, ConstCardType.CARD_TYPE.UNKNOWN.value)

        val cardUseHistorys: MutableList<CardUseHistory>? = DbUtil(baseContext).select(intCardType).get(ConstCardType.getCardType(intCardType))

        if (cardUseHistorys != null) {
            use_list_recyclerview.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            use_list_recyclerview.adapter = CardUseListAdapter(cardUseHistorys)
            use_list_recyclerview.layoutManager = LinearLayoutManager(applicationContext)
        } else {
            use_list_recyclerview.visibility = View.GONE
            empty_list_textview.visibility = View.VISIBLE
        }
    }
}
package com.sweetsound.cardinfo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sweetsound.cardinfo.R
import com.sweetsound.cardinfo.constant.ConstDate
import com.sweetsound.cardinfo.data.CardUseHistory
import com.sweetsound.cardinfo.utils.Utils
import kotlinx.android.synthetic.main.activity_card_use_list_item.view.*

class CardUseListAdapter(private val items: MutableList<CardUseHistory>) : RecyclerView.Adapter<CardUseListAdapter.ItemViewHolder> () {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.activity_card_use_list_item, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        holder.apply {
            bind(item)
        }
    }

    inner class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: CardUseHistory) {
            var dateText: String = ""

            when (item.date) {
                ConstDate.MANUAL_INPUT -> dateText = view.context.getString(R.string.manual_input)

                ConstDate.RECEIPT_OF_SALES -> dateText = view.context.getString(R.string.receipt_of_sales)

                else -> dateText = Utils.millisToDate(item.date)
            }
            view.date_textview.text = dateText
            view.price_textview.text = "${Utils.getNumberWithComma(item.price)}원"
        }
    }
}
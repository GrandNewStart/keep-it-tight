package dev.jinwoo.reflection.expense

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.jinwoo.reflection.DateUtils
import dev.jinwoo.reflection.R
import dev.jinwoo.reflection.Theme
import dev.jinwoo.reflection.db.Expense

class ExpenseAdapter(
    expenses: List<Expense>,
    private val mode: Theme,
    private val listener: ExpenseAdapterListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var groupedItems: List<RecyclerViewItem> = groupExpensesByDate(expenses)

    private fun groupExpensesByDate(expenses: List<Expense>): List<RecyclerViewItem> {
        val groupedItems = mutableListOf<RecyclerViewItem>()


        expenses.groupBy { DateUtils.formatTimestampToDateOnly(it.date.toLong()) }
            .forEach { (date, expensesForDate) ->
                groupedItems.add(RecyclerViewItem.Header(date))
                expensesForDate.forEach { expense ->
                    groupedItems.add(RecyclerViewItem.ExpenseItem(expense))
                }
            }
        return groupedItems
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: RecyclerViewItem.Header) {
            (itemView as? MaterialButton)?.let {
                it.backgroundTintList = ColorStateList.valueOf(
                    it.resources.getColor(
                        if (mode == Theme.Dark) R.color.darkSecondaryBackground else R.color.lightSecondaryBackground,
                        null
                    )
                )
                it.rippleColor = ColorStateList.valueOf(
                    it.resources.getColor(
                        if (mode == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground,
                        null
                    )
                )
                it.setTextColor(
                    itemView.resources.getColor(
                        if (mode == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText,
                        null
                    )
                )
                it.text = item.date
                it.setOnClickListener {
                    listener?.onClick(item.date)
                }
            }
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: RecyclerViewItem.ExpenseItem) {
            val transparent = itemView.resources.getColor(R.color.transparent, null)
            val primaryTXT = itemView.resources.getColor(
                if (mode == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText, null
            )
            val secondaryBG = itemView.resources.getColor(
                if (mode == Theme.Dark) R.color.darkSecondaryBackground else R.color.lightSecondaryBackground,
                null
            )
            val primaryBG = itemView.resources.getColor(
                if (mode == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground,
                null
            )
            itemView.apply {
                (this as? MaterialCardView)?.let { root ->
                    root.isClickable = true
                    root.setBackgroundColor(transparent)
                    root.rippleColor = ColorStateList.valueOf(secondaryBG)
                    root.setOnClickListener {
                        listener?.onClick(item.expense)
                    }
                    setText(
                        findViewById(R.id.nameText), item.expense.name, primaryTXT
                    )
                    setText(
                        findViewById(R.id.costText), "${item.expense.cost}", primaryTXT
                    )
                    setText(
                        findViewById(R.id.optionText), item.expense.tag, primaryTXT
                    )
                }
            }
        }

        private fun setText(textView: TextView, text: String, color: Int) {
            textView.text = text
            textView.setTextColor(color)
        }
    }

    interface ExpenseAdapterListener {
        fun onClick(item: Expense)
        fun onClick(date: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_HEADER) {
            val itemView =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_expense_header, parent, false)
            return HeaderViewHolder(itemView)
        } else if (viewType == TYPE_ITEM) {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
            return ItemViewHolder(itemView)
        } else {
            throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int {
        return groupedItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = groupedItems[position]) {
            is RecyclerViewItem.Header -> (holder as HeaderViewHolder).bind(item)
            is RecyclerViewItem.ExpenseItem -> (holder as ItemViewHolder).bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (groupedItems[position]) {
            is RecyclerViewItem.Header -> TYPE_HEADER
            is RecyclerViewItem.ExpenseItem -> TYPE_ITEM
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}
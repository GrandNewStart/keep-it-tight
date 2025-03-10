package dev.bluelemonade.ledger.expense

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Colors
import dev.bluelemonade.ledger.db.Expense

class ExpenseAdapter(
    expenses: List<Expense>,
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
                it.backgroundTintList = ColorStateList.valueOf(Colors.secondary)
                it.rippleColor = ColorStateList.valueOf(Colors.primary)
                it.text = item.date
                it.setOnClickListener {
                    listener?.onClick(item.date)
                }
            }
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: RecyclerViewItem.ExpenseItem) {
            itemView.apply {
                (this as? MaterialCardView)?.let { root ->
                    root.isClickable = true
                    root.setBackgroundColor(Colors.transparent)
                    root.rippleColor = ColorStateList.valueOf(Colors.secondaryBackground)
                    root.setOnClickListener {
                        listener?.onClick(item.expense)
                    }
                    setText(
                        findViewById(R.id.nameText), item.expense.name, Colors.primaryText
                    )
                    if (item.expense.cost < 0) {
                        setText(
                            findViewById(R.id.costText), "${-item.expense.cost}", Colors.green
                        )
                    } else {
                        setText(
                            findViewById(R.id.costText), "${item.expense.cost}", Colors.red
                        )
                    }

                    setText(
                        findViewById(R.id.optionText), item.expense.tag, Colors.primaryText
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
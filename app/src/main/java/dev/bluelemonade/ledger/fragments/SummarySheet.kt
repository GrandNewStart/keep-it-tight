package dev.bluelemonade.ledger.fragments

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.db.Expense
import java.util.Calendar
import java.util.Date

class SummarySheet(
    private val expenses: MutableLiveData<List<Expense>>,
    private val tags: MutableLiveData<List<String>>,
    private val theme: MutableLiveData<Theme>,
    private val date: String? = null
) :
    BottomSheetDialogFragment() {

    private lateinit var titleText: TextView
    private lateinit var yearlyText: TextView
    private lateinit var yearlyTotalText: TextView
    private lateinit var monthlyText: TextView
    private lateinit var monthlyTotalText: TextView
    private lateinit var dailyText: TextView
    private lateinit var dailyTotalText: TextView
    private lateinit var tagSpinner: Spinner

    private val tagLiveData = MutableLiveData("All")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_summary, container, false).apply {
            initView(this)
            setSpinner()
            observe()
        }

    }

    private fun initView(view: View) {
        view.apply {
            titleText = findViewById(R.id.titleText)
            date?.let { titleText.text = date }
            yearlyText = findViewById(R.id.yearlyText)
            yearlyTotalText = findViewById(R.id.yearlyTotalText)
            yearlyText.visibility = if (date == null) View.VISIBLE else View.GONE
            yearlyTotalText.visibility = if (date == null) View.VISIBLE else View.GONE
            monthlyText = findViewById(R.id.monthlyText)
            monthlyTotalText = findViewById(R.id.monthlyTotalText)
            monthlyText.visibility = if (date == null) View.VISIBLE else View.GONE
            monthlyTotalText.visibility = if (date == null) View.VISIBLE else View.GONE
            dailyText = findViewById(R.id.dailyText)
            date?.let { dailyText.text = getString(R.string.this_day) }
            dailyTotalText = findViewById(R.id.dailyTotalText)
            tagSpinner = findViewById(R.id.tagSpinner)
        }
    }

    private fun setSpinner() {
        val primaryTXT = if (theme.value == Theme.Dark) resources.getColor(
            R.color.darkPrimaryText,
            null
        ) else resources.getColor(R.color.lightPrimaryText, null)
        tagSpinner.backgroundTintList = ColorStateList.valueOf(primaryTXT)
        tagSpinner.foregroundTintList = ColorStateList.valueOf(primaryTXT)
        tagSpinner.popupBackground.setTint(
            resources.getColor(
                R.color.lightPrimaryBackground,
                null
            )
        )
        (tagSpinner.getChildAt(0) as? TextView)?.setTextColor(primaryTXT)

        val dropdownItems = ArrayList<String>()
        dropdownItems.add(getString(R.string.no_tag))
        dropdownItems.addAll(tags.value!!.toMutableList())
        dropdownItems.add(getString(R.string.all_tag))
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dropdownItems
        ).let { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            tagSpinner.adapter = adapter
        }
        tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val textColor =
                    resources.getColor(
                        if (theme.value == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText,
                        null
                    )
                (view as? TextView)?.setTextColor(textColor)
                if (date == null)
                    calculateSum(expenses.value!!, dropdownItems[position])
                else
                    calculateSumForDate(expenses.value!!, dropdownItems[position], date)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        tagSpinner.setSelection(dropdownItems.size - 1)
    }

    private fun observe() {
        theme.observe(viewLifecycleOwner) { theme ->
            val primaryBG = resources.getColor(
                if (theme == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground,
                null
            )
            val primaryTXT = resources.getColor(
                if (theme == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText,
                null
            )
            view?.setBackgroundColor(primaryBG)
            titleText.setTextColor(primaryTXT)
            yearlyText.setTextColor(primaryTXT)
            yearlyTotalText.setTextColor(primaryTXT)
            monthlyText.setTextColor(primaryTXT)
            monthlyTotalText.setTextColor(primaryTXT)
            dailyText.setTextColor(primaryTXT)
            dailyTotalText.setTextColor(primaryTXT)
        }
        expenses.observe(viewLifecycleOwner) { expenses ->
            if (date == null)
                calculateSum(expenses, tagLiveData.value!!)
            else
                calculateSumForDate(expenses, tagLiveData.value!!, date)
        }
        tagLiveData.observe(viewLifecycleOwner) { tag ->
            if (date == null)
                calculateSum(expenses.value!!, tag)
            else
                calculateSumForDate(expenses.value!!, tag, date)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun calculateSumForDate(expenses: List<Expense>, tag: String, date: String) {
        var total = 0
        expenses
            .filter { DateUtils.formatTimestampToDateOnly(it.date.toLong()) == date }
            .filter { tag == getString(R.string.all_tag) || it.tag == tag }
            .forEach { total += it.cost }
        dailyTotalText.text = "₩ $total"
    }

    @SuppressLint("SetTextI18n")
    private fun calculateSum(expenses: List<Expense>, tag: String) {
        val today = Date()
        val calendar = Calendar.getInstance()
        calendar.time = today
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        var yearTotal = 0
        var monthTotal = 0
        var dayTotal = 0

        expenses
            .filter { tag == getString(R.string.all_tag) || it.tag == tag }
            .forEach {
                val expenseDate = Date(it.date.toLong())
                calendar.time = expenseDate
                val expenseYear = calendar.get(Calendar.YEAR)
                val expenseMonth = calendar.get(Calendar.MONTH) + 1
                val expenseDay = calendar.get(Calendar.DAY_OF_MONTH)

                if (expenseYear == currentYear) yearTotal += it.cost
                if (expenseMonth == currentMonth) monthTotal += it.cost
                if (expenseDay == currentDay) dayTotal += it.cost
            }

        yearlyTotalText.text = "₩ $yearTotal"
        monthlyTotalText.text = "₩ $monthTotal"
        dailyTotalText.text = "₩ $dayTotal"
    }

}
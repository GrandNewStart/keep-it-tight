package dev.bluelemonade.ledger.fragments

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.bluelemonade.ledger.GlobalApplication
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Colors
import dev.bluelemonade.ledger.comm.Strings
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.databinding.FragmentSummaryBinding
import dev.bluelemonade.ledger.databinding.ItemSpinnerBinding
import dev.bluelemonade.ledger.db.Expense
import java.util.Calendar
import java.util.Date

class SummarySheet(
    private val date: String? = null
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSummaryBinding
    private val tagLiveData = MutableLiveData("All")
    private val app = GlobalApplication.instance

    override fun getTheme(): Int {
        return R.style.Theme_BottomSheetDialog_Fullscreen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummaryBinding.inflate(layoutInflater)
        binding.apply {
            // Text setup
            yearlyText.visibility = if (date == null) View.VISIBLE else View.GONE
            yearlyTotalText.visibility = if (date == null) View.VISIBLE else View.GONE
            monthlyText.visibility = if (date == null) View.VISIBLE else View.GONE
            monthlyTotalText.visibility = if (date == null) View.VISIBLE else View.GONE
            date?.let {
                dailyText.text = Strings.this_day
                titleText.text = date
            }

            // Tag spinner setup
            tagSpinner.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
            tagSpinner.foregroundTintList = ColorStateList.valueOf(Colors.primaryText)
            tagSpinner.popupBackground.setTint(
                resources.getColor(
                    R.color.lightPrimaryBackground,
                    null
                )
            )
            (tagSpinner.getChildAt(0) as? TextView)?.setTextColor(Colors.primaryText)

            val dropdownItems = ArrayList<String>()
            dropdownItems.add(Strings.no_tag)
            dropdownItems.addAll(app.tags.toMutableList())
            dropdownItems.add(Strings.all_tag)
            val adapter = object : ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner,
                dropdownItems
            ) {
                @SuppressLint("ViewHolder")
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val binding = ItemSpinnerBinding.inflate(layoutInflater)
                    binding.textView.text = getItem(position)
                    binding.textView.setTextColor(Colors.primaryText)
                    binding.imageView.imageTintList = ColorStateList.valueOf(Colors.primaryText)
                    return binding.root
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.tagSpinner.adapter = adapter
            tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    (view as? TextView)?.setTextColor(Colors.primaryText)
                    if (date == null)
                        calculateSum(app.items, dropdownItems[position])
                    else
                        calculateSumForDate(app.items, dropdownItems[position], date)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
            tagSpinner.setSelection(dropdownItems.size - 1)


        }
        observeLiveData()
        return binding.root
    }

    private fun observeLiveData() {
        app.themeLiveData.observe(viewLifecycleOwner) {
            binding.apply {
                root.setBackgroundColor(Colors.primaryBackground)
                titleText.setTextColor(Colors.primaryText)
                yearlyText.setTextColor(Colors.secondaryText)
                yearlyTotalText.setTextColor(Colors.primaryText)
                monthlyText.setTextColor(Colors.secondaryText)
                monthlyTotalText.setTextColor(Colors.primaryText)
                dailyText.setTextColor(Colors.secondaryText)
                dailyTotalText.setTextColor(Colors.primaryText)
            }
        }
        app.itemsLiveData.observe(viewLifecycleOwner) { expenses ->
            if (date == null)
                calculateSum(expenses, tagLiveData.value!!)
            else
                calculateSumForDate(expenses, tagLiveData.value!!, date)
        }
        tagLiveData.observe(viewLifecycleOwner) { tag ->
            if (date == null)
                calculateSum(app.items, tag)
            else
                calculateSumForDate(app.items, tag, date)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun calculateSumForDate(expenses: List<Expense>, tag: String, date: String) {
        var total = 0
        expenses
            .filter { DateUtils.formatTimestampToDateOnly(it.date.toLong()) == date }
            .filter { tag == Strings.all_tag || it.tag == tag }
            .forEach { total += it.cost }

        if (total < 0) {
            binding.dailyTotalText.setTextColor(Colors.green)
            binding.dailyTotalText.text = "+ ₩${-total}"
        } else {
            binding.dailyTotalText.setTextColor(Colors.red)
            binding.dailyTotalText.text = "- ₩$total"
        }
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
            .filter { tag == Strings.all_tag || it.tag == tag }
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

        if (yearTotal < 0) {
            binding.yearlyTotalText.setTextColor(resources.getColor(R.color.green, null))
            binding.yearlyTotalText.text = "+ ₩${-yearTotal}"
        } else {
            binding.yearlyTotalText.setTextColor(resources.getColor(R.color.red, null))
            binding.yearlyTotalText.text = "- ₩$yearTotal"
        }
        if (monthTotal < 0) {
            binding.monthlyTotalText.setTextColor(resources.getColor(R.color.green, null))
            binding.monthlyTotalText.text = "+ ₩${-monthTotal}"
        } else {
            binding.monthlyTotalText.setTextColor(resources.getColor(R.color.red, null))
            binding.monthlyTotalText.text = "- ₩$monthTotal"
        }
        if (dayTotal < 0) {
            binding.dailyTotalText.setTextColor(resources.getColor(R.color.green, null))
            binding.dailyTotalText.text = "+ ₩${-dayTotal}"
        } else {
            binding.dailyTotalText.setTextColor(resources.getColor(R.color.red, null))
            binding.dailyTotalText.text = "- ₩$dayTotal"
        }
    }

}
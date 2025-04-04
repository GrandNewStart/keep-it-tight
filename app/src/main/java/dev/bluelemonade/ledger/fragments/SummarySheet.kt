package dev.bluelemonade.ledger.fragments

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
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
import dev.bluelemonade.ledger.comm.ItemUtils
import dev.bluelemonade.ledger.comm.Strings
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.databinding.FragmentSummaryBinding
import dev.bluelemonade.ledger.databinding.ItemSpinnerBinding
import dev.bluelemonade.ledger.db.Expense
import java.util.Calendar
import java.util.Date

class SummarySheet(
    private val month: String? = null,
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
            dropdownItems.addAll(app.tags)
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
                    if (date != null) {
                        calculateSumForDate(dropdownItems[position], date)
                    }
                    if (month != null) {
                        calculateSumForMonth(dropdownItems[position], month)
                    }
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
        tagLiveData.observe(viewLifecycleOwner) { tag ->
            date?.let {
                calculateSumForDate(tag, it)
            }
            month?.let {
                calculateSumForMonth(tag, it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun calculateSumForDate(tag: String, date: String) {
        var total = 0
        app.items
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
    private fun calculateSumForMonth(tag: String, yearMonthStr: String) {
        val yearStr = yearMonthStr.split(".")[0]
        val monthStr = yearMonthStr.split(".")[1]
        val calendar = Calendar.getInstance()
        val today = Date()
        calendar.time = today
        calendar.set(Calendar.YEAR, yearStr.toInt())
        calendar.set(Calendar.MONTH, monthStr.toInt() - 1)
        val updatedDate = calendar.time

        val dateStr = DateUtils.formatTimestampToDateTime(updatedDate.time)
        val sums = ItemUtils.getSum(app.items, dateStr, tag)

        binding.yearlyText.text = yearStr + "년"
        if (sums[0] < 0) {
            binding.yearlyTotalText.setTextColor(Colors.green)
            binding.yearlyTotalText.text = "+ ₩${-sums[0]}"
        } else {
            binding.yearlyTotalText.setTextColor(Colors.red)
            binding.yearlyTotalText.text = "- ₩${sums[0]}"
        }
        binding.monthlyText.text = "${yearStr}년 ${monthStr.toInt()}월"
        if (sums[1] < 0) {
            binding.monthlyTotalText.setTextColor(Colors.green)
            binding.monthlyTotalText.text = "+ ₩${-sums[1]}"
        } else {
            binding.monthlyTotalText.setTextColor(Colors.red)
            binding.monthlyTotalText.text = "- ₩${sums[1]}"
        }
        binding.dailyText.text = DateUtils.formatTimestampToDateKorean(today.time)
        if (sums[2] < 0) {
            binding.dailyTotalText.setTextColor(Colors.green)
            binding.dailyTotalText.text = "+ ₩${-sums[2]}"
        } else {
            binding.dailyTotalText.setTextColor(Colors.red)
            binding.dailyTotalText.text = "- ₩${sums[2]}"
        }
    }

}
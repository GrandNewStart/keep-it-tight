package dev.bluelemonade.ledger.fragments

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.db.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OptionSheet(
    private val item: Expense,
    private val theme: MutableLiveData<Theme>,
    private val expenses: MutableLiveData<List<Expense>>,
    private val tags: MutableLiveData<List<String>>,
    private val repository: ExpenseRepository
) :
    BottomSheetDialogFragment() {

    private lateinit var dateTimeText: TextView
    private lateinit var dateTimeEditButton: MaterialCardView
    private lateinit var dateTimeEditButtonImage: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var costEditText: EditText
    private lateinit var tagSpinner: Spinner
    private lateinit var confirmButton: MaterialButton
    private lateinit var deleteButton: MaterialButton

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_option, container, false).apply {
            dateTimeText = findViewById(R.id.dateTimeText)
            dateTimeEditButton = findViewById(R.id.dateTimeEditButton)
            dateTimeEditButtonImage = findViewById(R.id.dateTimeEditButtonImage)
            nameEditText = findViewById(R.id.nameEditText)
            costEditText = findViewById(R.id.costEditText)
            tagSpinner = findViewById(R.id.tagSpinner)
            confirmButton = findViewById(R.id.confirmButton)
            deleteButton = findViewById(R.id.deleteButton)
            dateTimeText.text = DateUtils.formatTimestampToDateTime(item.date.toLong())
            nameEditText.setText(item.name)
            costEditText.setText(item.cost.toString())
            setButtons()
            setSpinner(item.tag)
            observe()
        }
    }

    private fun setSpinner(tag: String) {
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            tags.value!!
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
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        val position = tags.value!!.indexOf(tag)
        if (position == -1) {
            tagSpinner.setSelection(0)
        } else {
            tagSpinner.setSelection(position)
        }
    }

    private fun setButtons() {
        dateTimeEditButton.setOnClickListener {
            selectDateTime()
        }
        confirmButton.setOnClickListener {
            updateItem()
        }
        deleteButton.setOnClickListener {
            deleteItem()
        }
    }

    private fun observe() {
        theme.observe(viewLifecycleOwner) { theme ->
            val primaryBG = resources.getColor(
                if (theme == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground,
                null
            )
            val secondaryBG = resources.getColor(
                if (theme == Theme.Dark) R.color.darkSecondaryBackground else R.color.lightSecondaryBackground,
                null
            )
            val primaryTXT = resources.getColor(
                if (theme == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText,
                null
            )
            val secondaryTXT = resources.getColor(
                if (theme == Theme.Dark) R.color.darkSecondaryText else R.color.lightSecondaryText,
                null
            )
            view?.setBackgroundColor(primaryBG)
            dateTimeText.setTextColor(primaryTXT)
            dateTimeEditButton.setCardBackgroundColor(primaryTXT)
            dateTimeEditButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
            dateTimeEditButtonImage.imageTintList = ColorStateList.valueOf(primaryBG)
            nameEditText.backgroundTintList = ColorStateList.valueOf(primaryTXT)
            nameEditText.setTextColor(primaryTXT)
            nameEditText.setHintTextColor(secondaryTXT)
            costEditText.backgroundTintList = ColorStateList.valueOf(primaryTXT)
            costEditText.setTextColor(primaryTXT)
            costEditText.setHintTextColor(secondaryTXT)
            tagSpinner.backgroundTintList = ColorStateList.valueOf(primaryTXT)
            confirmButton.setBackgroundColor(primaryTXT)
            confirmButton.setTextColor(primaryBG)
            confirmButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
            deleteButton.setBackgroundColor(primaryTXT)
            deleteButton.setTextColor(primaryBG)
            deleteButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
        }
    }

    private fun selectDateTime() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            selectTime(calendar)
        }
        datePicker.show(parentFragmentManager, "MaterialDatePicker")
    }

    private fun selectTime(calendar: Calendar) {
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
                calendar.set(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DATE),
                    hourOfDay,
                    minute,
                    0
                )
                dateTimeText.text = format.format(calendar.time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun updateItem() {
        val dateTimeStr = dateTimeText.text.toString()
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
        val date = format.parse(dateTimeStr)!!
        val name = nameEditText.text.toString()
        val cost = costEditText.text.toString()
        val tag = tagSpinner.selectedItem.toString()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.enter_name, Toast.LENGTH_SHORT).show()
            return
        }
        if (cost.isEmpty()) {
            Toast.makeText(requireContext(), R.string.enter_cost, Toast.LENGTH_SHORT).show()
            return
        }
        val newItem = Expense(
            item.id,
            date.time.toString(),
            name,
            cost.toInt(),
            tag
        )
        CoroutineScope(Dispatchers.Default).launch {
            repository.update(newItem)
            val newList = expenses.value!!.toMutableList()
            newList[newList.indexOf(item)] = newItem
            expenses.postValue(newList)
            dismiss()
        }
    }

    private fun deleteItem() {
        CoroutineScope(Dispatchers.Default).launch {
            repository.delete(item)
            val newList = expenses.value!!.toMutableList()
            newList.remove(item)
            expenses.postValue(newList)
            dismiss()
        }
    }

}
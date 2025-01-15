package dev.bluelemonade.ledger.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.databinding.FragmentOptionBinding
import dev.bluelemonade.ledger.databinding.ItemSpinnerBinding
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.db.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.absoluteValue

class OptionSheet(
    private val item: Expense,
    private val theme: MutableLiveData<Theme>,
    private val tags: MutableLiveData<List<String>>,
    private val repository: ExpenseRepository,
    private val onDataChange: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentOptionBinding
    private val minusLiveData = MutableLiveData(true)

    override fun getTheme(): Int {
        return R.style.Theme_BottomSheetDialog_Fullscreen
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOptionBinding.inflate(inflater, container, false)
        binding.apply {
            dateTimeText.text = DateUtils.formatTimestampToDateTime(item.date.toLong())
            signButton.setOnClickListener {
                minusLiveData.postValue(!minusLiveData.value!!)
            }
            costEditText.setText(item.cost.absoluteValue.toString())
            costEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    nameEditText.requestFocus()
                    true
                }
                false
            }
            nameEditText.setText(item.name)
            nameEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    nameEditText.clearFocus()
                    true
                }
                false
            }
            setButtons()
            setSpinner(item.tag)
            observe()
            minusLiveData.postValue(item.cost > 0)
        }
        return binding.root
    }

    private fun setSpinner(tag: String) {
        val tags = tags.value!!.toMutableList()
        tags.add(0, getString(R.string.no_tag))
        binding.tagSpinner.let { spinner ->
            val adapter = object : ArrayAdapter<String>(
                requireContext(),
                R.layout.item_spinner,
                tags
            ) {
                @SuppressLint("ViewHolder")
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val binding = ItemSpinnerBinding.inflate(layoutInflater)
                    binding.textView.text = getItem(position)
                    binding.textView.setTextColor(theme.value!!.primaryTXT(requireContext()))
                    binding.imageView.imageTintList =
                        ColorStateList.valueOf(theme.value!!.primaryTXT(requireContext()))
                    return binding.root
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            val position = tags.indexOf(tag)
            if (position == -1) {
                spinner.setSelection(0)
            } else {
                spinner.setSelection(position)
            }
        }
    }

    private fun setButtons() {
        binding.apply {
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
    }

    private fun observe() {
        theme.observe(viewLifecycleOwner) { theme ->
            val primaryBG = theme.primaryBG(requireContext())
            val primaryTXT = theme.primaryTXT(requireContext())
            val secondaryTXT = theme.secondaryTXT(requireContext())

            binding.apply {
                root.setBackgroundColor(primaryBG)
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
        minusLiveData.observe(viewLifecycleOwner) { minus ->
            binding.signButton.setBackgroundColor(
                resources.getColor(
                    if (minus) R.color.red else R.color.green,
                    null
                )
            )
            binding.signButton.setRippleColorResource(if (minus) R.color.red else R.color.green)
            binding.signButton.text = getString(if (minus) R.string.minus else R.string.plus)
            binding.costEditText.hint =
                getString(if (minus) R.string.cost_input_hint_minus else R.string.cost_input_hint_plus)
            binding.nameEditText.hint =
                getString(if (minus) R.string.expense_name_minus else R.string.expense_name_plus)
        }
    }

    private fun selectDateTime() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.date_selection))
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
                calendar.set(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DATE),
                    hourOfDay,
                    minute,
                    0
                )
                binding.dateTimeText.text = DateUtils.formatTimestampToDateTime(calendar.time.time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun updateItem() {
        val dateTimeStr = binding.dateTimeText.text.toString()
        val date = DateUtils.formatDateTimeToTimestamp(dateTimeStr)
        val name = binding.nameEditText.text.toString()
        var cost = binding.costEditText.text.toString()
        val tag = binding.tagSpinner.selectedItem.toString()

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.enter_name, Toast.LENGTH_SHORT).show()
            return
        }
        if (cost.isEmpty()) {
            Toast.makeText(requireContext(), R.string.enter_cost, Toast.LENGTH_SHORT).show()
            return
        }
        if (!minusLiveData.value!!) {
            cost = "-$cost"
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
            onDataChange()
            dismiss()
        }
    }

    private fun deleteItem() {
        AlertDialog.Builder(context)
            .setTitle(R.string.ask_delete_item)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                CoroutineScope(Dispatchers.Default).launch {
                    repository.delete(item)
                    onDataChange()
                    dismiss()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
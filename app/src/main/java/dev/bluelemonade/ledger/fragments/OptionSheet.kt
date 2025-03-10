package dev.bluelemonade.ledger.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import dev.bluelemonade.ledger.GlobalApplication
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Colors
import dev.bluelemonade.ledger.comm.Strings
import dev.bluelemonade.ledger.databinding.FragmentOptionBinding
import dev.bluelemonade.ledger.databinding.ItemSpinnerBinding
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.extensions.hideKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.absoluteValue

class OptionSheet(
    private val item: Expense
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentOptionBinding
    private val minusLiveData = MutableLiveData(true)
    private val app = GlobalApplication.instance

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
                    return@setOnEditorActionListener true
                }
                false
            }
            nameEditText.setText(item.name)
            nameEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    nameEditText.clearFocus()
                    requireActivity().hideKeyboard(nameEditText)
                    return@setOnEditorActionListener true
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
        val tags = app.tags.toMutableList()
        tags.add(0, Strings.no_tag)
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
                    binding.textView.setTextColor(Colors.primaryText)
                    binding.imageView.imageTintList =
                        ColorStateList.valueOf(Colors.primaryText)
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
        app.themeLiveData.observe(viewLifecycleOwner) {
            binding.apply {
                root.setBackgroundColor(Colors.primaryBackground)
                dateTimeText.setTextColor(Colors.primaryText)
                dateTimeEditButton.setCardBackgroundColor(Colors.primary)
                dateTimeEditButton.rippleColor = ColorStateList.valueOf(Colors.secondary)
                nameEditText.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
                nameEditText.setTextColor(Colors.primaryText)
                nameEditText.setHintTextColor(Colors.secondaryText)
                costEditText.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
                costEditText.setTextColor(Colors.primaryText)
                costEditText.setHintTextColor(Colors.secondaryText)
                tagSpinner.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
                confirmButton.setBackgroundColor(Colors.primary)
                confirmButton.rippleColor = ColorStateList.valueOf(Colors.secondary)
                deleteButton.setBackgroundColor(Colors.primary)
                deleteButton.rippleColor = ColorStateList.valueOf(Colors.secondary)
            }
        }
        minusLiveData.observe(viewLifecycleOwner) { minus ->
            binding.signButton.setBackgroundColor(if (minus) Colors.red else Colors.green)
            binding.signButton.setRippleColorResource(if (minus) R.color.red else R.color.green)
            binding.signButton.text = if (minus) Strings.minus else Strings.plus
            binding.costEditText.hint =
                if (minus) Strings.cost_input_hint_minus else Strings.cost_input_hint_plus
            binding.nameEditText.hint =
                if (minus) Strings.expense_name_minus else Strings.expense_name_plus
        }
    }

    private fun selectDateTime() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(Strings.date_selection)
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
        CoroutineScope(Dispatchers.Default).launch {
            app.updateItem(item.id, name, cost.toInt(), tag, date)
            dismiss()
        }
    }

    private fun deleteItem() {
        AlertDialog.Builder(context)
            .setTitle(R.string.ask_delete_item)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                CoroutineScope(Dispatchers.Default).launch {
                    app.deleteItem(item)
                    dismiss()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
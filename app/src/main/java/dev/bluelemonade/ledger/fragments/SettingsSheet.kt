package dev.bluelemonade.ledger.fragments

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Storage
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.databinding.FragmentSettingsBinding
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.db.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsSheet(
    private val theme: MutableLiveData<Theme>,
    private val expenses: MutableLiveData<List<Expense>>,
    private val tags: MutableLiveData<List<String>>,
    private val repository: ExpenseRepository
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var storage: Storage

    override fun getTheme(): Int {
        return R.style.Theme_BottomSheetDialog_Fullscreen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        storage = Storage(requireContext())
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        binding.apply {
            // Tag management button setup
            manageTagButton.setOnClickListener {
                dismiss()
                TagManagementSheet(tags, theme).show(parentFragmentManager, "ManageTagSheet")
            }

            // Reset button setup
            resetButton.setOnClickListener {
                reset()
            }

            // Theme switch setup
            themeSwitch.trackTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    resources.getColor(R.color.darkSecondaryBackground, null),
                    resources.getColor(R.color.lightSecondaryBackground, null)
                )
            )
            themeSwitch.thumbTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    resources.getColor(R.color.darkPrimaryText, null),
                    resources.getColor(R.color.lightPrimaryText, null)
                )
            )
            themeSwitch.setOnCheckedChangeListener { _, isChecked ->
                theme.postValue(if (isChecked) Theme.Dark else Theme.Light)
                storage.setTheme(theme.value!!)
            }
            themeSwitch.isChecked = storage.getTheme() == Theme.Dark
            themeSwitchText.setText(if (theme.value == Theme.Dark) R.string.dark_mode else R.string.light_mode)
        }
        observeLiveData()
        return binding.root
    }


    private fun observeLiveData() {
        theme.observe(viewLifecycleOwner) { theme ->
            val primaryTXT = theme.primaryTXT(requireContext())
            val secondaryTXT = theme.secondaryTXT(requireContext())
            val primaryBG = theme.primaryBG(requireContext())
            val primary = theme.primary(requireContext())
            val secondary = theme.secondary(requireContext())
            binding.apply {
                root.setBackgroundColor(primaryBG)
                titleText.setTextColor(primaryTXT)
                themeSwitchText.setTextColor(secondaryTXT)
                themeSwitchText.text = resources.getText(if (theme == Theme.Dark) R.string.dark_mode else R.string.light_mode)
                resetButton.setBackgroundColor(primary)
                resetButton.rippleColor = ColorStateList.valueOf(secondary)
                manageTagButton.setBackgroundColor(primary)
                manageTagButton.rippleColor = ColorStateList.valueOf(secondary)
            }
        }
    }

    private fun reset() {
        AlertDialog.Builder(context)
            .setTitle(R.string.ask_reset)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                CoroutineScope(Dispatchers.Default).launch {
                    repository.deleteAll()
                    expenses.postValue(listOf())
                    storage.clear()
                    tags.postValue(listOf())
                    dismiss()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
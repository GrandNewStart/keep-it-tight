package dev.jinwoo.reflection.fragments

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import dev.jinwoo.reflection.R
import dev.jinwoo.reflection.Storage
import dev.jinwoo.reflection.Theme
import dev.jinwoo.reflection.db.Expense
import dev.jinwoo.reflection.db.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsSheet(
    private val theme: MutableLiveData<Theme>,
    private val expenses: MutableLiveData<List<Expense>>,
    private val tags: MutableLiveData<List<String>>,
    private val repository: ExpenseRepository
) : BottomSheetDialogFragment() {

    private lateinit var titleText: TextView
    private lateinit var themeSwitch: MaterialSwitch;
    private lateinit var themeSwitchText: TextView
    private lateinit var manageTagButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var storage: Storage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        storage = Storage(requireContext())
        return inflater.inflate(R.layout.fragment_settings, container, false).apply {
            titleText = findViewById(R.id.titleText)
            manageTagButton = findViewById(R.id.manageTagButton)
            resetButton = findViewById(R.id.resetButton)
            themeSwitch = findViewById(R.id.themeSwitch)
            themeSwitchText = findViewById(R.id.themeSwitchText)
            manageTagButton.setOnClickListener {
                dismiss()
                TagManagementSheet(tags, theme).show(parentFragmentManager, "ManageTagSheet")
            }
            resetButton.setOnClickListener {
                CoroutineScope(Dispatchers.Default).launch {
                    repository.deleteAll()
                    expenses.postValue(listOf())
                    storage.clear()
                    tags.postValue(listOf())
                    dismiss()
                }
            }
            initThemeSwitch()
        }
    }


    private fun initThemeSwitch() {
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
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            theme.postValue(if (isChecked) Theme.Dark else Theme.Light)
            storage.setTheme(theme.value!!)
        }
        themeSwitchText.setText(if (theme.value == Theme.Dark) R.string.dark_mode else R.string.light_mode)
        themeSwitch.isChecked = storage.getTheme() == Theme.Dark
        theme.observe(viewLifecycleOwner) { theme ->
            val primaryTXT = resources.getColor(
                if (theme == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText,
                null
            )
            val secondaryTXT = resources.getColor(
                if (theme == Theme.Dark) R.color.darkSecondaryBackground else R.color.lightSecondaryBackground,
                null
            )
            val primaryBG = resources.getColor(
                if (theme == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground,
                null
            )
            view?.setBackgroundColor(primaryBG)
            titleText.setTextColor(primaryTXT)
            themeSwitchText.setTextColor(primaryTXT)
            resetButton.setBackgroundColor(primaryTXT)
            resetButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
            resetButton.setTextColor(primaryBG)
            manageTagButton.setBackgroundColor(primaryTXT)
            manageTagButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
            manageTagButton.setTextColor(primaryBG)
        }
    }


}
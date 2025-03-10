package dev.bluelemonade.ledger.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.bluelemonade.ledger.GlobalApplication
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Colors
import dev.bluelemonade.ledger.comm.Storage
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.databinding.FragmentSettingsBinding
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.db.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSettingsBinding
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
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        binding.apply {
            val packageInfo = requireContext().packageManager.getPackageInfo("dev.bluelemonade.ledger", 0)
            val versionName = packageInfo.versionName
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            versionText.text = "v$versionName(${versionCode})"

            // Tag management button setup
            manageTagButton.setOnClickListener {
                dismiss()
                TagManagementSheet().show(parentFragmentManager, "ManageTagSheet")
            }

            // Reset button setup
            resetButton.setOnClickListener {
                reset()
            }

            // Privacy Policy button
            privacyButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://bluelemonade.co.kr/keep-it-tight/privacy-policy.html"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
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
                app.setTheme(if (isChecked) Theme.Dark else Theme.Light)
            }
            themeSwitch.isChecked = app.theme == Theme.Dark
            themeSwitchText.setText(if (app.theme == Theme.Dark) R.string.dark_mode else R.string.light_mode)
        }
        observeLiveData()
        return binding.root
    }


    private fun observeLiveData() {
        app.themeLiveData.observe(viewLifecycleOwner) {
            binding.apply {
                root.setBackgroundColor(Colors.primaryBackground)
                titleText.setTextColor(Colors.primaryText)
                versionText.setTextColor(Colors.secondaryText)
                themeSwitchText.setTextColor(Colors.secondaryText)
                themeSwitchText.text =
                    resources.getText(if (it == Theme.Dark) R.string.dark_mode else R.string.light_mode)
                resetButton.setBackgroundColor(Colors.primary)
                resetButton.rippleColor = ColorStateList.valueOf(Colors.secondary)
                manageTagButton.setBackgroundColor(Colors.primary)
                manageTagButton.rippleColor = ColorStateList.valueOf(Colors.secondary)
                privacyButton.setBackgroundColor(Colors.primary)
                privacyButton.rippleColor = ColorStateList.valueOf(Colors.secondary)
            }
        }
    }

    private fun reset() {
        AlertDialog.Builder(context)
            .setTitle(R.string.ask_reset)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                CoroutineScope(Dispatchers.Default).launch {
                    app.reset()
                    dismiss()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
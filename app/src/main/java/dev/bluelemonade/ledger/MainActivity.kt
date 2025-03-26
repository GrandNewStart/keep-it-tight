package dev.bluelemonade.ledger

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.bluelemonade.ledger.comm.Colors
import dev.bluelemonade.ledger.comm.ItemUtils
import dev.bluelemonade.ledger.comm.Strings
import dev.bluelemonade.ledger.databinding.ActivityMainBinding
import dev.bluelemonade.ledger.databinding.ItemSpinnerBinding
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.expense.ExpenseAdapter
import dev.bluelemonade.ledger.extensions.handleKeyboardPopup
import dev.bluelemonade.ledger.extensions.hideKeyboard
import dev.bluelemonade.ledger.extensions.toast
import dev.bluelemonade.ledger.fragments.OptionSheet
import dev.bluelemonade.ledger.fragments.SettingsSheet
import dev.bluelemonade.ledger.fragments.SummarySheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val app = GlobalApplication.instance
    private lateinit var binding: ActivityMainBinding
    val exportFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let { uri -> exportFile(uri) }
    }
    val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFile(it) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeLiveData()
        handleKeyboardPopup()
    }

    private fun initView() {
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.apply {
            setContentView(root)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Settings button setup
            settingsButton.setOnClickListener {
                SettingsSheet().show(supportFragmentManager, "SettingsSheet")
            }

            // Summary button setup
            summaryButton.setOnClickListener {
                val month = monthSpinner.selectedItem as String
                SummarySheet(month = month).show(supportFragmentManager, "SummarySheet")
            }

            // Toggle +/- sign
            signButton.setOnClickListener {
                val text = signButton.text.toString()
                if (text == Strings.plus) {
                    signButton.text = Strings.minus
                    signButton.setBackgroundColor(Colors.red)
                    signButton.setRippleColorResource(R.color.red)
                    costEditText.hint = Strings.cost_input_hint_minus
                    nameEditText.hint = Strings.expense_name_minus
                    return@setOnClickListener
                }
                if (text == Strings.minus) {
                    signButton.text = Strings.plus
                    signButton.setBackgroundColor(Colors.green)
                    signButton.setRippleColorResource(R.color.green)
                    costEditText.hint = Strings.cost_input_hint_plus
                    nameEditText.hint = Strings.expense_name_plus
                    return@setOnClickListener
                }
            }

            nameEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    costEditText.requestFocus()
                    return@setOnEditorActionListener true
                }
                false
            }

            costEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    costEditText.clearFocus()
                    hideKeyboard(costEditText)
                    return@setOnEditorActionListener true
                }
                false
            }

            // Tag spinner setup
            tagSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val textColor = Colors.primaryText
                    (view as? TextView)?.setTextColor(textColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Month spinner setup
            monthSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val dateStr = monthSpinner.selectedItem as String
                    val filteredItems = ItemUtils.filterByMonth(app.items.toMutableList(), dateStr)
                    setRecyclerView(filteredItems)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Enter button setup
            enterButton.setOnClickListener {
                val costStr = costEditText.text.toString()
                if (costStr.isEmpty()) return@setOnClickListener
                var cost: Int
                try {
                    cost = costStr.toInt()
                } catch (e: NumberFormatException) {
                    toast(Strings.invalid_input)
                    return@setOnClickListener
                }
                if (signButton.text.toString() == Strings.plus) {
                    cost = -cost
                }

                val name = nameEditText.text.toString()
                if (name.isEmpty()) return@setOnClickListener

                val tag = tagSpinner.selectedItem.toString()
                if (tag.isEmpty()) return@setOnClickListener

                hideKeyboard(binding.root)

                app.addItem(name, cost, tag)

                runOnUiThread {
                    costEditText.setText("")
                    nameEditText.setText("")
                    tagSpinner.setSelection(0)
                }
            }
        }
    }

    private fun setRecyclerView(items: List<Expense>) {
        runOnUiThread {
            binding.recyclerView.apply {
                setBackgroundColor(Colors.transparent)
                adapter = ExpenseAdapter(items,
                    object : ExpenseAdapter.ExpenseAdapterListener {
                        override fun onClick(item: Expense) {
                            OptionSheet(item).show(
                                supportFragmentManager,
                                "OptionSheet"
                            )
                        }

                        override fun onClick(date: String) {
                            SummarySheet(date = date).show(
                                supportFragmentManager, "SummarySheet"
                            )
                        }
                    })
            }
        }
    }

    private fun setMonthSpinner(months: List<String>) {
        binding.monthSpinner.let {
            val adapter = object : ArrayAdapter<String>(
                this, R.layout.item_spinner, months
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
            runOnUiThread { it.adapter = adapter }
        }
    }

    private fun setTagSpinner(items: List<String>) {
        binding.tagSpinner.let {
            val adapter = object : ArrayAdapter<String>(
                this, R.layout.item_spinner, items
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
            runOnUiThread { it.adapter = adapter }
        }
    }

    private fun observeLiveData() {
        app.themeLiveData.observe(this) {
            window.statusBarColor = Colors.primaryBackground

            val colorAnimator =
                ValueAnimator.ofArgb(Colors.secondaryBackground, Colors.primaryBackground)
            colorAnimator.duration = 250
            colorAnimator.addUpdateListener { animator ->
                binding.root.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimator.start()

            binding.apply {
                titleText.setTextColor(Colors.primaryText)

                monthSpinner.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
                monthSpinner.foregroundTintList = ColorStateList.valueOf(Colors.primaryText)
                monthSpinner.findViewById<TextView>(R.id.textView)?.setTextColor(Colors.primaryText)
                monthSpinner.findViewById<ImageView>(R.id.imageView)?.imageTintList =
                    ColorStateList.valueOf(Colors.primaryText)
                monthSpinner.popupBackground.setTint(Color.WHITE)
                for (i in 0 until monthSpinner.count) {
                    monthSpinner.adapter.getDropDownView(i, null, root)?.let {
                        (it as? CheckedTextView)?.setTextColor(Color.BLACK)
                    }
                }

                settingsButton.backgroundTintList = ColorStateList.valueOf(Colors.primary)
                settingsButton.rippleColor = ColorStateList.valueOf(Colors.secondaryText)
                settingsImage.imageTintList = ColorStateList.valueOf(Colors.white)

                headerView.setBackgroundColor(Colors.primary)

                costEditText.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
                costEditText.setHintTextColor(Colors.secondaryText)
                costEditText.setTextColor(Colors.primaryText)

                nameEditText.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
                nameEditText.setHintTextColor(Colors.secondaryText)
                nameEditText.setTextColor(Colors.primaryText)

                tagSpinner.backgroundTintList = ColorStateList.valueOf(Colors.primaryText)
                tagSpinner.foregroundTintList = ColorStateList.valueOf(Colors.primaryText)
                tagSpinner.findViewById<TextView>(R.id.textView)?.setTextColor(Colors.primaryText)
                tagSpinner.findViewById<ImageView>(R.id.imageView)?.imageTintList =
                    ColorStateList.valueOf(Colors.primaryText)
                tagSpinner.popupBackground.setTint(Color.WHITE)
                for (i in 0 until tagSpinner.count) {
                    tagSpinner.adapter.getDropDownView(i, null, root)?.let { it ->
                        (it as? CheckedTextView)?.setTextColor(Color.BLACK)
                    }
                }

                enterButton.setBackgroundColor(Colors.primary)
                enterButton.rippleColor = ColorStateList.valueOf(Colors.secondaryText)

                summaryButton.setBackgroundColor(Colors.primary)
                summaryButton.rippleColor = ColorStateList.valueOf(Colors.secondaryText)

                recyclerView.setThumbColor(Colors.secondaryText)
                recyclerView.setThumbInactiveColor(Colors.secondaryText)
                recyclerView.setTrackColor(Colors.secondaryBackground)
                recyclerView.setPopupTextColor(Colors.primaryBackground)
            }

            setRecyclerView(app.itemsLiveData.value!!)
        }
        app.tagsLiveData.observe(this) { tags ->
            val dropdownItems = ArrayList<String>()
            dropdownItems.add(Strings.no_tag)
            dropdownItems.addAll(tags)
            setTagSpinner(dropdownItems)
        }
        app.itemsLiveData.observe(this) { items ->
            setMonthSpinner(ItemUtils.getAllMonths(items))
            val dateStr = binding.monthSpinner.selectedItem as String
            val filteredItems = ItemUtils.filterByMonth(app.items.toMutableList(), dateStr)
            setRecyclerView(filteredItems)
        }
    }

    private fun exportFile(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val jsonArray = app.items.map {
                JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("date", it.date)
                    put("cost", it.cost)
                    put("tag", it.tag)
                }
            }
            val output = contentResolver.openOutputStream(uri)
            output?.bufferedWriter()?.use {
                it.write(
                    jsonArray.joinToString(
                        prefix = "[",
                        postfix = "]",
                        separator = ","
                    ) { obj -> obj.toString() })
            }

//            CoroutineScope(Dispatchers.Main).launch {
//                AlertDialog.Builder(applicationContext)
//                    .setMessage("Data exported successfully.")
//                    .setPositiveButton("OK", null)
//                    .show()
//            }
        }
    }

    private fun importFile(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val input = contentResolver.openInputStream(uri)
            val jsonString = input?.bufferedReader()?.use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val importedItems = mutableListOf<Expense>()

            try {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue

                    val id: String? = obj.optString("id", null)
                    val name: String? = obj.optString("name", null)
                    val date: String? = obj.optString("date", null)
                    val cost = obj.optInt("cost", Int.MIN_VALUE)
                    val tag: String? = obj.optString("tag", null)

                    if (id == null || name == null || date == null || tag == null || cost == Int.MIN_VALUE) {
                        continue
                    }

                    importedItems.add(Expense(id, name, date, cost, tag))
                }
            } catch (e: Exception) {
                Log.e("Import", "Failed to import JSON", e)
                CoroutineScope(Dispatchers.Main).launch {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Failed to import: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
                return@launch
            }

            // Filter out duplicates based on ID
            val existingIds = app.items.map { it.id }.toSet()
            val newItems = importedItems.filter { it.id !in existingIds }

            if (newItems.isEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("No new items to import.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                return@launch
            }

            // Ask user for confirmation to merge
            CoroutineScope(Dispatchers.Main).launch {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Confirm Import")
                    .setMessage("Import ${newItems.size} new item(s) into your data?")
                    .setPositiveButton("Import") { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            app.addItems(newItems)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            CoroutineScope(Dispatchers.Main).launch {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("Import complete: ${importedItems.size} items.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

}
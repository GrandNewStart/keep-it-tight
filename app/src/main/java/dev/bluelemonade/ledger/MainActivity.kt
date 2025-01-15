package dev.bluelemonade.ledger

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import dev.bluelemonade.ledger.comm.DateUtils
import dev.bluelemonade.ledger.comm.Storage
import dev.bluelemonade.ledger.comm.Theme
import dev.bluelemonade.ledger.databinding.ActivityMainBinding
import dev.bluelemonade.ledger.databinding.ItemSpinnerBinding
import dev.bluelemonade.ledger.db.Expense
import dev.bluelemonade.ledger.db.ExpenseDatabase
import dev.bluelemonade.ledger.db.ExpenseRepository
import dev.bluelemonade.ledger.expense.ExpenseAdapter
import dev.bluelemonade.ledger.fragments.OptionSheet
import dev.bluelemonade.ledger.fragments.SettingsSheet
import dev.bluelemonade.ledger.fragments.SummarySheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: ExpenseDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var storage: Storage

    private val themeLiveData = MutableLiveData(Theme.Dark)
    private val expensesLiveData = MutableLiveData<List<Expense>>(listOf())
    private val tagsLiveData = MutableLiveData<List<String>>(listOf())
    private val minusLiveData = MutableLiveData(true)
    private var allExpenses: List<Expense> = listOf()
    private var selectedMonth = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeLiveData()
        handleKeyboardPopup()
        initData()
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

            // Month spinner setup
            monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    (monthSpinner.selectedItem as? String)?.let { dateStr ->
                        val year = dateStr.substring(0, 4).toInt()
                        val month = dateStr.substring(5, 7).toInt()
                        val filteredItems = allExpenses.filter { item ->
                            val itemDate = Date(item.date.toLong())
                            val format = SimpleDateFormat("yyyy.MM", Locale.getDefault())
                            val itemDateStr = format.format(itemDate)
                            val itemYear = itemDateStr.substring(0, 4).toInt()
                            val itemMonth = itemDateStr.substring(5, 7).toInt()
                            return@filter itemYear == year && itemMonth == month
                        }
                        expensesLiveData.postValue(filteredItems)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}

            }

            // Settings button setup
            settingsButton.setOnClickListener {
                val settingsSheet =
                    SettingsSheet(themeLiveData, expensesLiveData, tagsLiveData, repository)
                settingsSheet.show(supportFragmentManager, "SettingsSheet")
            }

            // Summary button setup
            summaryButton.setOnClickListener {
                val summarySheet = SummarySheet(expensesLiveData, tagsLiveData, themeLiveData)
                summarySheet.show(supportFragmentManager, "SummarySheet")
            }

            // Toggle +/- sign
            signButton.setOnClickListener {
                minusLiveData.postValue(!minusLiveData.value!!)
            }

            nameEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    costEditText.requestFocus()
                    true
                }
                false
            }

            costEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    costEditText.clearFocus()
                    true
                }
                false
            }

            // Tag spinner setup
            tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val textColor = themeLiveData.value!!.primaryTXT(applicationContext)
                    (view as? TextView)?.setTextColor(textColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Enter button setup
            enterButton.setOnClickListener {
                val minus = minusLiveData.value!!
                val costStr = costEditText.text.toString()
                if (costStr.isEmpty()) return@setOnClickListener
                var cost: Int
                try {
                    cost = costStr.toInt()
                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        applicationContext, getString(R.string.invalid_input), Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (!minus) {
                    cost = -cost
                }

                val name = nameEditText.text.toString()
                if (name.isEmpty()) return@setOnClickListener

                val tag = tagSpinner.selectedItem.toString()
                if (tag.isEmpty()) return@setOnClickListener

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

                CoroutineScope(Dispatchers.Default).launch {
                    repository.insert(
                        Expense(
                            date = DateUtils.createTimestamp().toString(),
                            cost = cost,
                            name = name,
                            tag = tag
                        )
                    )
                    loadData()
                    runOnUiThread {
                        costEditText.setText("")
                        nameEditText.setText("")
                        tagSpinner.setSelection(0)
                    }
                }
            }
        }
    }

    private fun setRecyclerView(items: List<Expense>) {
        runOnUiThread {
            binding.recyclerView.apply {
                setBackgroundColor(getColor(R.color.transparent))
                adapter = ExpenseAdapter(items,
                    themeLiveData.value!!,
                    object : ExpenseAdapter.ExpenseAdapterListener {
                        override fun onClick(item: Expense) {
                            OptionSheet(item,
                                themeLiveData,
                                tagsLiveData,
                                repository,
                                { loadData() }).show(
                                supportFragmentManager,
                                "OptionSheet"
                            )
                        }

                        override fun onClick(date: String) {
                            SummarySheet(expensesLiveData, tagsLiveData, themeLiveData, date).show(
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
                    val primaryTXT = themeLiveData.value!!.primaryTXT(applicationContext)
                    binding.textView.text = getItem(position)
                    binding.textView.setTextColor(primaryTXT)
                    binding.imageView.imageTintList = ColorStateList.valueOf(primaryTXT)
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
                    val primaryTXT = themeLiveData.value!!.primaryTXT(applicationContext)
                    binding.textView.text = getItem(position)
                    binding.textView.setTextColor(primaryTXT)
                    binding.imageView.imageTintList = ColorStateList.valueOf(primaryTXT)
                    return binding.root
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            runOnUiThread { it.adapter = adapter }
        }
    }

    private fun observeLiveData() {
        themeLiveData.observe(this, { theme ->
            storage.setTheme(theme)
            val primaryBG = theme.primaryBG(applicationContext)
            val secondaryBG = theme.secondaryBG(applicationContext)
            val primaryTXT = theme.primaryTXT(applicationContext)
            val secondaryTXT = theme.secondaryTXT(applicationContext)

            window.statusBarColor = secondaryBG
            val colorAnimator = ValueAnimator.ofArgb(
                getColor(if (theme == Theme.Dark) R.color.lightPrimaryBackground else R.color.darkPrimaryBackground),
                getColor(if (theme == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground)
            )
            colorAnimator.duration = 250
            colorAnimator.addUpdateListener { animator ->
                binding.root.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimator.start()

            binding.apply {
                titleText.setTextColor(primaryTXT)

                monthSpinner.backgroundTintList = ColorStateList.valueOf(primaryTXT)
                monthSpinner.foregroundTintList = ColorStateList.valueOf(primaryTXT)
                monthSpinner.findViewById<TextView>(R.id.textView)?.setTextColor(primaryTXT)
                monthSpinner.findViewById<ImageView>(R.id.imageView)?.imageTintList =
                    ColorStateList.valueOf(primaryTXT)
                monthSpinner.popupBackground.setTint(Color.WHITE)
                for (i in 0 until monthSpinner.count) {
                    monthSpinner.adapter.getDropDownView(i, null, root)?.let {
                        (it as? CheckedTextView)?.setTextColor(Color.BLACK)
                    }
                }

                settingsButton.backgroundTintList = ColorStateList.valueOf(primaryTXT)
                settingsButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
                settingsImage.imageTintList = ColorStateList.valueOf(primaryBG)

                headerNameText.setTextColor(primaryTXT)
                headerCostText.setTextColor(primaryTXT)
                headerTagText.setTextColor(primaryTXT)

                costEditText.backgroundTintList = ColorStateList.valueOf(primaryTXT)
                costEditText.setHintTextColor(secondaryTXT)
                costEditText.setTextColor(primaryTXT)

                nameEditText.backgroundTintList = ColorStateList.valueOf(primaryTXT)
                nameEditText.setHintTextColor(secondaryTXT)
                nameEditText.setTextColor(primaryTXT)

                tagSpinner.backgroundTintList = ColorStateList.valueOf(primaryTXT)
                tagSpinner.foregroundTintList = ColorStateList.valueOf(primaryTXT)
                tagSpinner.findViewById<TextView>(R.id.textView)?.setTextColor(primaryTXT)
                tagSpinner.findViewById<ImageView>(R.id.imageView)?.imageTintList =
                    ColorStateList.valueOf(primaryTXT)
                tagSpinner.popupBackground.setTint(Color.WHITE)
                for (i in 0 until tagSpinner.count) {
                    tagSpinner.adapter.getDropDownView(i, null, root)?.let { it ->
                        (it as? CheckedTextView)?.setTextColor(Color.BLACK)
                    }
                }

                enterButton.setTextColor(primaryBG)
                enterButton.setBackgroundColor(primaryTXT)
                enterButton.rippleColor = ColorStateList.valueOf(secondaryTXT)

                summaryButton.setTextColor(primaryBG)
                summaryButton.setBackgroundColor(primaryTXT)
                summaryButton.rippleColor = ColorStateList.valueOf(secondaryTXT)

                recyclerView.setThumbColor(secondaryTXT)
                recyclerView.setThumbInactiveColor(secondaryTXT)
                recyclerView.setTrackColor(secondaryBG)
                recyclerView.setPopupTextColor(primaryBG)
            }

            setRecyclerView(expensesLiveData.value!!)
        })
        minusLiveData.observe(this) { minus ->
            binding.signButton.setBackgroundColor(getColor(if (minus) R.color.red else R.color.green))
            binding.signButton.setRippleColorResource(if (minus) R.color.red else R.color.green)
            binding.signButton.text = getString(if (minus) R.string.minus else R.string.plus)
            binding.costEditText.hint =
                getString(if (minus) R.string.cost_input_hint_minus else R.string.cost_input_hint_plus)
            binding.nameEditText.hint =
                getString(if (minus) R.string.expense_name_minus else R.string.expense_name_plus)
        }
        tagsLiveData.observe(this) { tags ->
            val dropdownItems = ArrayList<String>()
            dropdownItems.add(getString(R.string.no_tag))
            dropdownItems.addAll(tags)
            setTagSpinner(dropdownItems)
        }
        expensesLiveData.observe(this) { expenses ->
            setRecyclerView(expenses)
        }
    }

    private fun initData() {
        storage = Storage(this)
        tagsLiveData.postValue(storage.getTags())
        themeLiveData.postValue(storage.getTheme())
        database = ExpenseDatabase.getDatabase(this)
        repository = ExpenseRepository(database.expenseDao())
        loadData()
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.Default).launch {
            allExpenses = repository.getAllExpenses().sortedByDescending { it.date.toLong() }
            val format = SimpleDateFormat("yyyy.MM", Locale.getDefault())
            val months = ArrayList<String>()
            allExpenses.forEach { item ->
                val date = Date(item.date.toLong())
                val dateStr = format.format(date)
                if (!months.contains(dateStr)) {
                    months.add(dateStr)
                }
            }
            if (months.isEmpty()) {
                months.add(format.format(Date()))
            }
            if (selectedMonth.isEmpty()) {
                selectedMonth = months.first()
            }
            setMonthSpinner(months)
            val expenses = allExpenses.filter { item ->
                val year = selectedMonth.substring(0, 4).toInt()
                val month = selectedMonth.substring(5, 7).toInt()
                val itemDate = Date(item.date.toLong())
                val itemDateStr = format.format(itemDate)
                val itemYear = itemDateStr.substring(0, 4).toInt()
                val itemMonth = itemDateStr.substring(5, 7).toInt()
                return@filter itemYear == year && itemMonth == month
            }
            expensesLiveData.postValue(expenses)
        }
    }

    private fun handleKeyboardPopup() {
        val rootView = findViewById<View>(android.R.id.content)
        findViewById<View>(android.R.id.content).viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            if (keyboardHeight > screenHeight * 0.15) {
                rootView.translationY = -keyboardHeight.toFloat()
            } else {
                rootView.translationY = 0f
            }
        }
    }
}
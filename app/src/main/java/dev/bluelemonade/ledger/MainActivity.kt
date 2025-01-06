package dev.bluelemonade.ledger

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import dev.bluelemonade.ledger.fragments.OptionSheet
import dev.bluelemonade.ledger.fragments.SettingsSheet
import dev.bluelemonade.ledger.fragments.SummarySheet
import dev.bluelemonade.ledger.expense.ExpenseAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: ExpenseDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var storage: Storage

    private val themeLiveData = MutableLiveData(Theme.Dark)
    private val expensesLiveData = MutableLiveData<List<Expense>>(listOf())
    private val tagsLiveData = MutableLiveData<List<String>>(listOf())

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

            // Tag spinner setup
            tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val textColor = themeLiveData.value!!.primaryTXT(applicationContext)
                    (view as? TextView)?.setTextColor(textColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Enter button setup
            enterButton.setOnClickListener {
                val costStr = costEditText.text.toString()
                if (costStr.isEmpty()) return@setOnClickListener
                val cost: Int
                try {
                    cost = costStr.toInt()
                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.invalid_input),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
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
                adapter = ExpenseAdapter(
                    items,
                    themeLiveData.value!!,
                    object : ExpenseAdapter.ExpenseAdapterListener {
                        override fun onClick(item: Expense) {
                            OptionSheet(
                                item,
                                themeLiveData,
                                expensesLiveData,
                                tagsLiveData,
                                repository
                            ).show(supportFragmentManager, "OptionSheet")
                        }

                        override fun onClick(date: String) {
                            SummarySheet(expensesLiveData, tagsLiveData, themeLiveData, date).show(
                                supportFragmentManager,
                                "SummarySheet"
                            )
                        }
                    })
            }
        }
    }

    private fun setTagSpinner(items: List<String>) {
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_spinner,
            items
        ) {
            @SuppressLint("ViewHolder")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val binding = ItemSpinnerBinding.inflate(layoutInflater)
                binding.textView.text = getItem(position)
                binding.textView.setTextColor(themeLiveData.value!!.primaryTXT(applicationContext))
                binding.imageView.imageTintList =
                    ColorStateList.valueOf(themeLiveData.value!!.primaryTXT(applicationContext))
                return binding.root
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.tagSpinner.adapter = adapter
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

                settingsButton.setCardBackgroundColor(primaryTXT)
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
                tagSpinner.popupBackground.setTint(getColor(R.color.lightPrimaryBackground))
                (tagSpinner.getChildAt(0) as? TextView)?.setTextColor(primaryTXT)

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

                tagSpinner.findViewById<TextView>(R.id.textView)?.setTextColor(primaryTXT)
                tagSpinner.findViewById<ImageView>(R.id.imageView)?.imageTintList =
                    ColorStateList.valueOf(primaryTXT)
            }

            setRecyclerView(expensesLiveData.value!!)
        })
        tagsLiveData.observe(this) { tags ->
            val dropdownItems = ArrayList<String>()
            dropdownItems.add(getString(R.string.no_tag))
            dropdownItems.addAll(tags)
            setTagSpinner(dropdownItems)
        }
        expensesLiveData.observe(this) { expenses ->
            setRecyclerView(expenses.sortedByDescending { it.date.toLong() })
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
            expensesLiveData.postValue(repository.getAllExpenses())
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
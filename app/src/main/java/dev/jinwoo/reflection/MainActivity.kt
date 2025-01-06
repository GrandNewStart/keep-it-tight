package dev.jinwoo.reflection

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.jinwoo.reflection.db.Expense
import dev.jinwoo.reflection.db.ExpenseDatabase
import dev.jinwoo.reflection.db.ExpenseRepository
import dev.jinwoo.reflection.fragments.OptionSheet
import dev.jinwoo.reflection.fragments.SettingsSheet
import dev.jinwoo.reflection.fragments.SummarySheet
import dev.jinwoo.reflection.expense.ExpenseAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var rootView: View;
    private lateinit var titleText: TextView;
    private lateinit var settingsButton: MaterialCardView;
    private lateinit var settingsImage: ImageView;
    private lateinit var headerNameText: TextView
    private lateinit var headerCostText: TextView
    private lateinit var headerTagText: TextView
    private lateinit var costEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var tagSpinner: Spinner
    private lateinit var enterButton: MaterialButton
    private lateinit var summaryButton: MaterialButton
    private lateinit var recyclerView: RecyclerView

    private lateinit var database: ExpenseDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var storage: Storage

    private val themeLiveData = MutableLiveData(Theme.Dark)
    private val expensesLiveData = MutableLiveData<List<Expense>>(listOf())
    private val tagsLiveData = MutableLiveData<List<String>>(listOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        observe()
        handleSoftKeyboard()
        initData()
        themeLiveData.postValue(storage.getTheme())
    }

    private fun initView() {
        rootView = findViewById(android.R.id.content)
        titleText = findViewById(R.id.titleText)

        settingsButton = findViewById(R.id.settingsButton)
        settingsImage = findViewById(R.id.settingsImage)
        headerNameText = findViewById(R.id.headerNameText)
        headerCostText = findViewById(R.id.headerCostText)
        headerTagText = findViewById(R.id.headerTagText)
        costEditText = findViewById(R.id.costEditText)
        tagSpinner = findViewById(R.id.tagSpinner)
        nameEditText = findViewById(R.id.nameEditText)
        enterButton = findViewById(R.id.enterButton)
        summaryButton = findViewById(R.id.summaryButton)
        recyclerView = findViewById(R.id.recyclerView)
        rootView.setBackgroundColor(getColor(R.color.darkPrimaryBackground))
        settingsButton.setOnClickListener {
            val settingsSheet =
                SettingsSheet(themeLiveData, expensesLiveData, tagsLiveData, repository)
            settingsSheet.show(supportFragmentManager, "SettingsSheet")
        }
        initSpinner()
        setEnterButton()
        setSummaryButton()
    }

    private fun initSpinner() {
        tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val textColor =
                    getColor(if (themeLiveData.value == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText)
                (view as? TextView)?.setTextColor(textColor)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun setRecyclerView(items: List<Expense>) {
        runOnUiThread {
            recyclerView.apply {
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

    private fun setSummaryButton() {
        summaryButton.setOnClickListener {
            val summarySheet = SummarySheet(expensesLiveData, tagsLiveData, themeLiveData)
            summarySheet.show(supportFragmentManager, "SummarySheet")
        }
    }

    private fun setEnterButton() {
        enterButton.setOnClickListener {
            val cost = costEditText.text.toString()
            if (cost.isEmpty()) return@setOnClickListener
            val name = nameEditText.text.toString()
            if (name.isEmpty()) return@setOnClickListener
            val tag = tagSpinner.selectedItem.toString()
            if (tag.isEmpty()) return@setOnClickListener

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(rootView.windowToken, 0)

            CoroutineScope(Dispatchers.Default).launch {
                repository.insert(
                    Expense(
                        date = Date().time.toString(),
                        cost = cost.toInt(),
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

    private fun observe() {
        themeLiveData.observe(this, { theme ->
            storage.setTheme(theme)
            val primaryBG =
                getColor(if (theme == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground)
            val secondaryBG =
                getColor(if (theme == Theme.Dark) R.color.darkSecondaryBackground else R.color.lightSecondaryBackground)
            val primaryTXT =
                getColor(if (theme == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText)
            val secondaryTXT =
                getColor(if (theme == Theme.Dark) R.color.darkSecondaryText else R.color.lightSecondaryText)

            window.statusBarColor = secondaryBG
            val colorAnimator = ValueAnimator.ofArgb(
                getColor(if (theme == Theme.Dark) R.color.lightPrimaryBackground else R.color.darkPrimaryBackground),
                getColor(if (theme == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground)
            )
            colorAnimator.duration = 250
            colorAnimator.addUpdateListener { animator ->
                rootView.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimator.start()

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

            setRecyclerView(expensesLiveData.value!!)
        })
        tagsLiveData.observe(this) { tags ->
            val dropdownItems = ArrayList<String>()
            dropdownItems.add(getString(R.string.no_tag))
            dropdownItems.addAll(tags)
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                dropdownItems
            ).let { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                tagSpinner.adapter = adapter
            }
        }
        expensesLiveData.observe(this) { expenses ->

            setRecyclerView(expenses.sortedByDescending { it.date.toLong() })
        }
    }

    private fun initData() {
        database = ExpenseDatabase.getDatabase(this)
        repository = ExpenseRepository(database.expenseDao())
        loadData()
        storage = Storage(this)
        tagsLiveData.postValue(storage.getTags())
        themeLiveData.postValue(storage.getTheme())
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.Default).launch {
            expensesLiveData.postValue(repository.getAllExpenses())
        }
    }

    private fun handleSoftKeyboard() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
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
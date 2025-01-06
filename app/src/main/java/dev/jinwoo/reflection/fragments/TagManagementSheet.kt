package dev.jinwoo.reflection.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.jinwoo.reflection.R
import dev.jinwoo.reflection.Storage
import dev.jinwoo.reflection.Theme
import dev.jinwoo.reflection.tag.TagAdapter

class TagManagementSheet(
    private val tags: MutableLiveData<List<String>>,
    private val theme: MutableLiveData<Theme>
) : BottomSheetDialogFragment() {

    private lateinit var titleText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: MaterialCardView
    private lateinit var addButtonImage: ImageView
    private lateinit var saveButton: MaterialButton
    private lateinit var storage: Storage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        storage = Storage(requireContext())
        return inflater.inflate(R.layout.fragment_tag_management, container, false).apply {
            titleText = findViewById(R.id.titleText)
            recyclerView = findViewById(R.id.recyclerView)
            addButton = findViewById(R.id.addButton)
            addButtonImage = findViewById(R.id.addButtonImage)
            saveButton = findViewById(R.id.saveButton)
            val transparent = resources.getColor(R.color.transparent, null)
            val primaryBG = resources.getColor(
                if (theme.value == Theme.Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground,
                null
            )
            val primaryTXT = resources.getColor(
                if (theme.value == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText,
                null
            )
            val secondaryTXT = resources.getColor(
                if (theme.value == Theme.Dark) R.color.darkSecondaryText else R.color.lightSecondaryText,
                null
            )
            setBackgroundColor(primaryBG)
            recyclerView.setBackgroundColor(transparent)
            titleText.setTextColor(primaryTXT)
            tags.observe(viewLifecycleOwner) {
                recyclerView.apply {
                    adapter = TagAdapter(ArrayList(it), theme.value!!)
                }
            }
            addButton.setCardBackgroundColor(primaryTXT)
            addButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
            addButtonImage.imageTintList = ColorStateList.valueOf(primaryBG)
            addButton.setOnClickListener {
                (recyclerView.adapter as? TagAdapter)?.let { adapter ->
                    adapter.addNewTag()
                }
            }
            saveButton.setBackgroundColor(primaryTXT)
            saveButton.rippleColor = ColorStateList.valueOf(secondaryTXT)
            saveButton.setTextColor(primaryBG)
            saveButton.setOnClickListener {
                saveTags()
            }
        }
    }

    private fun saveTags() {
        (recyclerView.adapter as? TagAdapter)?.let { adapter ->
            tags.postValue(adapter.getTags())
            storage.setTags(adapter.getTags())
            dismiss()
        }
    }

}
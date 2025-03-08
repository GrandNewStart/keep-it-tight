package dev.bluelemonade.ledger.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.bluelemonade.ledger.GlobalApplication
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Colors
import dev.bluelemonade.ledger.databinding.FragmentTagManagementBinding
import dev.bluelemonade.ledger.tag.TagAdapter

class TagManagementSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTagManagementBinding
    private val app = GlobalApplication.instance

    override fun getTheme(): Int {
        return R.style.Theme_BottomSheetDialog_Fullscreen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTagManagementBinding.inflate(layoutInflater)
        binding.apply {
            app.tagsLiveData.observe(viewLifecycleOwner) {
                recyclerView.apply {
                    adapter = TagAdapter(ArrayList(it))
                }
            }
            addButton.setOnClickListener {
                (recyclerView.adapter as? TagAdapter)?.addNewTag()
            }
            saveButton.setOnClickListener {
                saveTags()
            }
        }
        observeLiveData()
        return binding.root
    }

    private fun observeLiveData() {
        app.themeLiveData.observe(viewLifecycleOwner) {
            binding.apply {
                root.setBackgroundColor(Colors.primaryBackground)
                recyclerView.setBackgroundColor(Colors.transparent)
                titleText.setTextColor(Colors.primaryText)

                addButton.setCardBackgroundColor(Colors.primary)
                addButton.rippleColor = ColorStateList.valueOf(Colors.secondary)

                saveButton.setBackgroundColor(Colors.primary)
                saveButton.rippleColor = ColorStateList.valueOf(Colors.secondary)
            }
        }
    }

    private fun saveTags() {
        (binding.recyclerView.adapter as? TagAdapter)?.let { adapter ->
            app.setTags(adapter.getTags())
            dismiss()
        }
    }

}
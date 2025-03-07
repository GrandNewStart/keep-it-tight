package dev.bluelemonade.ledger.fragments

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
import dev.bluelemonade.ledger.databinding.FragmentTagManagementBinding
import dev.bluelemonade.ledger.tag.TagAdapter

class TagManagementSheet(
    private val tags: MutableLiveData<List<String>>,
    private val theme: MutableLiveData<Theme>
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTagManagementBinding
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
        binding = FragmentTagManagementBinding.inflate(layoutInflater)
        binding.apply {
            tags.observe(viewLifecycleOwner) {
                recyclerView.apply {
                    adapter = TagAdapter(ArrayList(it), theme.value!!)
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
        theme.observe(viewLifecycleOwner) { theme ->
            val transparent = resources.getColor(R.color.transparent, null)
            val primary = theme.primary(requireContext())
            val secondary = theme.secondary(requireContext())
            val primaryBG = theme.primaryBG(requireContext())
            val primaryTXT = theme.primaryTXT(requireContext())

            binding.apply {
                root.setBackgroundColor(primaryBG)
                recyclerView.setBackgroundColor(transparent)
                titleText.setTextColor(primaryTXT)

                addButton.setCardBackgroundColor(primary)
                addButton.rippleColor = ColorStateList.valueOf(secondary)

                saveButton.setBackgroundColor(primary)
                saveButton.rippleColor = ColorStateList.valueOf(secondary)
            }
        }
    }

    private fun saveTags() {
        (binding.recyclerView.adapter as? TagAdapter)?.let { adapter ->
            tags.postValue(adapter.getTags())
            storage.setTags(adapter.getTags())
            dismiss()
        }
    }

}
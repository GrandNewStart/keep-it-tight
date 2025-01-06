package dev.bluelemonade.ledger.tag

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dev.bluelemonade.ledger.R
import dev.bluelemonade.ledger.comm.Theme

class TagAdapter(
    private val tags: ArrayList<String>,
    private val theme: Theme,
) : RecyclerView.Adapter<TagAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var editText: EditText
        private lateinit var deleteButton: MaterialCardView
        fun bind(tag: String) {
            itemView.apply {
                val primaryTXT = resources.getColor(
                    if (theme == Theme.Dark) R.color.darkPrimaryText else R.color.lightPrimaryText,
                    null
                )
                val secondaryTXT = resources.getColor(
                    if (theme == Theme.Dark) R.color.darkSecondaryText else R.color.lightSecondaryText,
                    null
                )
                setBackgroundColor(resources.getColor(R.color.transparent, null))
                editText = findViewById(R.id.editText)
                editText.setTextColor(primaryTXT)
                editText.setHintTextColor(secondaryTXT)
                editText.backgroundTintList = ColorStateList.valueOf(primaryTXT)
                editText.setText(tag)
                editText.addTextChangedListener {
                    tags[adapterPosition] = it.toString()
                }
                deleteButton = findViewById(R.id.deleteButton)
                deleteButton.setOnClickListener {
                    tags.remove(tag)
                    notifyItemRemoved(adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return tags.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    fun getTags(): List<String> {
        return tags.filter { it.isNotEmpty() }
    }

    fun addNewTag() {
        tags.add("")
        notifyItemInserted(tags.count() - 1)

    }

}
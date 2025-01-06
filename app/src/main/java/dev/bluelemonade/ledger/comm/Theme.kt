package dev.bluelemonade.ledger.comm

import android.content.Context
import dev.bluelemonade.ledger.R

enum class Theme {
    Light, Dark;

    fun primaryBG(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground)
    }
    fun secondaryBG(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkSecondaryBackground else R.color.lightSecondaryBackground)
    }
    fun primaryTXT(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkPrimaryText else R.color.lightPrimaryText)
    }
    fun secondaryTXT(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkSecondaryText else R.color.lightSecondaryText)
    }
}
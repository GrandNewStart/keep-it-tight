package dev.bluelemonade.ledger.comm

import android.content.Context
import dev.bluelemonade.ledger.R

enum class Theme {
    Light, Dark;

    fun primaryBackground(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkPrimaryBackground else R.color.lightPrimaryBackground)
    }

    fun secondaryBackground(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkSecondaryBackground else R.color.lightSecondaryBackground)
    }

    fun primaryText(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkPrimaryText else R.color.lightPrimaryText)
    }

    fun secondaryText(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkSecondaryText else R.color.lightSecondaryText)
    }

    fun primary(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkPrimary else R.color.lightPrimary)
    }

    fun secondary(context: Context): Int {
        return context.getColor(if (this == Dark) R.color.darkSecondary else R.color.lightSecondary)
    }
}
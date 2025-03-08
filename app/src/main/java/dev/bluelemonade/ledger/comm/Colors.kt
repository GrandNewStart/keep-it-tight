package dev.bluelemonade.ledger.comm

import dev.bluelemonade.ledger.GlobalApplication.Companion.instance
import dev.bluelemonade.ledger.R

object Colors {
    val white = instance.resources.getColor(R.color.white, null)
    val red = instance.resources.getColor(R.color.red, null)
    val green = instance.resources.getColor(R.color.green, null)
    val transparent = instance.resources.getColor(R.color.transparent, null)
    val primary: Int get() = instance.theme.primary(instance)
    val primaryBackground: Int get() = instance.theme.primaryBackground(instance)
    val primaryText: Int get() = instance.theme.primaryText(instance)
    val secondary: Int get() = instance.theme.secondary(instance)
    val secondaryBackground: Int get() = instance.theme.secondaryBackground(instance)
    val secondaryText: Int get() = instance.theme.secondaryText(instance)
}
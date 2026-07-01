package com.localbookkeeping.app

import java.util.Locale
import kotlin.math.abs

fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val amount = abs(cents / 100.0)
    return "${sign}¥${String.format(Locale.US, "%.2f", amount)}"
}

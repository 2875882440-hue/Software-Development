package com.localbookkeeping.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun formatsMoneyForUi() {
        assertEquals("¥0.00", formatMoney(0L))
        assertEquals("¥12.50", formatMoney(1250L))
        assertEquals("-¥12.50", formatMoney(-1250L))
    }
}

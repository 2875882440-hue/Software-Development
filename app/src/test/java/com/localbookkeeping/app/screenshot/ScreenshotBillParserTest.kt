package com.localbookkeeping.app.screenshot

import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenshotBillParserTest {
    private val parser = ScreenshotBillParser()

    @Test
    fun parsesWechatScreenshotWithAmount() {
        val result = parser.parse(
            rawText = """
                微信支付
                支付成功
                ¥12.80
                商户：便利店
                2026-06-10 12:30:00
            """.trimIndent()
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals("wechat_screenshot", bill!!.sourceType)
        assertEquals(1280L, bill.amountCents)
        assertEquals(RecordStatus.PENDING_CONFIRM, bill.status)
        assertEquals(TransactionType.EXPENSE, bill.type)
        assertEquals("便利店", bill.merchant)
    }

    @Test
    fun parsesAlipayScreenshotWithYuanAmount() {
        val result = parser.parse(
            rawText = """
                支付宝
                交易成功
                实付 36.00
            """.trimIndent()
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals("alipay_screenshot", bill!!.sourceType)
        assertEquals(3600L, bill.amountCents)
        assertEquals(RecordStatus.PENDING_CONFIRM, bill.status)
    }

    @Test
    fun createsNeedAmountForPaymentScreenshotWithoutAmount() {
        val result = parser.parse(rawText = "微信支付\n付款成功")

        val bill = result.bill
        assertNotNull(bill)
        assertNull(bill!!.amountCents)
        assertEquals(RecordStatus.NEED_AMOUNT, bill.status)
        assertEquals("screenshot_need_amount", result.parseStatus)
    }

    @Test
    fun parsesNegativeAmountAsPositiveExpense() {
        val result = parser.parse(rawText = "账单详情\n-12.80\n交易成功")

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(1280L, bill!!.amountCents)
        assertEquals(TransactionType.EXPENSE, bill.type)
    }

    @Test
    fun reportsInvalidScreenshot() {
        val result = parser.parse(rawText = "今天阳光很好")

        assertNull(result.bill)
        assertEquals("未识别到有效账单信息", result.failureReason)
    }
}

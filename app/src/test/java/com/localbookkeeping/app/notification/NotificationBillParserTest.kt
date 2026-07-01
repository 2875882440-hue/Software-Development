package com.localbookkeeping.app.notification

import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationBillParserTest {
    private val parser = NotificationBillParser()

    @Test
    fun parsesWechatExpenseNotification() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "你已成功付款 ¥12.50"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(1250L, bill!!.amountCents)
        assertEquals(TransactionType.EXPENSE, bill.type)
        assertEquals("微信", bill.sourceApp)
    }

    @Test
    fun parsesAlipayIncomeNotification() {
        val result = parser.parse(
            packageName = "com.eg.android.AlipayGphone",
            title = "支付宝收款到账",
            text = "收到 88.00元"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(8800L, bill!!.amountCents)
        assertEquals(TransactionType.INCOME, bill.type)
        assertEquals("支付宝", bill.sourceApp)
    }

    @Test
    fun ignoresOtherApps() {
        val result = parser.parse(
            packageName = "com.example.other",
            title = "支付通知",
            text = "支付 ¥9.90"
        )

        assertNull(result.bill)
        assertEquals("非微信/支付宝通知", result.failureReason)
    }

    @Test
    fun parsesChineseYuanSymbolAmount() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "扣款成功 ￥12.80"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(1280L, bill!!.amountCents)
        assertEquals(TransactionType.EXPENSE, bill.type)
    }

    @Test
    fun createsNeedAmountBillForPaymentTextWithoutAmount() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "支付成功"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertNull(bill!!.amountCents)
        assertEquals(RecordStatus.NEED_AMOUNT, bill.status)
        assertEquals(TransactionType.EXPENSE, bill.type)
        assertEquals("need_amount", result.parseStatus)
        assertEquals("识别到支付关键词但没有可靠金额，生成待补金额账单", result.parseReason)
    }

    @Test
    fun parsesRequestedAmountFormats() {
        val cases = listOf(
            "微信支付 付款成功 ¥12.80" to TransactionType.EXPENSE,
            "微信支付 付款成功 ￥12.80" to TransactionType.EXPENSE,
            "微信支付 支付12.80元" to TransactionType.EXPENSE,
            "微信支付 付款金额12.80元" to TransactionType.EXPENSE,
            "微信支付 已付款12.80元" to TransactionType.EXPENSE,
            "微信支付 实付12.80元" to TransactionType.EXPENSE,
            "微信支付 金额12.80" to TransactionType.UNKNOWN,
            "微信支付 -12.80" to TransactionType.UNKNOWN,
            "微信支付 支出12.80" to TransactionType.EXPENSE,
            "微信支付 12.80元 付款成功" to TransactionType.EXPENSE,
            "微信支付 收款12.80元" to TransactionType.INCOME
        )

        cases.forEach { (text, type) ->
            val bill = parser.parse(
                packageName = "com.tencent.mm",
                title = "",
                text = text
            ).bill

            assertNotNull(text, bill)
            assertEquals(text, 1280L, bill!!.amountCents)
            assertEquals(text, type, bill.type)
        }
    }

    @Test
    fun reportsBlankRawText() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "",
            text = "  "
        )

        assertNull(result.bill)
        assertEquals("rawText为空", result.failureReason)
    }

    @Test
    fun usesUnknownTypeWhenDirectionIsUnclear() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信通知",
            text = "交易成功 ¥12.80"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(1280L, bill!!.amountCents)
        assertEquals(RecordStatus.PENDING_CONFIRM, bill.status)
        assertEquals(TransactionType.UNKNOWN, bill.type)
    }

    @Test
    fun createsNeedAmountBillsForRequestedNoAmountExamples() {
        val cases = listOf(
            Triple("com.tencent.mm", "微信支付 付款成功", TransactionType.EXPENSE),
            Triple("com.eg.android.AlipayGphone", "支付宝 交易提醒", TransactionType.UNKNOWN),
            Triple("com.tencent.mm", "你有一笔收款到账", TransactionType.INCOME)
        )

        cases.forEach { (packageName, text, type) ->
            val bill = parser.parse(
                packageName = packageName,
                title = "",
                text = text
            ).bill

            assertNotNull(text, bill)
            assertNull(text, bill!!.amountCents)
            assertEquals(text, RecordStatus.NEED_AMOUNT, bill.status)
            assertEquals(text, type, bill.type)
            assertEquals(text, true, bill.isPaymentNotification)
            assertEquals(text, false, bill.hasAmount)
        }
    }

    @Test
    fun createsPendingConfirmBillsForRequestedAmountExamples() {
        val cases = listOf(
            Triple("com.tencent.mm", "微信支付 ¥12.80", TransactionType.UNKNOWN),
            Triple("com.eg.android.AlipayGphone", "支付宝付款 36.00元", TransactionType.EXPENSE)
        )

        cases.forEach { (packageName, text, type) ->
            val bill = parser.parse(
                packageName = packageName,
                title = "",
                text = text
            ).bill

            assertNotNull(text, bill)
            assertEquals(text, RecordStatus.PENDING_CONFIRM, bill!!.status)
            assertEquals(text, type, bill.type)
            assertEquals(text, true, bill.hasAmount)
        }
    }

    @Test
    fun extractsMerchantFromExplicitMerchantField() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "商户：美团外卖\n付款成功 ¥28.50"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals("美团外卖", bill!!.merchantName)
        assertEquals(TransactionType.EXPENSE, bill.type)
    }

    @Test
    fun extractsMerchantFromNaturalPaymentText() {
        val result = parser.parse(
            packageName = "com.eg.android.AlipayGphone",
            title = "支付宝",
            text = "向星巴克付款 36.00元"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals("星巴克", bill!!.merchantName)
        assertEquals(3600L, bill.amountCents)
    }

    @Test
    fun ignoresAmountOnlyWhenPaymentKeywordIsMissing() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "美团外卖",
            text = "¥28.50"
        )

        assertNull(result.bill)
        assertEquals("未识别为支付相关通知", result.failureReason)
        assertEquals(2850L, result.diagnostics.selectedAmount?.amountCents)
    }

    @Test
    fun parsesRealWechatIncomeNotification() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "个人收款码到账¥2.00"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(200L, bill!!.amountCents)
        assertEquals(TransactionType.INCOME, bill.type)
        assertEquals(RecordStatus.PENDING_CONFIRM, bill.status)
        assertEquals(true, bill.hasAmount)
        assertEquals(listOf("收款", "到账", "微信支付"), result.diagnostics.matchedKeywords)
    }

    @Test
    fun parsesRealAlipayAutoDebitNotification() {
        val result = parser.parse(
            packageName = "com.eg.android.AlipayGphone",
            title = "交易提醒",
            text = "你在浙江嗨便利网络科技有限公司有一笔4.50元的免密/自动扣款支付，点击领取10个支付宝积分。"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(450L, bill!!.amountCents)
        assertEquals(TransactionType.EXPENSE, bill.type)
        assertEquals("浙江嗨便利网络科技有限公司", bill.merchantName)
        assertEquals(RecordStatus.PENDING_CONFIRM, bill.status)
    }

    @Test
    fun choosesAmountNearPaymentKeywordsInsteadOfDates() {
        val result = parser.parse(
            packageName = "com.eg.android.AlipayGphone",
            title = "支付宝",
            text = "订单号20260611163703\n06月11日 16:37:03\n付款12.80元"
        )

        val bill = result.bill
        assertNotNull(bill)
        assertEquals(1280L, bill!!.amountCents)
        assertEquals(1, result.diagnostics.amountCandidates.count { it.amountCents == 1280L })
    }

    @Test
    fun extractsRequestedMerchantPatterns() {
        val cases = listOf(
            "微信支付\n向 星巴克 付款\n¥36.00" to "星巴克",
            "微信支付\n付款给 李四\n¥8.80" to "李四",
            "支付宝\n收款方：便利店\n已付款12.80元" to "便利店",
            "支付宝\n对方账户：张三\n转账12.00元" to "张三",
            "微信支付\n小罗商店 收款成功\n¥6.00" to "小罗商店"
        )

        cases.forEach { (rawText, merchant) ->
            val bill = parser.parse(
                packageName = if (rawText.contains("支付宝")) "com.eg.android.AlipayGphone" else "com.tencent.mm",
                title = "",
                text = rawText
            ).bill

            assertNotNull(rawText, bill)
            assertEquals(rawText, merchant, bill!!.merchantName)
        }
    }

    @Test
    fun fingerprintsKeepDifferentAmountsSeparate() {
        val first = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "商户：便利店\n付款成功 ¥12.80",
            postTimeMillis = 60_000L
        ).bill
        val second = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "商户：便利店\n付款成功 ¥18.80",
            postTimeMillis = 60_000L
        ).bill

        assertNotNull(first)
        assertNotNull(second)
        assertNotEquals(first!!.fingerprint, second!!.fingerprint)
    }

    @Test
    fun parsesThirtySevenYuanPaymentExamples() {
        val cases = listOf(
            "微信支付 付款成功 37元",
            "支付宝 消费37.00元",
            "向某某商户付款37元",
            "交易金额37.00元",
            "¥37",
            "￥37.00"
        )

        cases.forEach { text ->
            val bill = parser.parse(
                packageName = if (text.contains("支付宝")) "com.eg.android.AlipayGphone" else "com.tencent.mm",
                title = "微信支付",
                text = text
            ).bill

            assertNotNull(text, bill)
            assertEquals(text, 3700L, bill!!.amountCents)
        }
    }

    @Test
    fun choosesThirtySevenYuanInsteadOfOrderNumbersOrTime() {
        val cases = listOf(
            "支付成功 37元 订单号123456",
            "12:30 支付37元",
            "2026-06-30 支付37元 订单号123456789",
            "手机尾号1234 支付37元"
        )

        cases.forEach { text ->
            val result = parser.parse(
                packageName = "com.tencent.mm",
                title = "微信支付",
                text = text
            )

            assertNotNull(text, result.bill)
            assertEquals(text, 3700L, result.bill!!.amountCents)
            assertEquals(text, 3700L, result.diagnostics.selectedAmount?.amountCents)
        }
    }

    @Test
    fun repairsSeparatedDigitsThatUsedToReadOnlyFirstDigit() {
        val result = parser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "微信支付 付款成功 3 7元"
        )

        assertNotNull(result.bill)
        assertEquals(3700L, result.bill!!.amountCents)
    }
}

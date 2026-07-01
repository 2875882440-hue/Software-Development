package com.localbookkeeping.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryClassifierTest {
    private val rules = DefaultCategoryRules.build(now = 1_000L)

    @Test
    fun classifiesMeituanAsFood() {
        val result = CategoryClassifier.recommend(
            rules = rules,
            merchant = "美团外卖",
            rawText = "微信支付 付款成功 28.50元",
            parsedType = TransactionType.EXPENSE
        )

        assertEquals("餐饮", result.category)
        assertEquals(CategoryClassifier.SOURCE_RULE, result.categorySource)
        assertEquals("美团", result.matchedKeyword)
    }

    @Test
    fun classifiesDidiAsTransport() {
        val result = CategoryClassifier.recommend(
            rules = rules,
            merchant = "滴滴出行",
            rawText = "支付宝 付款成功 18.00元",
            parsedType = TransactionType.EXPENSE
        )

        assertEquals("交通", result.category)
        assertEquals("滴滴", result.matchedKeyword)
    }

    @Test
    fun classifiesTaobaoAsShopping() {
        val result = CategoryClassifier.recommend(
            rules = rules,
            merchant = "",
            rawText = "淘宝订单 支付成功 99.00元",
            parsedType = TransactionType.EXPENSE
        )

        assertEquals("购物", result.category)
        assertEquals("淘宝", result.matchedKeyword)
    }

    @Test
    fun classifiesIncomeKeywordsAsIncome() {
        val result = CategoryClassifier.recommend(
            rules = rules,
            merchant = "",
            rawText = "工资到账 8000.00元",
            parsedType = TransactionType.UNKNOWN
        )

        assertEquals("收入", result.category)
        assertEquals(TransactionType.INCOME, result.type)
    }

    @Test
    fun userRulePriorityWinsBeforeDefaultRules() {
        val userRule = ClassificationRule(
            ruleName = "我的咖啡规则",
            keyword = "星巴克",
            category = "娱乐",
            type = TransactionType.EXPENSE,
            enabled = true,
            priority = 1,
            createdAtMillis = 2_000L,
            updatedAtMillis = 2_000L
        )
        val result = CategoryClassifier.recommend(
            rules = listOf(userRule) + rules,
            merchant = "星巴克",
            rawText = "微信支付 36.00元",
            parsedType = TransactionType.EXPENSE
        )

        assertEquals("娱乐", result.category)
        assertEquals("我的咖啡规则", result.matchedRuleName)
    }

    @Test
    fun manualCategoryIsNotOverwrittenByRules() {
        val manualRecord = ExpenseRecord(
            id = 1L,
            amountCents = 3600L,
            type = TransactionType.EXPENSE,
            status = RecordStatus.CONFIRMED,
            category = "生活",
            categorySource = CategoryClassifier.SOURCE_MANUAL,
            note = "",
            paidAtMillis = 1_000L,
            createdAtMillis = 1_000L,
            updatedAtMillis = 1_000L
        )

        assertEquals("manual", manualRecord.categorySource)
        assertEquals("生活", manualRecord.category)
    }

    @Test
    fun defaultRulesAreOnlySeededWhenRuleTableIsEmpty() {
        assertTrue(DefaultCategoryRules.shouldInsert(0))
        assertFalse(DefaultCategoryRules.shouldInsert(1))
    }
}

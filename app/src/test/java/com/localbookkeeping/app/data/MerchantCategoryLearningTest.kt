package com.localbookkeeping.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantCategoryLearningTest {
    @Test
    fun normalizesSimilarMerchantNames() {
        assertEquals(
            MerchantNormalizer.normalize("星巴克咖啡"),
            MerchantNormalizer.normalize("星巴克咖啡中国")
        )
        assertEquals("瑞幸咖啡", MerchantNormalizer.normalize("瑞幸咖啡 37.00元 订单号123456"))
    }

    @Test
    fun recommendsLearnedCategoryForSameMerchant() {
        val learning = learning("瑞幸咖啡", "餐饮", useCount = 2)

        val recommendation = MerchantLearningMatcher.bestMatch(
            merchantName = "瑞幸咖啡",
            rawText = "微信支付 向瑞幸咖啡付款37元",
            sourceApp = "微信",
            learnings = listOf(learning)
        )

        assertNotNull(recommendation)
        assertEquals("餐饮", recommendation!!.learning.category)
    }

    @Test
    fun updatesRecommendationWhenRecentCategoryHasHigherUseCount() {
        val oldCategory = learning("瑞幸咖啡", "餐饮", useCount = 1, lastUsedAt = 100)
        val newCategory = learning("瑞幸咖啡", "其他", useCount = 3, lastUsedAt = 200)

        val recommendation = MerchantLearningMatcher.bestMatch(
            merchantName = "瑞幸咖啡",
            rawText = "瑞幸咖啡",
            sourceApp = "微信",
            learnings = listOf(oldCategory, newCategory)
        )

        assertNotNull(recommendation)
        assertEquals("其他", recommendation!!.learning.category)
    }

    @Test
    fun disabledLearningIsNotRecommended() {
        val learning = learning("瑞幸咖啡", "餐饮", useCount = 3, enabled = false)

        val recommendation = MerchantLearningMatcher.bestMatch(
            merchantName = "瑞幸咖啡",
            rawText = "瑞幸咖啡",
            sourceApp = "微信",
            learnings = listOf(learning)
        )

        assertNull(recommendation)
    }

    @Test
    fun merchantCandidatesBringCategory() {
        val candidates = MerchantLearningMatcher.candidates(
            input = "瑞",
            learnings = listOf(learning("瑞幸咖啡", "餐饮", useCount = 2)),
            limit = 3
        )

        assertEquals(1, candidates.size)
        assertEquals("瑞幸咖啡", candidates.first().merchantDisplayName)
        assertEquals("餐饮", candidates.first().category)
    }

    private fun learning(
        merchant: String,
        category: String,
        useCount: Int = 1,
        lastUsedAt: Long = 1_000,
        enabled: Boolean = true
    ): MerchantCategoryLearning =
        MerchantCategoryLearning(
            id = lastUsedAt,
            merchantNormalized = MerchantNormalizer.normalize(merchant),
            merchantDisplayName = merchant,
            category = category,
            sourceApp = "微信",
            matchKeyword = MerchantNormalizer.keyword(merchant),
            useCount = useCount,
            confidence = 50,
            lastUsedAt = lastUsedAt,
            createdAt = lastUsedAt,
            updatedAt = lastUsedAt,
            isEnabled = enabled
        )
}

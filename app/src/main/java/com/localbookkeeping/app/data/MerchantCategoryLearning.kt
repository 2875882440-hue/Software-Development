package com.localbookkeeping.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_category_learning",
    indices = [
        Index(value = ["merchantNormalized", "category", "sourceApp"], unique = true),
        Index(value = ["merchantNormalized"]),
        Index(value = ["lastUsedAt"])
    ]
)
data class MerchantCategoryLearning(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantNormalized: String,
    val merchantDisplayName: String,
    val category: String,
    val sourceApp: String = "unknown",
    val matchKeyword: String = "",
    val useCount: Int = 1,
    val confidence: Int = 50,
    val lastUsedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isEnabled: Boolean = true
)

data class MerchantLearningRecommendation(
    val learning: MerchantCategoryLearning,
    val score: Int,
    val reason: String
)

object MerchantNormalizer {
    fun normalize(input: String): String {
        val noAmounts = input
            .lowercase()
            .replace(Regex("""(?:订单号|尾号|手机号|通知|第\d+笔|优惠券|积分)\s*[:：]?\s*\d+"""), " ")
            .replace(Regex("""[¥￥]?\s*-?\d+(?:,\d{3})*(?:\.\d{1,2})?\s*元?"""), " ")
            .replace(Regex("""\d{1,4}[-/:年月日]\d{1,2}(?:[-/:年月日]\d{1,2})?"""), " ")
            .replace(Regex("""[^\p{IsHan}a-z0-9]+"""), "")
        return noAmounts
            .replace("咖啡中国", "咖啡")
            .replace("中国", "")
            .trim()
            .take(40)
    }

    fun keyword(input: String): String {
        val normalized = normalize(input)
        return when {
            normalized.length <= 4 -> normalized
            normalized.any { it in '\u4e00'..'\u9fff' } -> normalized.take(4)
            else -> normalized.take(8)
        }
    }
}

object MerchantLearningMatcher {
    fun bestMatch(
        merchantName: String,
        rawText: String,
        sourceApp: String,
        learnings: List<MerchantCategoryLearning>
    ): MerchantLearningRecommendation? {
        val merchantNormalized = MerchantNormalizer.normalize(merchantName)
        val rawNormalized = MerchantNormalizer.normalize(rawText)
        if (merchantNormalized.isBlank() && rawNormalized.isBlank()) return null

        return learnings
            .filter { it.isEnabled && it.merchantNormalized.isNotBlank() }
            .mapNotNull { learning ->
                val score = scoreLearning(learning, merchantNormalized, rawNormalized, sourceApp)
                if (score > 0) MerchantLearningRecommendation(learning, score, matchReason(score)) else null
            }
            .maxWithOrNull(
                compareBy<MerchantLearningRecommendation> { it.score }
                    .thenBy { it.learning.useCount }
                    .thenBy { it.learning.lastUsedAt }
            )
    }

    fun candidates(
        input: String,
        learnings: List<MerchantCategoryLearning>,
        limit: Int = 5
    ): List<MerchantCategoryLearning> {
        val normalized = MerchantNormalizer.normalize(input)
        return learnings
            .filter { it.isEnabled }
            .filter {
                normalized.isBlank() ||
                    it.merchantNormalized.contains(normalized) ||
                    normalized.contains(it.merchantNormalized) ||
                    it.merchantDisplayName.contains(input, ignoreCase = true)
            }
            .sortedWith(compareByDescending<MerchantCategoryLearning> { it.useCount }.thenByDescending { it.lastUsedAt })
            .take(limit)
    }

    private fun scoreLearning(
        learning: MerchantCategoryLearning,
        merchantNormalized: String,
        rawNormalized: String,
        sourceApp: String
    ): Int {
        var score = 0
        val learned = learning.merchantNormalized
        if (merchantNormalized == learned) score += 100
        if (merchantNormalized.isNotBlank() && (merchantNormalized.contains(learned) || learned.contains(merchantNormalized))) score += 75
        if (rawNormalized.contains(learned)) score += 55
        if (sourceApp.isNotBlank() && learning.sourceApp == sourceApp) score += 10
        score += (learning.useCount.coerceAtMost(5) * 5)
        return score
    }

    private fun matchReason(score: Int): String =
        if (score >= 100) "商户完全匹配" else "商户相似匹配"
}

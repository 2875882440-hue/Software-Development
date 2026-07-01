package com.localbookkeeping.app.data

data class CategoryRecommendation(
    val category: String,
    val categorySource: String,
    val matchedRuleName: String = "",
    val matchedKeyword: String = "",
    val type: TransactionType = TransactionType.UNKNOWN
)

object CategoryClassifier {
    const val SOURCE_MANUAL = "manual"
    const val SOURCE_LEARNED = "learned"
    const val SOURCE_RULE = "rule"
    const val SOURCE_UNKNOWN = "unknown"

    fun recommend(
        rules: List<ClassificationRule>,
        merchant: String,
        rawText: String,
        title: String = "",
        text: String = "",
        sourceApp: String = "",
        parsedType: TransactionType = TransactionType.UNKNOWN
    ): CategoryRecommendation {
        val enabledRules = rules
            .filter { it.enabled }
            .sortedWith(compareBy<ClassificationRule> { it.priority }.thenByDescending { it.updatedAtMillis }.thenByDescending { it.id })

        findMatch(enabledRules, merchant)?.let { return it }
        findMatch(enabledRules, listOf(rawText, title, text).joinToString("\n"))?.let { return it }

        if (parsedType == TransactionType.INCOME || sourceApp.contains("收入")) {
            return CategoryRecommendation(
                category = "收入",
                categorySource = SOURCE_RULE,
                matchedRuleName = "来源规则",
                matchedKeyword = sourceApp.ifBlank { "income" },
                type = TransactionType.INCOME
            )
        }

        return CategoryRecommendation(
            category = "未分类",
            categorySource = SOURCE_UNKNOWN,
            type = parsedType
        )
    }

    private fun findMatch(rules: List<ClassificationRule>, content: String): CategoryRecommendation? {
        val normalizedContent = content.lowercase()
        if (normalizedContent.isBlank()) return null
        rules.forEach { rule ->
            splitKeywords(rule.keyword).forEach { keyword ->
                if (keyword.isNotBlank() && normalizedContent.contains(keyword.lowercase())) {
                    return CategoryRecommendation(
                        category = rule.category,
                        categorySource = SOURCE_RULE,
                        matchedRuleName = rule.ruleName.ifBlank { rule.keyword },
                        matchedKeyword = keyword,
                        type = rule.type
                    )
                }
            }
        }
        return null
    }

    fun splitKeywords(keywords: String): List<String> =
        keywords
            .split(",", "，", "、", "\n", ";", "；")
            .map { it.trim() }
            .filter { it.isNotBlank() }
}

object DefaultCategoryRules {
    fun shouldInsert(existingRuleCount: Int): Boolean = existingRuleCount == 0

    fun build(now: Long = System.currentTimeMillis()): List<ClassificationRule> =
        listOf(
            defaultRule("默认-餐饮", "美团,饿了么,麦当劳,肯德基,星巴克,瑞幸,餐饮,饭店,外卖,奶茶,超市熟食", "餐饮", TransactionType.EXPENSE, 100, now),
            defaultRule("默认-交通", "滴滴,高德打车,地铁,公交,出租车,加油,停车,高铁,火车,机票", "交通", TransactionType.EXPENSE, 110, now),
            defaultRule("默认-购物", "淘宝,京东,拼多多,抖音商城,天猫,商城,便利店,超市,商店", "购物", TransactionType.EXPENSE, 120, now),
            defaultRule("默认-娱乐", "电影,游戏,KTV,会员,视频,音乐,直播", "娱乐", TransactionType.EXPENSE, 130, now),
            defaultRule("默认-生活", "水费,电费,燃气,话费,宽带,快递,物业,家政", "生活", TransactionType.EXPENSE, 140, now),
            defaultRule("默认-医疗", "医院,药房,药店,诊所,医疗", "医疗", TransactionType.EXPENSE, 150, now),
            defaultRule("默认-收入", "收款,到账,工资,转账收入,退款", "收入", TransactionType.INCOME, 160, now)
        )

    private fun defaultRule(
        name: String,
        keywords: String,
        category: String,
        type: TransactionType,
        priority: Int,
        now: Long
    ): ClassificationRule =
        ClassificationRule(
            ruleName = name,
            keyword = keywords,
            category = category,
            type = type,
            enabled = true,
            priority = priority,
            createdAtMillis = now,
            updatedAtMillis = now
        )
}

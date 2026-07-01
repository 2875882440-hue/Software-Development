package com.localbookkeeping.app.data

import com.localbookkeeping.app.backup.BackupRestoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.max
import kotlin.math.min

data class PendingInsertResult(
    val inserted: Boolean,
    val ruleMatched: Boolean = false,
    val matchedRuleName: String = "",
    val matchedKeyword: String = "",
    val finalCategory: String = "",
    val finalType: TransactionType = TransactionType.UNKNOWN,
    val confidence: Int = 0,
    val duplicateReason: String = ""
)

private data class CategoryMatch(
    val category: String,
    val type: TransactionType,
    val categorySource: String,
    val matchedRuleName: String = "",
    val matchedKeyword: String = "",
    val learnedMerchantId: Long = 0L
)

class BookkeepingRepository(
    private val expenseDao: ExpenseDao,
    private val debugNotificationLogDao: DebugNotificationLogDao? = null,
    private val classificationRuleDao: ClassificationRuleDao? = null,
    private val backgroundStabilityLogDao: BackgroundStabilityLogDao? = null,
    private val merchantCategoryLearningDao: MerchantCategoryLearningDao? = null
) {
    val expenses: Flow<List<ExpenseRecord>> = expenseDao.observeAll()
    val debugNotificationLogs: Flow<List<DebugNotificationLog>> =
        debugNotificationLogDao?.observeRecent100() ?: flowOf(emptyList())
    val classificationRules: Flow<List<ClassificationRule>> =
        classificationRuleDao?.observeAll() ?: flowOf(emptyList())
    val backgroundStabilityLogs: Flow<List<BackgroundStabilityLog>> =
        backgroundStabilityLogDao?.observeRecent200() ?: flowOf(emptyList())
    val merchantCategoryLearnings: Flow<List<MerchantCategoryLearning>> =
        merchantCategoryLearningDao?.observeAll() ?: flowOf(emptyList())

    suspend fun initializeDefaultClassificationRules() {
        val dao = classificationRuleDao ?: return
        if (DefaultCategoryRules.shouldInsert(dao.countAll())) {
            dao.insertAll(DefaultCategoryRules.build())
        }
    }

    suspend fun addRecord(
        amountCents: Long,
        type: TransactionType,
        category: String,
        note: String,
        paidAtMillis: Long,
        merchantName: String = "",
        sourceApp: String = "手动"
    ) {
        val now = System.currentTimeMillis()
        val learnedMerchantId = recordMerchantCategoryLearning(
            merchantName = merchantName,
            category = category,
            sourceApp = sourceApp,
            now = now
        )
        expenseDao.insert(
            ExpenseRecord(
                amountCents = amountCents,
                type = type,
                status = RecordStatus.CONFIRMED,
                category = category,
                categorySource = CategoryClassifier.SOURCE_MANUAL,
                note = note,
                paidAtMillis = paidAtMillis,
                merchantName = merchantName,
                sourceApp = sourceApp,
                learnedMerchantId = learnedMerchantId,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun addClassificationRule(
        ruleName: String,
        keyword: String,
        category: String,
        type: TransactionType,
        priority: Int = 10
    ) {
        val dao = requireNotNull(classificationRuleDao) { "Classification rule DAO is not configured." }
        val now = System.currentTimeMillis()
        dao.insert(
            ClassificationRule(
                ruleName = ruleName.trim().ifBlank { keyword.trim().take(20) },
                keyword = keyword.trim(),
                category = category.trim(),
                type = type,
                enabled = true,
                priority = priority,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun updateClassificationRule(
        rule: ClassificationRule,
        ruleName: String,
        keyword: String,
        category: String,
        type: TransactionType,
        priority: Int
    ) {
        val dao = requireNotNull(classificationRuleDao) { "Classification rule DAO is not configured." }
        dao.update(
            rule.copy(
                ruleName = ruleName.trim().ifBlank { keyword.trim().take(20) },
                keyword = keyword.trim(),
                category = category.trim(),
                type = type,
                priority = priority,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun setClassificationRuleEnabled(rule: ClassificationRule, enabled: Boolean) {
        requireNotNull(classificationRuleDao) { "Classification rule DAO is not configured." }
            .setEnabled(rule.id, enabled, System.currentTimeMillis())
    }

    suspend fun deleteClassificationRule(rule: ClassificationRule) {
        requireNotNull(classificationRuleDao) { "Classification rule DAO is not configured." }.delete(rule)
    }

    suspend fun setMerchantLearningEnabled(learning: MerchantCategoryLearning, enabled: Boolean) {
        merchantCategoryLearningDao?.setEnabled(learning.id, enabled, System.currentTimeMillis())
    }

    suspend fun addPendingFromNotification(
        amountCents: Long?,
        type: TransactionType,
        status: RecordStatus,
        sourceApp: String,
        note: String,
        rawText: String,
        merchantName: String,
        notificationTitle: String,
        notificationText: String,
        notificationPackageName: String,
        notificationFingerprint: String,
        paidAtMillis: Long,
        isPaymentNotification: Boolean
    ): PendingInsertResult {
        val normalizedRawText = normalizeForDuplicate(rawText)
        val categoryMatch = matchCategory(
            rawText = rawText,
            merchantName = merchantName,
            sourceApp = sourceApp,
            parsedType = type,
            title = notificationTitle,
            text = notificationText
        )
        val finalType = categoryMatch.type
        val finalCategory = categoryMatch.category
        val finalMerchant = merchantName.trim()
        val confidence = calculateConfidence(
            amountCents = amountCents,
            type = finalType,
            category = finalCategory,
            merchantName = finalMerchant,
            isPaymentNotification = isPaymentNotification
        )
        val duplicateReason = findDuplicateReason(
            sourceApp = sourceApp,
            packageName = notificationPackageName,
            amountCents = amountCents ?: 0L,
            rawText = rawText,
            normalizedRawText = normalizedRawText,
            notificationFingerprint = notificationFingerprint,
            paidAtMillis = paidAtMillis
        )
        if (duplicateReason.isNotBlank()) {
            return PendingInsertResult(
                inserted = false,
                ruleMatched = categoryMatch.categorySource != CategoryClassifier.SOURCE_UNKNOWN,
                matchedRuleName = categoryMatch.matchedRuleName,
                matchedKeyword = categoryMatch.matchedKeyword,
                finalCategory = finalCategory,
                finalType = finalType,
                confidence = confidence,
                duplicateReason = duplicateReason
            )
        }

        val now = System.currentTimeMillis()
        expenseDao.insert(
            ExpenseRecord(
                amountCents = amountCents ?: 0L,
                type = finalType,
                status = status,
                category = finalCategory,
                categorySource = categoryMatch.categorySource,
                note = note,
                paidAtMillis = paidAtMillis,
                sourceApp = sourceApp,
                rawText = rawText,
                merchantName = finalMerchant,
                notificationTitle = notificationTitle,
                notificationText = notificationText,
                notificationPackageName = notificationPackageName,
                notificationPostedAtMillis = paidAtMillis,
                notificationFingerprint = notificationFingerprint,
                confidence = confidence,
                matchedRuleName = categoryMatch.matchedRuleName,
                matchedKeyword = categoryMatch.matchedKeyword,
                learnedMerchantId = categoryMatch.learnedMerchantId,
                normalizedRawText = normalizedRawText,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return PendingInsertResult(
            inserted = true,
            ruleMatched = categoryMatch.categorySource != CategoryClassifier.SOURCE_UNKNOWN,
            matchedRuleName = categoryMatch.matchedRuleName,
            matchedKeyword = categoryMatch.matchedKeyword,
            finalCategory = finalCategory,
            finalType = finalType,
            confidence = confidence
        )
    }

    suspend fun addManualPendingFromNotificationLog(
        log: DebugNotificationLog,
        amountCents: Long?,
        type: TransactionType,
        sourceApp: String,
        merchantName: String,
        note: String,
        paidAtMillis: Long = log.postTime.takeIf { it > 0L } ?: log.receivedAtMillis
    ): Long {
        val now = System.currentTimeMillis()
        val status = if (amountCents != null && amountCents > 0L) {
            RecordStatus.PENDING_CONFIRM
        } else {
            RecordStatus.NEED_AMOUNT
        }
        val categoryMatch = matchCategory(
            rawText = log.rawText,
            merchantName = merchantName,
            sourceApp = sourceApp,
            parsedType = type,
            title = log.title,
            text = log.text
        )
        val finalType = categoryMatch.type
        val finalCategory = categoryMatch.category
        return expenseDao.insert(
            ExpenseRecord(
                amountCents = amountCents ?: 0L,
                type = finalType,
                status = status,
                category = finalCategory,
                categorySource = categoryMatch.categorySource,
                note = note.ifBlank { "从通知详情手动生成" },
                paidAtMillis = paidAtMillis,
                sourceApp = sourceApp,
                rawText = log.rawText,
                merchantName = merchantName.trim(),
                notificationTitle = log.title,
                notificationText = log.rawText,
                notificationPackageName = log.packageName,
                notificationPostedAtMillis = paidAtMillis,
                notificationFingerprint = "manual-debug-log-${log.id}-$now",
                confidence = calculateConfidence(amountCents, finalType, finalCategory, merchantName, true),
                matchedRuleName = categoryMatch.matchedRuleName,
                matchedKeyword = categoryMatch.matchedKeyword,
                learnedMerchantId = categoryMatch.learnedMerchantId,
                normalizedRawText = normalizeForDuplicate(log.rawText),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun addPendingFromScreenshot(
        amountCents: Long?,
        type: TransactionType,
        status: RecordStatus,
        sourceApp: String,
        sourceType: String,
        note: String,
        rawText: String,
        imageUri: String,
        ocrText: String,
        notificationFingerprint: String,
        paidAtMillis: Long,
        merchantName: String = ""
    ): Long? {
        if (expenseDao.countByNotificationFingerprint(notificationFingerprint) > 0) return null
        val now = System.currentTimeMillis()
        val categoryMatch = matchCategory(
            rawText = rawText,
            merchantName = merchantName,
            sourceApp = sourceApp,
            parsedType = type,
            title = "截图OCR",
            text = rawText
        )
        val finalType = categoryMatch.type
        val finalCategory = categoryMatch.category
        return expenseDao.insert(
            ExpenseRecord(
                amountCents = amountCents ?: 0L,
                type = finalType,
                status = status,
                category = finalCategory,
                categorySource = categoryMatch.categorySource,
                note = note,
                paidAtMillis = paidAtMillis,
                sourceApp = sourceApp,
                rawText = rawText,
                merchantName = merchantName,
                notificationTitle = "截图OCR",
                notificationText = rawText,
                notificationPackageName = sourceType,
                notificationPostedAtMillis = paidAtMillis,
                notificationFingerprint = notificationFingerprint,
                confidence = calculateConfidence(amountCents, finalType, finalCategory, merchantName, true),
                matchedRuleName = categoryMatch.matchedRuleName,
                matchedKeyword = categoryMatch.matchedKeyword,
                learnedMerchantId = categoryMatch.learnedMerchantId,
                normalizedRawText = normalizeForDuplicate(rawText),
                imageUri = imageUri,
                screenshotPath = imageUri,
                ocrText = ocrText,
                sourceType = "screenshot",
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun addConfirmedScreenshotRecord(
        amountCents: Long,
        type: TransactionType,
        category: String,
        note: String,
        paidAtMillis: Long,
        sourceApp: String,
        sourceType: String,
        rawText: String,
        imageUri: String,
        ocrText: String,
        merchantName: String = ""
    ) {
        val now = System.currentTimeMillis()
        val learnedMerchantId = recordMerchantCategoryLearning(
            merchantName = merchantName,
            category = category,
            sourceApp = sourceApp,
            now = now
        )
        expenseDao.insert(
            ExpenseRecord(
                amountCents = amountCents,
                type = type,
                status = RecordStatus.CONFIRMED,
                category = category,
                categorySource = CategoryClassifier.SOURCE_MANUAL,
                note = note,
                paidAtMillis = paidAtMillis,
                sourceApp = sourceApp,
                rawText = rawText,
                merchantName = merchantName,
                notificationTitle = "截图OCR",
                notificationText = rawText,
                notificationPackageName = sourceType,
                notificationPostedAtMillis = paidAtMillis,
                notificationFingerprint = "",
                confidence = calculateConfidence(amountCents, type, category, merchantName, true),
                learnedMerchantId = learnedMerchantId,
                normalizedRawText = normalizeForDuplicate(rawText),
                imageUri = imageUri,
                screenshotPath = imageUri,
                ocrText = ocrText,
                sourceType = "screenshot",
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun confirmPending(id: Long) {
        expenseDao.confirmPending(id, System.currentTimeMillis())
    }

    suspend fun updatePendingAndConfirm(
        id: Long,
        amountCents: Long,
        type: TransactionType,
        category: String,
        merchantName: String,
        note: String,
        paidAtMillis: Long
    ) {
        val now = System.currentTimeMillis()
        val learnedMerchantId = recordMerchantCategoryLearning(
            merchantName = merchantName,
            category = category,
            sourceApp = "manual",
            now = now
        )
        expenseDao.updatePendingAndConfirm(
            id = id,
            amountCents = amountCents,
            type = type,
            category = category,
            merchantName = merchantName.trim(),
            note = note,
            paidAtMillis = paidAtMillis,
            learnedMerchantId = learnedMerchantId,
            updatedAtMillis = now
        )
    }

    suspend fun updateConfirmedRecord(
        id: Long,
        amountCents: Long,
        type: TransactionType,
        category: String,
        merchantName: String,
        note: String,
        paidAtMillis: Long
    ) {
        val now = System.currentTimeMillis()
        val learnedMerchantId = recordMerchantCategoryLearning(
            merchantName = merchantName,
            category = category,
            sourceApp = "manual",
            now = now
        )
        expenseDao.updateConfirmedRecord(
            id = id,
            amountCents = amountCents,
            type = type,
            category = category,
            merchantName = merchantName.trim(),
            note = note,
            paidAtMillis = paidAtMillis,
            learnedMerchantId = learnedMerchantId,
            updatedAtMillis = now
        )
    }

    suspend fun softDeleteRecord(id: Long) {
        expenseDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun ignorePending(id: Long) {
        expenseDao.ignorePending(id, System.currentTimeMillis())
    }

    suspend fun getRecordsForBackup(): List<ExpenseRecord> =
        expenseDao.getAllIncludingDeletedOnce()

    suspend fun restoreRecordsFromBackup(records: List<ExpenseRecord>): BackupRestoreResult {
        val existingKeys = expenseDao.getAllIncludingDeletedOnce()
            .flatMap { it.backupDuplicateKeys() }
            .toMutableSet()
        var imported = 0
        var skipped = 0
        records.forEach { record ->
            val keys = record.backupDuplicateKeys()
            if (keys.isNotEmpty() && keys.any { it in existingKeys }) {
                skipped++
            } else {
                val restored = record.copy(id = 0L)
                expenseDao.insert(restored)
                existingKeys.addAll(restored.backupDuplicateKeys())
                imported++
            }
        }
        return BackupRestoreResult(
            totalCount = records.size,
            importedCount = imported,
            skippedCount = skipped
        )
    }

    suspend fun addDebugNotificationLog(log: DebugNotificationLog): Long =
        requireNotNull(debugNotificationLogDao) { "Debug notification log DAO is not configured." }.insert(log)

    suspend fun countDebugNotificationLogsSince(sinceMillis: Long): Int =
        requireNotNull(debugNotificationLogDao) { "Debug notification log DAO is not configured." }.countSince(sinceMillis)

    suspend fun addBackgroundStabilityLog(
        eventType: String,
        message: String = "",
        detail: String = "",
        createdAtMillis: Long = System.currentTimeMillis()
    ): Long? =
        backgroundStabilityLogDao?.insert(
            BackgroundStabilityLog(
                eventType = eventType,
                message = message,
                detail = detail,
                createdAtMillis = createdAtMillis
            )
        )

    suspend fun getBackgroundStabilityLogsSince(sinceMillis: Long): List<BackgroundStabilityLog> =
        backgroundStabilityLogDao?.getSince(sinceMillis).orEmpty()

    suspend fun updateDebugNotificationParseResult(
        id: Long,
        parseStatus: String,
        failureReason: String,
        isPaymentNotification: Boolean = false,
        hasAmount: Boolean = false,
        pendingCreated: Boolean = false,
        parseReason: String = failureReason,
        ruleMatched: Boolean = false,
        matchedRuleName: String = "",
        confidence: Int = 0,
        finalCategory: String = "",
        isPaymentRelated: Boolean = isPaymentNotification,
        isParsed: Boolean = pendingCreated,
        failReason: String = failureReason
    ) {
        requireNotNull(debugNotificationLogDao) { "Debug notification log DAO is not configured." }
            .updateParseResult(
                id = id,
                parseStatus = parseStatus,
                failureReason = failureReason,
                isPaymentNotification = isPaymentNotification,
                hasAmount = hasAmount,
                pendingCreated = pendingCreated,
                parseReason = parseReason,
                ruleMatched = ruleMatched,
                matchedRuleName = matchedRuleName,
                confidence = confidence,
                finalCategory = finalCategory,
                isPaymentRelated = isPaymentRelated,
                isParsed = isParsed,
                failReason = failReason
            )
    }

    private suspend fun matchCategory(
        rawText: String,
        merchantName: String,
        sourceApp: String,
        parsedType: TransactionType,
        title: String = "",
        text: String = ""
    ): CategoryMatch {
        val learningRecommendation = MerchantLearningMatcher.bestMatch(
            merchantName = merchantName,
            rawText = listOf(rawText, title, text).joinToString("\n"),
            sourceApp = sourceApp,
            learnings = merchantCategoryLearningDao?.getEnabledOnce().orEmpty()
        )
        if (learningRecommendation != null) {
            val learning = learningRecommendation.learning
            if (learning.useCount >= 2) {
                return CategoryMatch(
                    category = learning.category,
                    type = parsedType,
                    categorySource = CategoryClassifier.SOURCE_LEARNED,
                    matchedRuleName = "历史学习",
                    matchedKeyword = learning.merchantDisplayName,
                    learnedMerchantId = learning.id
                )
            }
        }

        val matchedRule = matchClassificationRule(
            rawText = rawText,
            merchantName = merchantName,
            title = title,
            text = text
        )
        return CategoryMatch(
            category = matchedRule?.category ?: "未分类",
            type = matchedRule?.type ?: parsedType,
            categorySource = if (matchedRule != null) CategoryClassifier.SOURCE_RULE else CategoryClassifier.SOURCE_UNKNOWN,
            matchedRuleName = matchedRule?.ruleName?.ifBlank { matchedRule.keyword }.orEmpty(),
            matchedKeyword = matchedRule?.keyword.orEmpty()
        )
    }

    private suspend fun recordMerchantCategoryLearning(
        merchantName: String,
        category: String,
        sourceApp: String,
        now: Long = System.currentTimeMillis()
    ): Long {
        val dao = merchantCategoryLearningDao ?: return 0L
        val normalized = MerchantNormalizer.normalize(merchantName)
        val cleanCategory = category.trim()
        if (normalized.isBlank() || cleanCategory.isBlank() || cleanCategory == "未分类" || cleanCategory == "待选择") return 0L

        val cleanSource = sourceApp.trim().ifBlank { "unknown" }
        val existing = dao.findExact(normalized, cleanCategory, cleanSource)
        if (existing != null) {
            dao.update(
                existing.copy(
                    merchantDisplayName = merchantName.trim().ifBlank { existing.merchantDisplayName },
                    matchKeyword = MerchantNormalizer.keyword(merchantName),
                    useCount = existing.useCount + 1,
                    confidence = (existing.confidence + 15).coerceAtMost(100),
                    lastUsedAt = now,
                    updatedAt = now,
                    isEnabled = true
                )
            )
            return existing.id
        }

        return dao.insert(
            MerchantCategoryLearning(
                merchantNormalized = normalized,
                merchantDisplayName = merchantName.trim(),
                category = cleanCategory,
                sourceApp = cleanSource,
                matchKeyword = MerchantNormalizer.keyword(merchantName),
                useCount = 1,
                confidence = 50,
                lastUsedAt = now,
                createdAt = now,
                updatedAt = now,
                isEnabled = true
            )
        )
    }

    private suspend fun matchClassificationRule(
        rawText: String,
        merchantName: String,
        title: String = "",
        text: String = ""
    ): ClassificationRule? {
        val rules = classificationRuleDao?.getEnabledRulesOnce().orEmpty()
            .ifEmpty { DefaultCategoryRules.build(0L) }
            .sortedWith(compareBy<ClassificationRule> { it.priority }.thenByDescending { it.updatedAtMillis }.thenByDescending { it.id })
        val merchantContent = merchantName.lowercase()
        findRuleInContent(rules, merchantContent)?.let { return it }
        val content = listOf(rawText, title, text).joinToString("\n").lowercase()
        return findRuleInContent(rules, content)
    }

    private fun findRuleInContent(rules: List<ClassificationRule>, content: String): ClassificationRule? {
        if (content.isBlank()) return null
        rules.forEach { rule ->
            CategoryClassifier.splitKeywords(rule.keyword).forEach { keyword ->
                if (content.contains(keyword.lowercase())) {
                    return rule.copy(keyword = keyword)
                }
            }
        }
        return null
    }

    private suspend fun findDuplicateReason(
        sourceApp: String,
        packageName: String,
        amountCents: Long,
        rawText: String,
        normalizedRawText: String,
        notificationFingerprint: String,
        paidAtMillis: Long
    ): String {
        if (expenseDao.countByNotificationFingerprint(notificationFingerprint) > 0) {
            return "同一通知指纹已存在"
        }
        val candidates = expenseDao.findRecentNotificationCandidates(
            sourceApp = sourceApp,
            packageName = packageName,
            amountCents = amountCents,
            fromMillis = paidAtMillis - DUPLICATE_WINDOW_MILLIS,
            toMillis = paidAtMillis + DUPLICATE_WINDOW_MILLIS
        )
        val duplicate = candidates.firstOrNull { candidate ->
            val candidateNormalized = candidate.normalizedRawText.ifBlank { normalizeForDuplicate(candidate.rawText) }
            areSimilarForDuplicate(normalizedRawText, candidateNormalized) ||
                areSimilarForDuplicate(rawText, candidate.rawText)
        }
        return if (duplicate != null) "同来源、同金额、相似通知内容在短时间内已存在" else ""
    }

    private fun calculateConfidence(
        amountCents: Long?,
        type: TransactionType,
        category: String,
        merchantName: String,
        isPaymentNotification: Boolean
    ): Int {
        val hasAmount = amountCents != null && amountCents > 0
        val hasType = type != TransactionType.UNKNOWN
        val hasCategory = category.isNotBlank() && category != "待分类" && category != "未分类"
        val hasMerchant = merchantName.isNotBlank()
        return when {
            hasAmount && hasType && hasCategory && hasMerchant -> 100
            hasAmount && hasType -> 70
            isPaymentNotification && !hasAmount -> 40
            isPaymentNotification -> 20
            else -> 0
        }
    }

    private fun normalizeForDuplicate(text: String): String =
        text.lowercase()
            .replace(amountRegex, "")
            .replace(dateTimeRegex, "")
            .filter { it.isLetter() || it.isDigit() }
            .filterNot { it.isDigit() }

    private fun areSimilarForDuplicate(left: String, right: String): Boolean {
        val a = normalizeForDuplicate(left)
        val b = normalizeForDuplicate(right)
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        val shorter = min(a.length, b.length)
        if (shorter >= 8 && (a.contains(b) || b.contains(a))) return true
        return jaccard(bigrams(a), bigrams(b)) >= DUPLICATE_TEXT_SIMILARITY
    }

    private fun ExpenseRecord.backupDuplicateKeys(): List<String> {
        val keys = mutableListOf<String>()
        if (notificationFingerprint.isNotBlank()) {
            keys += "fingerprint:${notificationFingerprint.trim()}"
        }
        val normalizedRaw = normalizedRawText.ifBlank { normalizeForDuplicate(rawText) }
        keys += listOf(
            "signature",
            amountCents.toString(),
            type.name,
            status.name,
            paidAtMillis.toString(),
            sourceApp.trim(),
            sourceType.trim(),
            merchantName.trim(),
            note.trim(),
            rawText.trim(),
            normalizedRaw,
            category.trim()
        ).joinToString("|")
        return keys
    }

    private fun bigrams(text: String): Set<String> =
        if (text.length < 2) setOf(text) else text.windowed(2).toSet()

    private fun jaccard(left: Set<String>, right: Set<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val intersection = left.intersect(right).size
        val union = left.size + right.size - intersection
        return intersection.toDouble() / max(union, 1)
    }

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS = 60_000L
        const val DUPLICATE_TEXT_SIMILARITY = 0.82
        val amountRegex = Regex("""[¥￥]?\s*-?[0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?\s*元?""")
        val dateTimeRegex = Regex("""\d{4}[-/年]\d{1,2}[-/月]\d{1,2}[日\s]*(?:\d{1,2}:\d{2}(?::\d{2})?)?""")
    }
}

package com.localbookkeeping.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localbookkeeping.app.data.BookkeepingRepository
import com.localbookkeeping.app.data.BackgroundEventType
import com.localbookkeeping.app.data.BackgroundStabilityLog
import com.localbookkeeping.app.data.ClassificationRule
import com.localbookkeeping.app.data.DebugNotificationLog
import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.MerchantCategoryLearning
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import com.localbookkeeping.app.notification.NotificationBillParser
import com.localbookkeeping.app.screenshot.ParsedScreenshotBill
import com.localbookkeeping.app.screenshot.ScreenshotBillParser
import com.localbookkeeping.app.screenshot.ScreenshotParseResult
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookkeepingViewModel(
    private val repository: BookkeepingRepository
) : ViewModel() {
    private val parser = NotificationBillParser()
    private val screenshotParser = ScreenshotBillParser()

    init {
        viewModelScope.launch {
            repository.initializeDefaultClassificationRules()
        }
    }

    val uiState: StateFlow<BookkeepingUiState> = combine(
        repository.expenses,
        repository.debugNotificationLogs,
        repository.classificationRules,
        repository.backgroundStabilityLogs,
        repository.merchantCategoryLearnings
    ) { records, debugLogs, rules, backgroundLogs, learnings ->
            val confirmed = records.filter { it.status == RecordStatus.CONFIRMED }
            val pending = records.filter {
                it.status == RecordStatus.PENDING_CONFIRM ||
                    it.status == RecordStatus.NEED_AMOUNT ||
                    it.status == RecordStatus.PENDING
            }
            BookkeepingUiState(
                confirmedRecords = confirmed,
                pendingRecords = pending,
                pendingCount = pending.size,
                incomeCents = confirmed.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
                expenseCents = confirmed.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
                debugNotificationLogs = debugLogs,
                classificationRules = rules,
                backgroundStabilityLogs = backgroundLogs,
                merchantCategoryLearnings = learnings
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookkeepingUiState())

    fun addRecord(
        amountCents: Long,
        type: TransactionType,
        category: String,
        note: String,
        paidAtMillis: Long,
        merchantName: String = "",
        sourceApp: String = "手动"
    ) {
        viewModelScope.launch {
            repository.addRecord(
                amountCents = amountCents,
                type = type,
                category = category,
                note = note,
                paidAtMillis = paidAtMillis,
                merchantName = merchantName,
                sourceApp = sourceApp
            )
        }
    }

    fun confirmPending(id: Long) {
        viewModelScope.launch {
            repository.confirmPending(id)
        }
    }

    fun updatePendingAndConfirm(
        id: Long,
        amountCents: Long,
        type: TransactionType,
        category: String,
        merchantName: String,
        note: String,
        paidAtMillis: Long
    ) {
        viewModelScope.launch {
            repository.updatePendingAndConfirm(id, amountCents, type, category, merchantName, note, paidAtMillis)
        }
    }

    fun addClassificationRule(keyword: String, category: String, type: TransactionType) {
        addClassificationRule(keyword, keyword, category, type, 10)
    }

    fun addClassificationRule(ruleName: String, keyword: String, category: String, type: TransactionType, priority: Int) {
        viewModelScope.launch {
            if (keyword.isNotBlank() && category.isNotBlank()) {
                repository.addClassificationRule(
                    ruleName = ruleName,
                    keyword = keyword,
                    category = category,
                    type = type,
                    priority = priority
                )
            }
        }
    }

    fun updateClassificationRule(rule: ClassificationRule, keyword: String, category: String, type: TransactionType) {
        updateClassificationRule(rule, rule.ruleName.ifBlank { keyword }, keyword, category, type, rule.priority)
    }

    fun updateClassificationRule(rule: ClassificationRule, ruleName: String, keyword: String, category: String, type: TransactionType, priority: Int) {
        viewModelScope.launch {
            if (keyword.isNotBlank() && category.isNotBlank()) {
                repository.updateClassificationRule(
                    rule = rule,
                    ruleName = ruleName,
                    keyword = keyword,
                    category = category,
                    type = type,
                    priority = priority
                )
            }
        }
    }

    fun updateConfirmedRecord(
        id: Long,
        amountCents: Long,
        type: TransactionType,
        category: String,
        merchantName: String,
        note: String,
        paidAtMillis: Long
    ) {
        viewModelScope.launch {
            repository.updateConfirmedRecord(id, amountCents, type, category, merchantName, note, paidAtMillis)
        }
    }

    fun softDeleteRecord(id: Long) {
        viewModelScope.launch {
            repository.softDeleteRecord(id)
        }
    }

    fun setClassificationRuleEnabled(rule: ClassificationRule, enabled: Boolean) {
        viewModelScope.launch {
            repository.setClassificationRuleEnabled(rule, enabled)
        }
    }

    fun addBackgroundEvent(eventType: String, message: String = "", detail: String = "") {
        viewModelScope.launch {
            repository.addBackgroundStabilityLog(eventType, message, detail)
        }
    }

    fun recordAutoListenChanged(enabled: Boolean) {
        addBackgroundEvent(
            eventType = if (enabled) BackgroundEventType.AUTO_LISTEN_ENABLED else BackgroundEventType.AUTO_LISTEN_DISABLED,
            message = if (enabled) "用户开启自动监听" else "用户关闭自动监听"
        )
    }

    fun deleteClassificationRule(rule: ClassificationRule) {
        viewModelScope.launch {
            repository.deleteClassificationRule(rule)
        }
    }

    fun setMerchantLearningEnabled(learning: MerchantCategoryLearning, enabled: Boolean) {
        viewModelScope.launch {
            repository.setMerchantLearningEnabled(learning, enabled)
        }
    }

    fun ignorePending(id: Long) {
        viewModelScope.launch {
            repository.ignorePending(id)
        }
    }

    fun parseScreenshotText(rawText: String, imageUri: String): ScreenshotParseResult =
        screenshotParser.parse(rawText = rawText, imageUri = imageUri)

    suspend fun addScreenshotDebugLog(
        imageUri: String,
        rawText: String,
        parseResult: ScreenshotParseResult,
        pendingCreated: Boolean
    ) {
        val now = System.currentTimeMillis()
        val logId = repository.addDebugNotificationLog(
            DebugNotificationLog(
                packageName = parseResult.sourceType,
                title = "截图OCR识别",
                text = rawText,
                subText = "",
                bigText = "",
                textLines = "",
                rawText = rawText,
                postTime = now,
                receivedAtMillis = now,
                parseStatus = parseResult.parseStatus,
                failureReason = parseResult.failureReason,
                isPaymentNotification = parseResult.isPaymentScreenshot,
                hasAmount = parseResult.hasAmount,
                pendingCreated = pendingCreated,
                parseReason = parseResult.parseReason
            )
        )
        repository.updateDebugNotificationParseResult(
            id = logId,
            parseStatus = parseResult.parseStatus,
            failureReason = parseResult.failureReason,
            isPaymentNotification = parseResult.isPaymentScreenshot,
            hasAmount = parseResult.hasAmount,
            pendingCreated = pendingCreated,
            parseReason = parseResult.parseReason
        )
    }

    suspend fun createPendingFromScreenshot(imageUri: String, parsed: ParsedScreenshotBill): Long? =
        repository.addPendingFromScreenshot(
            amountCents = parsed.amountCents,
            type = parsed.type,
            status = parsed.status,
            sourceApp = parsed.sourceApp,
            sourceType = parsed.sourceType,
            note = buildScreenshotNote(parsed),
            rawText = parsed.rawText,
            imageUri = imageUri,
            ocrText = parsed.rawText,
            notificationFingerprint = parsed.fingerprint,
            paidAtMillis = parsed.paidAtMillis ?: System.currentTimeMillis(),
            merchantName = parsed.merchant
        )

    fun addConfirmedScreenshotRecord(
        amountCents: Long,
        type: TransactionType,
        category: String,
        note: String,
        paidAtMillis: Long,
        sourceApp: String,
        sourceType: String,
        rawText: String,
        imageUri: String,
        merchantName: String = ""
    ) {
        viewModelScope.launch {
            repository.addConfirmedScreenshotRecord(
                amountCents = amountCents,
                type = type,
                category = category,
                note = note,
                paidAtMillis = paidAtMillis,
                sourceApp = sourceApp,
                sourceType = sourceType,
                rawText = rawText,
                imageUri = imageUri,
                ocrText = rawText,
                merchantName = merchantName
            )
        }
    }

    fun simulatePaymentNotificationParse(rawInput: String) {
        viewModelScope.launch {
            val rawText = normalizeRawText(rawInput)
            val packageName = detectSimulatedPackageName(rawText)
            val title = "模拟支付通知解析"
            val now = System.currentTimeMillis()
            val logId = repository.addDebugNotificationLog(
                DebugNotificationLog(
                    packageName = packageName,
                    title = title,
                    text = rawText,
                    subText = "",
                    bigText = "",
                    textLines = "",
                    rawText = rawText,
                    postTime = now,
                    receivedAtMillis = now,
                    parseStatus = "SIMULATED",
                    failureReason = ""
                )
            )

            val parseResult = parser.parse(
                packageName = packageName,
                title = title,
                text = rawText,
                postTimeMillis = now
            )
            val parsed = parseResult.bill
            if (parsed == null) {
                repository.updateDebugNotificationParseResult(
                    id = logId,
                    parseStatus = "SIMULATION_FAILED",
                    failureReason = parseResult.failureReason,
                    isPaymentNotification = parseResult.isPaymentNotification,
                    hasAmount = parseResult.hasAmount,
                    pendingCreated = false,
                    parseReason = parseResult.parseReason
                )
                return@launch
            }

            val inserted = repository.addPendingFromNotification(
                amountCents = parsed.amountCents,
                type = parsed.type,
                status = parsed.status,
                sourceApp = parsed.sourceApp,
                note = parsed.note,
                rawText = parsed.rawText,
                merchantName = parsed.merchantName,
                notificationTitle = title,
                notificationText = parsed.rawText,
                notificationPackageName = packageName,
                notificationFingerprint = parsed.fingerprint,
                paidAtMillis = now,
                isPaymentNotification = parsed.isPaymentNotification
            )
            repository.updateDebugNotificationParseResult(
                id = logId,
                parseStatus = if (inserted.inserted) "SIMULATION_${parsed.parseStatus.uppercase()}" else "DUPLICATE",
                failureReason = if (inserted.inserted) "" else inserted.duplicateReason.ifBlank { "被判定为重复通知" },
                isPaymentNotification = parsed.isPaymentNotification,
                hasAmount = parsed.hasAmount,
                pendingCreated = inserted.inserted,
                parseReason = if (inserted.inserted) parsed.parseReason else inserted.duplicateReason.ifBlank { "被判定为重复通知" },
                ruleMatched = inserted.ruleMatched,
                matchedRuleName = inserted.matchedRuleName,
                confidence = inserted.confidence,
                finalCategory = inserted.finalCategory
            )
        }
    }

    suspend fun createPendingFromNotificationLog(log: DebugNotificationLog): Long {
        val parseResult = parser.parse(
            packageName = log.packageName,
            title = log.title,
            text = log.rawText,
            postTimeMillis = log.postTime
        )
        val parsed = parseResult.bill
        val sourceApp = parsed?.sourceApp ?: sourceNameForPackage(log.packageName)
        val pendingId = repository.addManualPendingFromNotificationLog(
            log = log,
            amountCents = parsed?.amountCents,
            type = parsed?.type ?: TransactionType.UNKNOWN,
            sourceApp = sourceApp,
            merchantName = parsed?.merchantName.orEmpty(),
            note = parsed?.note ?: "${sourceApp}通知：待手动确认"
        )
        repository.updateDebugNotificationParseResult(
            id = log.id,
            parseStatus = "manual_pending_created",
            failureReason = parseResult.failureReason,
            isPaymentNotification = parseResult.isPaymentNotification,
            hasAmount = parseResult.hasAmount,
            pendingCreated = true,
            parseReason = "已从通知详情手动生成待确认账单",
            isPaymentRelated = log.isPaymentRelated || parseResult.isPaymentNotification,
            isParsed = parsed != null,
            failReason = parseResult.failureReason
        )
        return pendingId
    }

    private fun normalizeRawText(text: String): String =
        text.replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")

    private fun detectSimulatedPackageName(rawText: String): String =
        if (rawText.contains("支付宝")) "com.eg.android.AlipayGphone" else "com.tencent.mm"

    private fun sourceNameForPackage(packageName: String): String = when (packageName) {
        "com.tencent.mm" -> "微信"
        "com.eg.android.AlipayGphone" -> "支付宝"
        else -> "通知"
    }

    private fun buildScreenshotNote(parsed: ParsedScreenshotBill): String =
        listOf(parsed.sourceApp, parsed.merchant.ifBlank { "截图识别" })
            .joinToString("：")
            .take(80)
}

data class BookkeepingUiState(
    val confirmedRecords: List<ExpenseRecord> = emptyList(),
    val pendingRecords: List<ExpenseRecord> = emptyList(),
    val pendingCount: Int = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val debugNotificationLogs: List<DebugNotificationLog> = emptyList(),
    val classificationRules: List<ClassificationRule> = emptyList(),
    val backgroundStabilityLogs: List<BackgroundStabilityLog> = emptyList(),
    val merchantCategoryLearnings: List<MerchantCategoryLearning> = emptyList()
)

class BookkeepingViewModelFactory(
    private val repository: BookkeepingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        BookkeepingViewModel(repository) as T
}

package com.localbookkeeping.app.notification

import android.app.KeyguardManager
import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.localbookkeeping.app.AppVisibilityTracker
import com.localbookkeeping.app.analytics.AutoBookkeepingEvent
import com.localbookkeeping.app.analytics.AutoBookkeepingStatsStore
import com.localbookkeeping.app.data.AppDatabase
import com.localbookkeeping.app.data.BackgroundEventType
import com.localbookkeeping.app.data.BookkeepingRepository
import com.localbookkeeping.app.data.DebugNotificationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = NotificationBillParser()
    private var heartbeatJob: Job? = null

    private val repository by lazy {
        val database = AppDatabase.create(this)
        BookkeepingRepository(
            expenseDao = database.expenseDao(),
            debugNotificationLogDao = database.debugNotificationLogDao(),
            classificationRuleDao = database.classificationRuleDao(),
            backgroundStabilityLogDao = database.backgroundStabilityLogDao(),
            merchantCategoryLearningDao = database.merchantCategoryLearningDao()
        )
    }
    private val autoBookkeepingStats by lazy { AutoBookkeepingStatsStore.create(this) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationListenerState.markConnected(this)
        startHeartbeat()
        scope.launch {
            repository.addBackgroundStabilityLog(BackgroundEventType.LISTENER_CONNECTED, "通知监听服务已连接")
        }
        Log.i(TAG, "onListenerConnected componentName=${NotificationListenerState.componentName(this)}")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        stopHeartbeat()
        NotificationListenerState.markDisconnected(this)
        scope.launch {
            repository.addBackgroundStabilityLog(BackgroundEventType.LISTENER_DISCONNECTED, "通知监听服务已断开")
            delay(4_000)
            ListenerRecoveryManager.checkNow(
                context = this@PaymentNotificationListenerService,
                repository = repository,
                source = "onListenerDisconnected",
                forceRebind = false
            )
        }
        Log.w(TAG, "onListenerDisconnected componentName=${NotificationListenerState.componentName(this)}")
    }

    override fun onDestroy() {
        stopHeartbeat()
        NotificationListenerState.markDisconnected(this)
        scope.launch {
            repository.addBackgroundStabilityLog(BackgroundEventType.LISTENER_DISCONNECTED, "通知监听服务销毁")
        }
        Log.w(TAG, "onDestroy componentName=${NotificationListenerState.componentName(this)}")
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val content = collectNotificationContent(sbn.notification)
        NotificationListenerState.markNotificationReceived(
            context = this,
            packageName = sbn.packageName,
            title = content.title,
            text = content.text.ifBlank { content.rawText }
        )
        val probeToken = ListenerProbeNotification.probeToken(sbn.id, sbn.notification.extras)
        if (sbn.packageName == packageName && probeToken != null) {
            val capturedAt = System.currentTimeMillis()
            ListenerProbeNotification.markCaptured(this, probeToken, capturedAt)
            ListenerRecoveryState.markHealthCheck(
                this,
                ListenerHealthEvaluation(ListenerServiceStatus.HEALTHY, listOf("探测通知已捕获")),
                capturedAt
            )
            scope.launch {
                repository.addBackgroundStabilityLog(
                    BackgroundEventType.PROBE_SUCCESS,
                    "probeSuccess",
                    "token=$probeToken",
                    capturedAt
                )
            }
            Log.i(TAG, "probe notification captured token=$probeToken")
            return
        }
        val appName = MonitoredAppConfig.appName(this, sbn.packageName)
        MonitoredAppConfig.markSeen(this, sbn.packageName, appName)
        val isMonitoredApp = MonitoredAppConfig.isEnabled(this, sbn.packageName)
        val isFromPaymentApp = isPaymentApp(sbn.packageName) || isMonitoredApp
        val isRawPaymentRelated = isPaymentRelated(sbn.packageName, content.rawText) ||
            (isMonitoredApp && isGenericPaymentRelated(content.rawText))
        val screenLocked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        Log.i(
            TAG,
            "onNotificationPosted packageName=${sbn.packageName}, title=${content.title}, text=${content.text}, componentName=${NotificationListenerState.componentName(this)}"
        )

        scope.launch {
            val logId = repository.addDebugNotificationLog(
                DebugNotificationLog(
                    packageName = sbn.packageName,
                    title = content.title,
                    text = content.text,
                    subText = content.subText,
                    bigText = content.bigText,
                    textLines = content.textLines,
                    rawText = content.rawText,
                    notificationKey = sbn.key,
                    postTime = sbn.postTime,
                    receivedAtMillis = System.currentTimeMillis(),
                    parseStatus = "RECEIVED",
                    failureReason = "",
                    isFromPaymentApp = isFromPaymentApp,
                    isPaymentRelated = isRawPaymentRelated,
                    serviceAlive = true,
                    appInBackground = AppVisibilityTracker.isInBackground,
                    screenLocked = screenLocked
                )
            )

            if (!isMonitoredApp) {
                repository.addBackgroundStabilityLog(
                    BackgroundEventType.IGNORED_BY_APP_FILTER,
                    sbn.packageName,
                    "appName=$appName"
                )
                repository.updateDebugNotificationParseResult(
                    id = logId,
                    parseStatus = "IGNORED_BY_APP_FILTER",
                    failureReason = "未勾选监听应用",
                    isPaymentNotification = false,
                    hasAmount = false,
                    pendingCreated = false,
                    parseReason = "packageName=${sbn.packageName}; appName=$appName; 监听应用未启用",
                    isPaymentRelated = false,
                    isParsed = false,
                    failReason = "未勾选监听应用"
                )
                Log.i(TAG, "ignoredByAppFilter package=${sbn.packageName}, appName=$appName")
                return@launch
            }

            autoBookkeepingStats.record(
                AutoBookkeepingEvent.NOTIFICATION_RECEIVED,
                sbn.packageName
            )
            val parseResult = parser.parse(
                packageName = sbn.packageName,
                title = content.title,
                text = content.rawText,
                postTimeMillis = sbn.postTime,
                allowGenericPaymentApps = !isPaymentApp(sbn.packageName),
                genericSourceApp = appName
            )
            val isParsedPaymentRelated = isRawPaymentRelated || parseResult.isPaymentNotification
            if (isParsedPaymentRelated) {
                autoBookkeepingStats.record(
                    AutoBookkeepingEvent.PAYMENT_RELATED,
                    sbn.packageName
                )
            }
            if (parseResult.isPaymentNotification && !parseResult.hasAmount) {
                autoBookkeepingStats.record(
                    AutoBookkeepingEvent.AMOUNT_PARSE_FAILED,
                    sbn.packageName
                )
            }
            if (!isPaymentApp(sbn.packageName)) {
                repository.addBackgroundStabilityLog(
                    BackgroundEventType.GENERIC_PAYMENT_PARSE_RESULT,
                    sbn.packageName,
                    "appName=$appName,parseStatus=${parseResult.parseStatus},isPayment=${parseResult.isPaymentNotification},hasAmount=${parseResult.hasAmount}"
                )
            }
            val parsed = parseResult.bill
            if (parsed == null) {
                val enrichedParseReason =
                    "packageName=${sbn.packageName}; appName=$appName; enabled=$isMonitoredApp; sourceApp=${appName}; parseResult=${parseResult.parseStatus}; ${parseResult.parseReason}"
                if (isFromPaymentApp || isRawPaymentRelated) {
                    repository.addBackgroundStabilityLog(
                        BackgroundEventType.PAYMENT_PARSE_FAIL,
                        parseResult.failureReason,
                        content.rawText.take(500)
                    )
                }
                repository.updateDebugNotificationParseResult(
                    id = logId,
                    parseStatus = parseResult.parseStatus.uppercase(),
                    failureReason = parseResult.failureReason,
                    isPaymentNotification = parseResult.isPaymentNotification,
                    hasAmount = parseResult.hasAmount,
                    pendingCreated = false,
                    parseReason = enrichedParseReason,
                    isPaymentRelated = isRawPaymentRelated || parseResult.isPaymentNotification,
                    isParsed = false,
                    failReason = parseResult.failureReason,
                    amountCandidates = parseResult.diagnostics.amountCandidatesText(),
                    selectedAmount = parseResult.diagnostics.selectedAmountText(),
                    selectedReason = parseResult.diagnostics.selectedReasonText()
                )
                Log.i(TAG, "ignored package=${sbn.packageName}, reason=${parseResult.failureReason}, rawText=${content.rawText}")
                return@launch
            }

            repository.addBackgroundStabilityLog(
                BackgroundEventType.PAYMENT_NOTIFICATION_CAPTURED,
                "${parsed.sourceApp}付款通知已捕获",
                parsed.rawText.take(500)
            )

            val insertResult = repository.addPendingFromNotification(
                amountCents = parsed.amountCents,
                type = parsed.type,
                status = parsed.status,
                sourceApp = parsed.sourceApp,
                note = parsed.note,
                rawText = parsed.rawText,
                merchantName = parsed.merchantName,
                notificationTitle = content.title,
                notificationText = content.rawText,
                notificationPackageName = sbn.packageName,
                notificationFingerprint = parsed.fingerprint,
                paidAtMillis = sbn.postTime,
                isPaymentNotification = parsed.isPaymentNotification
            )
            if (insertResult.inserted) {
                autoBookkeepingStats.record(
                    AutoBookkeepingEvent.PENDING_CREATED,
                    sbn.packageName
                )
                NotificationListenerState.markPaymentParsed(this@PaymentNotificationListenerService, sbn.packageName)
                repository.addBackgroundStabilityLog(
                    BackgroundEventType.PAYMENT_PARSE_SUCCESS,
                    "已生成待确认账单",
                    "amount=${parsed.amountCents}, type=${insertResult.finalType}, status=${parsed.status}"
                )
                repository.updateDebugNotificationParseResult(
                    id = logId,
                    parseStatus = parsed.parseStatus,
                    failureReason = "",
                    isPaymentNotification = parsed.isPaymentNotification,
                    hasAmount = parsed.hasAmount,
                    pendingCreated = true,
                    parseReason = "packageName=${sbn.packageName}; appName=$appName; enabled=$isMonitoredApp; sourceApp=${parsed.sourceApp}; parseResult=${parsed.parseStatus}; ${parsed.parseReason}",
                    ruleMatched = insertResult.ruleMatched,
                    matchedRuleName = insertResult.matchedRuleName,
                    confidence = insertResult.confidence,
                    finalCategory = insertResult.finalCategory,
                    isPaymentRelated = true,
                    isParsed = true,
                    failReason = "",
                    amountCandidates = parseResult.diagnostics.amountCandidatesText(),
                    selectedAmount = parseResult.diagnostics.selectedAmountText(),
                    selectedReason = parseResult.diagnostics.selectedReasonText()
                )
            } else {
                autoBookkeepingStats.record(
                    AutoBookkeepingEvent.DUPLICATE_FILTERED,
                    sbn.packageName
                )
                val duplicateReason = insertResult.duplicateReason.ifBlank { "被判定为重复通知" }
                repository.addBackgroundStabilityLog(
                    BackgroundEventType.PAYMENT_PARSE_FAIL,
                    duplicateReason,
                    parsed.rawText.take(500)
                )
                repository.updateDebugNotificationParseResult(
                    id = logId,
                    parseStatus = "duplicate",
                    failureReason = duplicateReason,
                    isPaymentNotification = parsed.isPaymentNotification,
                    hasAmount = parsed.hasAmount,
                    pendingCreated = false,
                    parseReason = "packageName=${sbn.packageName}; appName=$appName; enabled=$isMonitoredApp; sourceApp=${parsed.sourceApp}; parseResult=duplicate; $duplicateReason",
                    ruleMatched = insertResult.ruleMatched,
                    matchedRuleName = insertResult.matchedRuleName,
                    confidence = insertResult.confidence,
                    finalCategory = insertResult.finalCategory,
                    isPaymentRelated = true,
                    isParsed = false,
                    failReason = duplicateReason,
                    amountCandidates = parseResult.diagnostics.amountCandidatesText(),
                    selectedAmount = parseResult.diagnostics.selectedAmountText(),
                    selectedReason = parseResult.diagnostics.selectedReasonText()
                )
            }
            Log.i(TAG, "parsed package=${sbn.packageName}, inserted=${insertResult.inserted}, amount=${parsed.amountCents}, type=${insertResult.finalType}, status=${parsed.status}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        val content = collectNotificationContent(sbn.notification)
        NotificationListenerState.markNotificationRemoved(
            context = this,
            packageName = sbn.packageName,
            title = content.title,
            text = content.text.ifBlank { content.rawText }
        )
        Log.i(
            TAG,
            "onNotificationRemoved packageName=${sbn.packageName}, title=${content.title}, text=${content.text}, componentName=${NotificationListenerState.componentName(this)}"
        )
    }

    private fun collectNotificationContent(notification: Notification): NotificationContent {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() }
            .orEmpty()
        val extrasText = collectReadableExtras(extras)
        val rawText = listOf(title, text, subText, bigText, textLines, notification.tickerText?.toString().orEmpty(), extrasText)
            .flatMap { it.replace("\r", "\n").lines() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")
        return NotificationContent(
            title = title,
            text = text,
            subText = subText,
            bigText = bigText,
            textLines = textLines,
            rawText = rawText
        )
    }

    private fun collectReadableExtras(bundle: Bundle): String =
        bundle.keySet()
            .flatMap { key -> readableText(bundle.get(key)) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")

    private fun readableText(value: Any?): List<String> = when (value) {
        null -> emptyList()
        is CharSequence -> listOf(value.toString())
        is Array<*> -> value.flatMap { readableText(it) }
        is Iterable<*> -> value.flatMap { readableText(it) }
        is Bundle -> value.keySet().flatMap { readableText(value.get(it)) }
        else -> emptyList()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                NotificationListenerState.markHeartbeat(this@PaymentNotificationListenerService)
                delay(HEARTBEAT_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    companion object {
        private const val TAG = "PaymentNotificationListener"
        private const val HEARTBEAT_INTERVAL_MILLIS = 30_000L
        private val paymentPackages = setOf("com.tencent.mm", "com.eg.android.AlipayGphone")
        private val paymentKeywords = listOf(
            "支付成功",
            "付款成功",
            "交易成功",
            "已付款",
            "已支付",
            "扫码支付",
            "二维码付款",
            "向商家付款",
            "向商户付款",
            "扣款",
            "消费",
            "商户消费",
            "收款",
            "收款方",
            "商家",
            "到账",
            "转账",
            "实付",
            "消费金额",
            "交易金额",
            "微信支付",
            "支付宝"
        )

        fun isPaymentApp(packageName: String): Boolean = packageName in paymentPackages

        fun isPaymentRelated(packageName: String, rawText: String): Boolean =
            isPaymentApp(packageName) && paymentKeywords.any { rawText.contains(it) }

        fun isGenericPaymentRelated(rawText: String): Boolean =
            paymentKeywords.any { rawText.contains(it) }
    }
}

private fun NotificationParseDiagnostics.amountCandidatesText(): String =
    amountCandidates.joinToString("\n") { candidate ->
        "${candidate.text} -> ${candidate.amountCents}分 / score=${candidate.score} / ${candidate.reason}"
    }

private fun NotificationParseDiagnostics.selectedAmountText(): String =
    selectedAmount?.let { "${it.text} -> ${it.amountCents}分" }.orEmpty()

private fun NotificationParseDiagnostics.selectedReasonText(): String =
    selectedAmount?.reason.orEmpty()

private data class NotificationContent(
    val title: String,
    val text: String,
    val subText: String,
    val bigText: String,
    val textLines: String,
    val rawText: String
)

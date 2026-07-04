package com.localbookkeeping.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localbookkeeping.app.backup.LocalBackupManager
import com.localbookkeeping.app.data.ClassificationRule
import com.localbookkeeping.app.data.BackgroundEventType
import com.localbookkeeping.app.data.BackgroundDiagnosticsCalculator
import com.localbookkeeping.app.data.BackgroundStabilityLog
import com.localbookkeeping.app.data.CategoryClassifier
import com.localbookkeeping.app.data.DebugNotificationLog
import com.localbookkeeping.app.data.ExpenseCategories
import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.MerchantCategoryLearning
import com.localbookkeeping.app.data.MerchantLearningMatcher
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import com.localbookkeeping.app.limit.DailyLimitCalculator
import com.localbookkeeping.app.limit.DailyLimitConfig
import com.localbookkeeping.app.limit.DailyLimitManager
import com.localbookkeeping.app.limit.DailyLimitNotifier
import com.localbookkeeping.app.limit.DailyLimitStatus
import com.localbookkeeping.app.notification.KeepAliveNotificationService
import com.localbookkeeping.app.notification.AmountCandidate
import com.localbookkeeping.app.notification.ListenerHealthEvaluator
import com.localbookkeeping.app.notification.ListenerHealthInput
import com.localbookkeeping.app.notification.ListenerRecoveryManager
import com.localbookkeeping.app.notification.ListenerRecoveryState
import com.localbookkeeping.app.notification.RebindAttemptStatus
import com.localbookkeeping.app.notification.ListenerServiceStatus
import com.localbookkeeping.app.notification.NotificationBillParser
import com.localbookkeeping.app.notification.NotificationChannels
import com.localbookkeeping.app.notification.NotificationListenerState
import com.localbookkeeping.app.notification.TestPaymentNotificationSender
import com.localbookkeeping.app.screenshot.ScreenshotParseResult
import com.localbookkeeping.app.stats.BillGroupType
import com.localbookkeeping.app.stats.BillStatistics
import com.localbookkeeping.app.stats.BillStatisticsCalculator
import com.localbookkeeping.app.stats.StatsRangeType
import com.localbookkeeping.app.stats.TimeRange
import com.localbookkeeping.app.ui.BookkeepingUiState
import com.localbookkeeping.app.ui.BookkeepingViewModel
import com.localbookkeeping.app.ui.BookkeepingViewModelFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.ensureAll(this)
        val app = application as BookkeepingApplication
        val initialScreen = when (intent?.action) {
            KeepAliveNotificationService.ACTION_OPEN_BACKGROUND_SETTINGS -> AppScreen.BACKGROUND_SETTINGS
            KeepAliveNotificationService.ACTION_OPEN_QUICK_BACKFILL -> AppScreen.QUICK_BACKFILL
            else -> AppScreen.LEDGER
        }
        setContent { BookkeepingApp(app, initialScreen) }
    }
}

@Composable
private fun BookkeepingApp(app: BookkeepingApplication, initialScreen: AppScreen = AppScreen.LEDGER) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: BookkeepingViewModel = viewModel(
        factory = BookkeepingViewModelFactory(app.repository)
    )
    val uiState by viewModel.uiState.collectAsState()
    var screen by remember { mutableStateOf(initialScreen) }
    var selectedMainTab by remember {
        mutableStateOf(if (initialScreen == AppScreen.LEDGER) MainTab.BOOKKEEPING else MainTab.LISTENER)
    }
    var editingPending by remember { mutableStateOf<ExpenseRecord?>(null) }
    var selectedConfirmedRecord by remember { mutableStateOf<ExpenseRecord?>(null) }
    var editingConfirmedRecord by remember { mutableStateOf<ExpenseRecord?>(null) }
    var selectedNotificationLog by remember { mutableStateOf<DebugNotificationLog?>(null) }
    var notificationDetailReturnScreen by remember { mutableStateOf(AppScreen.REAL_NOTIFICATION_TEST) }
    var notificationAccessEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var autoListeningEnabled by remember { mutableStateOf(KeepAliveNotificationService.isEnabled(context)) }
    var autoListenServiceMissing by remember {
        mutableStateOf(KeepAliveNotificationService.isEnabled(context) && !KeepAliveNotificationService.isRunning(context))
    }
    var showAutoListenConfirmDialog by remember { mutableStateOf(false) }
    var showAutoListenStartupDialog by remember { mutableStateOf(false) }
    var showAutoListenRestoreDialog by remember { mutableStateOf(false) }
    var startupAutoListenPromptChecked by remember { mutableStateOf(false) }
    var healthRefreshTick by remember { mutableStateOf(System.currentTimeMillis()) }
    var rebindMessage by remember { mutableStateOf("") }
    var testNotificationMessage by remember { mutableStateOf("") }
    var listenerRescueState by remember { mutableStateOf(ListenerRescueUiState()) }
    var screenshotPreview by remember { mutableStateOf<ScreenshotPreviewState?>(null) }
    var screenshotOcrBusy by remember { mutableStateOf(false) }
    var screenshotOcrError by remember { mutableStateOf("") }
    var dailyLimitConfig by remember { mutableStateOf(DailyLimitManager.load(context)) }
    var showDailyLimitAlert by remember { mutableStateOf(false) }
    var lastAlertedExpenseSignature by remember { mutableStateOf("") }
    var suppressInitialLimitAlert by remember { mutableStateOf(true) }
    var quickBackfillReturnScreen by remember { mutableStateOf(AppScreen.LEDGER) }
    var quickBackfillPreset by remember {
        mutableStateOf(
            if (initialScreen == AppScreen.QUICK_BACKFILL) wechatScanBackfillPreset() else defaultQuickBackfillPreset()
        )
    }
    var backupBusy by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf("") }
    var pendingBackupExportText by remember { mutableStateOf("") }
    var pendingBackupExportType by remember { mutableStateOf("") }
    var lastBackupAt by remember { mutableStateOf(LocalBackupManager.lastBackupAt(context)) }
    val bookkeepingListState = rememberLazyListState()
    val dailyLimitListState = rememberLazyListState()
    val listenerListState = rememberLazyListState()
    val dailyLimitStatus = DailyLimitCalculator.statusForToday(uiState.confirmedRecords, dailyLimitConfig)

    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            screenshotOcrBusy = true
            screenshotOcrError = ""
            val imageUri = uri.toString()
            val rawTextResult = runCatching { recognizeTextFromImage(context, uri) }
            val rawText = rawTextResult.getOrDefault("")
            val parseResult = if (rawTextResult.isSuccess) {
                viewModel.parseScreenshotText(rawText, imageUri)
            } else {
                ScreenshotParseResult(failureReason = rawTextResult.exceptionOrNull()?.message ?: "OCR 识别失败")
            }
            val pendingId = parseResult.bill?.let { parsed ->
                viewModel.createPendingFromScreenshot(imageUri, parsed)
            }
            viewModel.addScreenshotDebugLog(
                imageUri = imageUri,
                rawText = rawText,
                parseResult = parseResult,
                pendingCreated = pendingId != null
            )
            screenshotPreview = ScreenshotPreviewState(
                imageUri = imageUri,
                rawText = rawText,
                parseResult = parseResult,
                pendingRecordId = pendingId,
                message = when {
                    rawTextResult.isFailure -> "OCR 识别失败，请重新选择截图"
                    parseResult.bill == null -> "未能从截图中识别账单，请检查金额、商户和时间"
                    pendingId == null -> "截图账单已识别，但未能创建待确认账单"
                    else -> parseResult.parseReason
                }
            )
            screenshotOcrBusy = false
            screen = AppScreen.SCREENSHOT_PREVIEW
        }
    }

    val jsonBackupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            backupMessage = "已取消导出"
            return@rememberLauncherForActivityResult
        }
        val text = pendingBackupExportText
        val exportType = pendingBackupExportType
        coroutineScope.launch {
            backupBusy = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    LocalBackupManager.writeText(context, uri, text)
                    LocalBackupManager.markBackupCompleted(context)
                }
            }
            backupBusy = false
            if (result.isSuccess) {
                lastBackupAt = LocalBackupManager.lastBackupAt(context)
                backupMessage = "${exportType.ifBlank { "JSON" }} 备份成功"
            } else {
                backupMessage = "导出失败：${result.exceptionOrNull()?.message.orEmpty()}"
            }
        }
    }

    val csvBackupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) {
            backupMessage = "已取消导出"
            return@rememberLauncherForActivityResult
        }
        val text = pendingBackupExportText
        coroutineScope.launch {
            backupBusy = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    LocalBackupManager.writeText(context, uri, text)
                    LocalBackupManager.markBackupCompleted(context)
                }
            }
            backupBusy = false
            if (result.isSuccess) {
                lastBackupAt = LocalBackupManager.lastBackupAt(context)
                backupMessage = "CSV 备份成功"
            } else {
                backupMessage = "导出失败：${result.exceptionOrNull()?.message.orEmpty()}"
            }
        }
    }

    val jsonBackupOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            backupMessage = "已取消恢复"
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            backupBusy = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = LocalBackupManager.readText(context, uri)
                    val records = LocalBackupManager.parseJsonBackup(text)
                    app.repository.restoreRecordsFromBackup(records)
                }
            }
            backupBusy = false
            backupMessage = result.fold(
                onSuccess = {
                    "恢复完成：总数 ${it.totalCount}，导入数量 ${it.importedCount}，跳过重复 ${it.skippedCount}"
                },
                onFailure = { "恢复失败：${it.message.orEmpty()}" }
            )
        }
    }

    fun exportJsonBackup() {
        if (backupBusy) return
        coroutineScope.launch {
            backupBusy = true
            val now = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    LocalBackupManager.buildJsonBackup(app.repository.getRecordsForBackup(), now)
                }
            }
            backupBusy = false
            result.fold(
                onSuccess = { text ->
                    pendingBackupExportText = text
                    pendingBackupExportType = "JSON"
                    jsonBackupCreateLauncher.launch(LocalBackupManager.defaultJsonFileName(now))
                },
                onFailure = { backupMessage = "导出 JSON 失败：${it.message.orEmpty()}" }
            )
        }
    }

    fun exportCsvBackup() {
        if (backupBusy) return
        coroutineScope.launch {
            backupBusy = true
            val now = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    LocalBackupManager.buildCsv(app.repository.getRecordsForBackup())
                }
            }
            backupBusy = false
            result.fold(
                onSuccess = { text ->
                    pendingBackupExportText = text
                    pendingBackupExportType = "CSV"
                    csvBackupCreateLauncher.launch(LocalBackupManager.defaultCsvFileName(now))
                },
                onFailure = { backupMessage = "导出 CSV 失败：${it.message.orEmpty()}" }
            )
        }
    }

    fun importJsonBackup() {
        if (!backupBusy) {
            jsonBackupOpenLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream", "*/*"))
        }
    }

    fun sendTestNotificationAndVerify(onResult: ((Boolean, String) -> Unit)? = null) {
        val startedAtMillis = System.currentTimeMillis()
        val beforeState = NotificationListenerState.current(context)
        Log.i(
            NOTIFICATION_LOG_TAG,
            "sendTestNotification start componentName=${NotificationListenerState.componentName(context)}, lastNotificationTime=${beforeState.lastNotificationTime}"
        )
        val sent = TestPaymentNotificationSender.send(context)
        if (!sent) {
            testNotificationMessage = "已发送 App 测试通知，请查看通知日志是否收到"
            onResult?.invoke(false, testNotificationMessage)
            return
        }
        testNotificationMessage = "正在发送测试通知..."
        coroutineScope.launch {
            delay(3_000)
            val afterState = NotificationListenerState.current(context)
            val newLogCount = withContext(Dispatchers.IO) {
                app.repository.countDebugNotificationLogsSince(startedAtMillis)
            }
            val postedCallbackTriggered = afterState.lastNotificationTime > beforeState.lastNotificationTime &&
                afterState.lastNotificationTime >= startedAtMillis
            val debugLogAdded = newLogCount > 0
            Log.i(
                NOTIFICATION_LOG_TAG,
                "sendTestNotification result postedCallbackTriggered=$postedCallbackTriggered, debugLogAdded=$debugLogAdded, newLogCount=$newLogCount"
            )
            testNotificationMessage = if (postedCallbackTriggered && debugLogAdded) {
                "测试通知已发送，但日志中暂未看到本 App 通知记录"
            } else {
                "测试通知发送失败，或通知监听未捕获到本 App 通知，请检查通知权限和监听状态"
            }
            onResult?.invoke(postedCallbackTriggered && debugLogAdded, testNotificationMessage)
            healthRefreshTick = System.currentTimeMillis()
        }
    }

    fun runListenerRescue() {
        if (listenerRescueState.running) return
        listenerRescueState = ListenerRescueUiState(running = true, finalConclusion = "正在检查...")
        coroutineScope.launch {
            app.repository.addBackgroundStabilityLog(
                BackgroundEventType.LISTENER_RESCUE,
                "listenerRescueStart"
            )
            val permissionGranted = NotificationListenerState.isPermissionEnabled(context)
            listenerRescueState = listenerRescueState.copy(
                permissionResult = if (permissionGranted) "已授权" else "未授权"
            )
            if (!permissionGranted) {
                listenerRescueState = listenerRescueState.copy(
                    running = false,
                    foregroundServiceResult = if (KeepAliveNotificationService.isRunning(context)) "运行中" else "未运行",
                    listenerConnectionResult = "未检查",
                    probeResult = "已跳过",
                    rebindResult = "未执行",
                    finalConclusion = "请先开启通知监听权限"
                )
                app.repository.addBackgroundStabilityLog(
                    BackgroundEventType.LISTENER_RESCUE,
                    "listenerRescuePermissionMissing"
                )
                healthRefreshTick = System.currentTimeMillis()
                return@launch
            }

            val autoEnabled = KeepAliveNotificationService.isEnabled(context)
            var foregroundRunning = KeepAliveNotificationService.isRunning(context)
            if (autoEnabled && !foregroundRunning) {
                app.repository.addBackgroundStabilityLog(
                    BackgroundEventType.FOREGROUND_SERVICE_MISSING,
                    "foregroundServiceMissing",
                    "source=listenerRescue"
                )
                KeepAliveNotificationService.start(context)
                app.repository.addBackgroundStabilityLog(
                    BackgroundEventType.AUTO_LISTEN_RESTORE,
                    "autoListenRestore",
                    "source=listenerRescue"
                )
                delay(1_000)
                foregroundRunning = KeepAliveNotificationService.isRunning(context)
            }
            listenerRescueState = listenerRescueState.copy(
                foregroundServiceResult = if (foregroundRunning) "运行中" else "未运行"
            )

            val beforeProbe = NotificationListenerState.current(context)
            listenerRescueState = listenerRescueState.copy(
                listenerConnectionResult = when {
                    beforeProbe.listenerConnected -> "已连接"
                    beforeProbe.rawListenerConnected -> "系统已连接"
                    else -> "未连接"
                }
            )
            val firstProbe = ListenerRecoveryManager.runProbeCheck(
                context,
                app.repository,
                "listenerRescueInitialProbe"
            )
            listenerRescueState = listenerRescueState.copy(
                probeResult = when (firstProbe) {
                    true -> "成功"
                    false -> "失败"
                    null -> "失败：请开启 App 通知权限"
                }
            )
            if (firstProbe == true) {
                ListenerRecoveryState.clearListenerRecheckNeeded(context)
                listenerRescueState = listenerRescueState.copy(
                    running = false,
                    rebindResult = "无需重绑",
                    listenerConnectionResult = "已连接",
                    finalConclusion = "监听恢复正常"
                )
                app.repository.addBackgroundStabilityLog(
                    BackgroundEventType.LISTENER_RESCUE,
                    "listenerRescueSuccessBeforeRebind"
                )
                healthRefreshTick = System.currentTimeMillis()
                return@launch
            }

            val rebind = ListenerRecoveryManager.requestRebindNow(
                context = context,
                repository = app.repository,
                source = "listenerRescueAfterProbeFail",
                afterProbeFail = true
            )
            listenerRescueState = listenerRescueState.copy(
                rebindResult = when (rebind.status) {
                    RebindAttemptStatus.ATTEMPTED -> "已尝试重绑"
                    RebindAttemptStatus.COOLDOWN -> "冷却中"
                    RebindAttemptStatus.PERMISSION_MISSING -> "权限缺失，无法重绑"
                    RebindAttemptStatus.UNSUPPORTED -> "系统不支持自动重绑"
                }
            )
            val secondProbe = if (rebind.status == RebindAttemptStatus.ATTEMPTED) {
                ListenerRecoveryManager.runProbeCheck(
                    context,
                    app.repository,
                    "listenerRescueAfterRebind"
                )
            } else {
                false
            }
            if (secondProbe == true) {
                ListenerRecoveryState.clearListenerRecheckNeeded(context)
            }
            listenerRescueState = listenerRescueState.copy(
                running = false,
                probeResult = if (secondProbe == true) "成功，已恢复" else listenerRescueState.probeResult,
                listenerConnectionResult = if (secondProbe == true) "已连接" else listenerRescueState.listenerConnectionResult,
                finalConclusion = when {
                    secondProbe == true -> "监听恢复正常"
                    firstProbe == null -> "请先开启 App 通知权限"
                    else -> "监听仍未恢复，请检查通知监听权限并等待系统重连"
                }
            )
            app.repository.addBackgroundStabilityLog(
                BackgroundEventType.LISTENER_RESCUE,
                "listenerRescueComplete",
                listenerRescueState.finalConclusion
            )
            healthRefreshTick = System.currentTimeMillis()
        }
    }

    fun restoreAutoListen(source: String) {
        KeepAliveNotificationService.start(context)
        KeepAliveNotificationService.markBootRestorePending(context, false)
        viewModel.addBackgroundEvent(BackgroundEventType.AUTO_LISTEN_RESTORE, "恢复自动监听：$source")
        autoListeningEnabled = true
        autoListenServiceMissing = false
        healthRefreshTick = System.currentTimeMillis()
        coroutineScope.launch(Dispatchers.IO) {
            delay(1_000)
            ListenerRecoveryManager.checkNow(
                context = context,
                repository = app.repository,
                source = source,
                probeWhenHealthy = true
            )
            val missing = KeepAliveNotificationService.isEnabled(context) && !KeepAliveNotificationService.isRunning(context)
            withContext(Dispatchers.Main) {
                autoListeningEnabled = KeepAliveNotificationService.isEnabled(context)
                autoListenServiceMissing = missing
                healthRefreshTick = System.currentTimeMillis()
            }
        }
    }

    fun openDefaultQuickBackfill(returnScreen: AppScreen) {
        quickBackfillPreset = defaultQuickBackfillPreset()
        quickBackfillReturnScreen = returnScreen
        screen = AppScreen.QUICK_BACKFILL
    }

    fun openWechatScanBackfill(returnScreen: AppScreen) {
        quickBackfillPreset = wechatScanBackfillPreset()
        quickBackfillReturnScreen = returnScreen
        screen = AppScreen.QUICK_BACKFILL
    }

    fun navigateBack() {
        when (screen) {
            AppScreen.LEDGER -> {
                if (selectedMainTab != MainTab.BOOKKEEPING) {
                    selectedMainTab = MainTab.BOOKKEEPING
                }
            }
            AppScreen.EDIT_PENDING -> screen = AppScreen.PENDING
            AppScreen.EDIT_CONFIRMED -> screen = AppScreen.RECORD_DETAIL
            AppScreen.NOTIFICATION_RAW_DETAIL -> screen = notificationDetailReturnScreen
            AppScreen.QUICK_BACKFILL -> screen = quickBackfillReturnScreen
            else -> screen = AppScreen.LEDGER
        }
    }

    BackHandler(enabled = screen != AppScreen.LEDGER || selectedMainTab != MainTab.BOOKKEEPING) {
        navigateBack()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sendTestNotificationAndVerify()
        } else {
            testNotificationMessage = "未收到 App 测试通知，请检查通知权限和通知监听设置"
        }
    }

    LaunchedEffect(Unit) {
        if (!startupAutoListenPromptChecked) {
            startupAutoListenPromptChecked = true
            val autoEnabled = KeepAliveNotificationService.isEnabled(context)
            val foregroundRunning = KeepAliveNotificationService.isRunning(context)
            autoListeningEnabled = autoEnabled
            autoListenServiceMissing = autoEnabled && !foregroundRunning
            if (autoEnabled && !foregroundRunning) {
                app.repository.addBackgroundStabilityLog(
                    BackgroundEventType.FOREGROUND_SERVICE_MISSING,
                    "foregroundServiceMissing",
                    "source=appStartup"
                )
                showAutoListenRestoreDialog = true
            } else if (!autoEnabled) {
                showAutoListenStartupDialog = true
            }
        }
    }

    LaunchedEffect(uiState.confirmedRecords, dailyLimitConfig) {
        val latestTodayExpense = latestTodayExpenseRecord(uiState.confirmedRecords)
        val signature = latestTodayExpense?.let { "${it.id}-${it.updatedAtMillis}-${dailyLimitStatus.spentCents}" }.orEmpty()
        if (suppressInitialLimitAlert) {
            suppressInitialLimitAlert = false
            lastAlertedExpenseSignature = signature
            return@LaunchedEffect
        }
        if (dailyLimitStatus.shouldAlert && signature.isNotBlank() && signature != lastAlertedExpenseSignature) {
            lastAlertedExpenseSignature = signature
            DailyLimitNotifier.notifyExceeded(context, dailyLimitStatus.limitCents)
            showDailyLimitAlert = true
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessEnabled = isNotificationListenerEnabled(context)
                autoListeningEnabled = KeepAliveNotificationService.isEnabled(context)
                autoListenServiceMissing = autoListeningEnabled && !KeepAliveNotificationService.isRunning(context)
                healthRefreshTick = System.currentTimeMillis()
                coroutineScope.launch(Dispatchers.IO) {
                    val autoEnabled = KeepAliveNotificationService.isEnabled(context)
                    val foregroundRunning = KeepAliveNotificationService.isRunning(context)
                    if (autoEnabled && !foregroundRunning) {
                        app.repository.addBackgroundStabilityLog(
                            BackgroundEventType.FOREGROUND_SERVICE_MISSING,
                            "foregroundServiceMissing",
                            "source=appResume"
                        )
                    }
                    ListenerRecoveryManager.checkNow(
                        context = context,
                        repository = app.repository,
                        source = "appResume",
                        probeWhenHealthy = autoEnabled && foregroundRunning
                    )
                    val missing = KeepAliveNotificationService.isEnabled(context) && !KeepAliveNotificationService.isRunning(context)
                    withContext(Dispatchers.Main) {
                        autoListeningEnabled = KeepAliveNotificationService.isEnabled(context)
                        autoListenServiceMissing = missing
                        healthRefreshTick = System.currentTimeMillis()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Green,
            background = PageBackground,
            surface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
            when (screen) {
                AppScreen.LEDGER -> MainTabsScreen(
                    uiState = uiState,
                    selectedTab = selectedMainTab,
                    bookkeepingListState = bookkeepingListState,
                    dailyLimitListState = dailyLimitListState,
                    listenerListState = listenerListState,
                    onSelectedTabChange = { selectedMainTab = it },
                    notificationAccessEnabled = notificationAccessEnabled,
                    autoListeningEnabled = autoListeningEnabled,
                    autoListenServiceMissing = autoListenServiceMissing,
                    dailyLimitStatus = dailyLimitStatus,
                    healthRefreshTick = healthRefreshTick,
                    listenerRescueState = listenerRescueState,
                    screenshotOcrBusy = screenshotOcrBusy,
                    screenshotOcrError = screenshotOcrError,
                    onAdd = { screen = AppScreen.ADD },
                    onOpenPending = { screen = AppScreen.PENDING },
                    onOpenDebugLogs = { screen = AppScreen.DEBUG_LOGS },
                    onOpenRules = { screen = AppScreen.RULES },
                    onOpenBackup = { screen = AppScreen.BACKUP },
                    onOpenHealth = { screen = AppScreen.HEALTH },
                    onOpenStats = {
                        selectedMainTab = MainTab.STATS
                        screen = AppScreen.LEDGER
                    },
                    onOpenBackgroundSettings = { screen = AppScreen.BACKGROUND_SETTINGS },
                    onOpenBackgroundReport = { screen = AppScreen.BACKGROUND_REPORT },
                    onEnableAutoListen = { showAutoListenConfirmDialog = true },
                    onDisableAutoListen = {
                        KeepAliveNotificationService.stop(context)
                        viewModel.recordAutoListenChanged(false)
                        autoListeningEnabled = false
                        autoListenServiceMissing = false
                        healthRefreshTick = System.currentTimeMillis()
                        coroutineScope.launch {
                            delay(1_000)
                            healthRefreshTick = System.currentTimeMillis()
                        }
                    },
                    onRestoreAutoListen = {
                        restoreAutoListen("manualAutoListenRestore")
                    },
                    onOpenRealNotificationTest = { screen = AppScreen.REAL_NOTIFICATION_TEST },
                    onOpenWechatScanTest = { screen = AppScreen.WECHAT_SCAN_TEST },
                    onOpenTroubleshooting = { screen = AppScreen.TROUBLESHOOTING },
                    onOpenQuickBackfill = { openDefaultQuickBackfill(AppScreen.LEDGER) },
                    onOpenWechatScanBackfill = { openWechatScanBackfill(AppScreen.LEDGER) },
                    onOpenLearning = { screen = AppScreen.LEARNING },
                    onOpenScreenshotPicker = {
                        screenshotPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onGoToListenerTab = { selectedMainTab = MainTab.LISTENER },
                    onOpenRecordDetail = {
                        selectedConfirmedRecord = it
                        screen = AppScreen.RECORD_DETAIL
                    },
                    onSaveDailyLimit = { enabled, limitCents ->
                        DailyLimitManager.save(context, enabled, limitCents)
                        dailyLimitConfig = DailyLimitManager.load(context)
                        suppressInitialLimitAlert = true
                    },
                    onToggleDailyLimit = { enabled ->
                        DailyLimitManager.setEnabled(context, enabled)
                        dailyLimitConfig = DailyLimitManager.load(context)
                        suppressInitialLimitAlert = true
                    },
                    onOneClickRepair = {
                        viewModel.addBackgroundEvent(BackgroundEventType.REQUEST_REBIND, "一键修复 requestRebind")
                        val called = NotificationListenerState.requestRebind(context)
                        rebindMessage = if (called) "requestRebind called" else "requestRebind unavailable"
                        notificationAccessEnabled = isNotificationListenerEnabled(context)
                        healthRefreshTick = System.currentTimeMillis()
                        coroutineScope.launch {
                            delay(3_000)
                            val connected = NotificationListenerState.isConnected(context)
                            viewModel.addBackgroundEvent(
                                BackgroundEventType.REQUEST_REBIND_RESULT,
                                if (connected) "requestRebindConnected" else "requestRebindNotConnected"
                            )
                            rebindMessage = if (connected) {
                                "监听已重新连接"
                            } else {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                "已打开通知监听权限设置，请关闭再重新开启本地自动记账"
                            }
                            healthRefreshTick = System.currentTimeMillis()
                        }
                    },
                    onListenerRescue = ::runListenerRescue,
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )

                AppScreen.ADD -> RecordFormScreen(
                    title = "记账",
                    initialRecord = null,
                    merchantLearnings = uiState.merchantCategoryLearnings,
                    onBack = ::navigateBack,
                    onSave = { amountCents, type, category, merchantName, note, paidAtMillis ->
                        viewModel.addRecord(amountCents, type, category, note, paidAtMillis, merchantName)
                        screen = AppScreen.LEDGER
                    }
                )

                AppScreen.PENDING -> PendingBillsScreen(
                    pendingRecords = uiState.pendingRecords,
                    onBack = ::navigateBack,
                    onConfirm = viewModel::confirmPending,
                    onEdit = {
                        editingPending = it
                        screen = AppScreen.EDIT_PENDING
                    },
                    onIgnore = viewModel::ignorePending
                )

                AppScreen.EDIT_PENDING -> {
                    val record = editingPending
                    if (record == null) {
                        LaunchedEffect(Unit) { screen = AppScreen.PENDING }
                    } else {
                        RecordFormScreen(
                            title = "编辑待确认账单",
                            initialRecord = record,
                            merchantLearnings = uiState.merchantCategoryLearnings,
                            onBack = ::navigateBack,
                            onSave = { amountCents, type, category, merchantName, note, paidAtMillis ->
                                viewModel.updatePendingAndConfirm(
                                    id = record.id,
                                    amountCents = amountCents,
                                    type = type,
                                    category = category,
                                    merchantName = merchantName,
                                    note = note,
                                    paidAtMillis = paidAtMillis
                                )
                                editingPending = null
                                screen = AppScreen.PENDING
                            }
                        )
                    }
                }

                AppScreen.RECORD_DETAIL -> {
                    val record = selectedConfirmedRecord?.let { selected ->
                        uiState.confirmedRecords.firstOrNull { it.id == selected.id } ?: selected
                    }
                    if (record == null) {
                        LaunchedEffect(Unit) { screen = AppScreen.LEDGER }
                    } else {
                        RecordDetailScreen(
                            record = record,
                            onBack = ::navigateBack,
                            onEdit = {
                                editingConfirmedRecord = record
                                screen = AppScreen.EDIT_CONFIRMED
                            },
                            onDelete = {
                                viewModel.softDeleteRecord(record.id)
                                selectedConfirmedRecord = null
                                screen = AppScreen.LEDGER
                            }
                        )
                    }
                }

                AppScreen.EDIT_CONFIRMED -> {
                    val record = editingConfirmedRecord
                    if (record == null) {
                        LaunchedEffect(Unit) { screen = AppScreen.LEDGER }
                    } else {
                        RecordFormScreen(
                            title = "编辑账单",
                            initialRecord = record,
                            merchantLearnings = uiState.merchantCategoryLearnings,
                            onBack = ::navigateBack,
                            onSave = { amountCents, type, category, merchantName, note, paidAtMillis ->
                                viewModel.updateConfirmedRecord(
                                    id = record.id,
                                    amountCents = amountCents,
                                    type = type,
                                    category = category,
                                    merchantName = merchantName,
                                    note = note,
                                    paidAtMillis = paidAtMillis
                                )
                                editingConfirmedRecord = null
                                selectedConfirmedRecord = record.copy(
                                    amountCents = amountCents,
                                    type = type,
                                    category = category,
                                    merchantName = merchantName,
                                    note = note,
                                    paidAtMillis = paidAtMillis,
                                    categorySource = "manual",
                                    matchedRuleName = "",
                                    matchedKeyword = ""
                                )
                                screen = AppScreen.LEDGER
                            }
                        )
                    }
                }

                AppScreen.DEBUG_LOGS -> NotificationDebugLogsScreen(
                    logs = uiState.debugNotificationLogs,
                    onBack = ::navigateBack,
                    testNotificationMessage = testNotificationMessage,
                    onSimulate = viewModel::simulatePaymentNotificationParse,
                    onSendTestNotification = {
                        testNotificationMessage = ""
                        if (!TestPaymentNotificationSender.canPostNotifications(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            sendTestNotificationAndVerify()
                        }
                    }
                )

                AppScreen.REAL_NOTIFICATION_TEST -> RealNotificationTestScreen(
                    logs = uiState.debugNotificationLogs,
                    testNotificationMessage = testNotificationMessage,
                    onBack = ::navigateBack,
                    onOpenLogDetail = { log ->
                        selectedNotificationLog = log
                        notificationDetailReturnScreen = AppScreen.REAL_NOTIFICATION_TEST
                        screen = AppScreen.NOTIFICATION_RAW_DETAIL
                    },
                    onSendAppTestNotification = { onResult ->
                        testNotificationMessage = ""
                        if (!TestPaymentNotificationSender.canPostNotifications(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            onResult(false, "没有通知权限，无法发送 App 测试通知")
                        } else {
                            sendTestNotificationAndVerify(onResult)
                        }
                    }
                )

                AppScreen.WECHAT_SCAN_TEST -> WechatScanPaymentTestScreen(
                    logs = uiState.debugNotificationLogs,
                    onBack = ::navigateBack,
                    onOpenLogDetail = { log ->
                        selectedNotificationLog = log
                        notificationDetailReturnScreen = AppScreen.WECHAT_SCAN_TEST
                        screen = AppScreen.NOTIFICATION_RAW_DETAIL
                    },
                    onOpenQuickBackfill = { openWechatScanBackfill(AppScreen.WECHAT_SCAN_TEST) }
                )

                AppScreen.HEALTH -> ListenerHealthScreen(
                    health = buildListenerHealthStatus(context, uiState.debugNotificationLogs, healthRefreshTick),
                    autoListeningEnabled = autoListeningEnabled,
                    rebindMessage = rebindMessage,
                    onBack = ::navigateBack,
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onRequestRebind = {
                        viewModel.addBackgroundEvent(BackgroundEventType.REQUEST_REBIND, "手动重新绑定监听")
                        val called = NotificationListenerState.requestRebind(context)
                        rebindMessage = if (called) {
                            "已请求系统重新绑定通知监听，请稍后查看状态"
                        } else {
                            "当前系统不支持 requestRebind，请手动重新授权通知监听"
                        }
                        notificationAccessEnabled = isNotificationListenerEnabled(context)
                        healthRefreshTick = System.currentTimeMillis()
                        coroutineScope.launch {
                            delay(3_000)
                            val connected = NotificationListenerState.isConnected(context)
                            rebindMessage = if (connected) {
                                viewModel.addBackgroundEvent(BackgroundEventType.REQUEST_REBIND_RESULT, "监听重新连接成功")
                                "监听已重新连接"
                            } else {
                                viewModel.addBackgroundEvent(BackgroundEventType.REQUEST_REBIND_RESULT, "监听重新连接失败")
                                "重新连接失败，请关闭再重新开启通知监听权限"
                            }
                            healthRefreshTick = System.currentTimeMillis()
                        }
                    },
                    onOneClickRepair = {
                        viewModel.addBackgroundEvent(BackgroundEventType.REQUEST_REBIND, "一键修复 requestRebind")
                        val called = NotificationListenerState.requestRebind(context)
                        rebindMessage = if (called) "requestRebind called" else "requestRebind unavailable"
                        notificationAccessEnabled = isNotificationListenerEnabled(context)
                        healthRefreshTick = System.currentTimeMillis()
                        coroutineScope.launch {
                            delay(3_000)
                            val connected = NotificationListenerState.isConnected(context)
                            viewModel.addBackgroundEvent(
                                BackgroundEventType.REQUEST_REBIND_RESULT,
                                if (connected) "requestRebindConnected" else "requestRebindNotConnected"
                            )
                            rebindMessage = if (connected) {
                                "监听已重新连接"
                            } else {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                "已打开通知监听权限设置，请关闭再重新开启本地自动记账"
                            }
                            healthRefreshTick = System.currentTimeMillis()
                        }
                    },
                    onReauthorizeNotificationListener = {
                        rebindMessage = "请在系统设置中关闭再重新开启本地自动记账的通知监听权限"
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onOpenBatterySettings = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                    onToggleAutoListening = { enabled ->
                        if (enabled) {
                            showAutoListenConfirmDialog = true
                        } else {
                            KeepAliveNotificationService.stop(context)
                            viewModel.recordAutoListenChanged(false)
                            autoListeningEnabled = false
                            healthRefreshTick = System.currentTimeMillis()
                        }
                    }
                )

                AppScreen.STATS -> StatisticsScreen(
                    records = uiState.confirmedRecords,
                    onBack = ::navigateBack
                )

                AppScreen.LEARNING -> LearningRecordsScreen(
                    learnings = uiState.merchantCategoryLearnings,
                    onBack = ::navigateBack,
                    onToggleLearning = viewModel::setMerchantLearningEnabled
                )

                AppScreen.BACKUP -> BackupScreen(
                    lastBackupAt = lastBackupAt,
                    message = backupMessage,
                    busy = backupBusy,
                    onBack = ::navigateBack,
                    onExportCsv = ::exportCsvBackup,
                    onExportJson = ::exportJsonBackup,
                    onImportJson = ::importJsonBackup
                )

                AppScreen.BACKGROUND_SETTINGS -> BackgroundSettingsScreen(
                    health = buildListenerHealthStatus(context, uiState.debugNotificationLogs, healthRefreshTick),
                    autoListeningEnabled = autoListeningEnabled,
                    onBack = ::navigateBack,
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onOpenBatterySettings = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                    onOpenAppSettings = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    },
                    onToggleAutoListening = { enabled ->
                        if (enabled) {
                            showAutoListenConfirmDialog = true
                        } else {
                            KeepAliveNotificationService.stop(context)
                            viewModel.recordAutoListenChanged(false)
                            autoListeningEnabled = false
                            healthRefreshTick = System.currentTimeMillis()
                        }
                    }
                )

                AppScreen.BACKGROUND_REPORT -> BackgroundReportScreen(
                    logs = uiState.backgroundStabilityLogs,
                    notificationLogs = uiState.debugNotificationLogs,
                    records = uiState.confirmedRecords,
                    onBack = ::navigateBack
                )

                AppScreen.TROUBLESHOOTING -> PaymentTroubleshootingScreen(
                    health = buildListenerHealthStatus(context, uiState.debugNotificationLogs, healthRefreshTick),
                    onBack = ::navigateBack,
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onOpenBatterySettings = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                    onOpenScreenshotPicker = {
                        screenshotPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenQuickBackfill = {
                        quickBackfillReturnScreen = AppScreen.TROUBLESHOOTING
                        screen = AppScreen.QUICK_BACKFILL
                    }
                )

                AppScreen.QUICK_BACKFILL -> QuickBackfillScreen(
                    preset = quickBackfillPreset,
                    onBack = ::navigateBack,
                    onSave = { amountCents, type, category, merchantName, sourceApp, paidAtMillis, userNote ->
                        val note = userNote.ifBlank {
                            presetNote(
                                preset = quickBackfillPreset,
                                sourceApp = sourceApp,
                                merchantName = merchantName
                            )
                        }.take(80)
                        viewModel.addRecord(
                            amountCents = amountCents,
                            type = type,
                            category = category,
                            note = note,
                            paidAtMillis = paidAtMillis,
                            merchantName = merchantName,
                            sourceApp = sourceApp
                        )
                        screen = AppScreen.LEDGER
                    }
                )

                AppScreen.RULES -> ClassificationRulesScreen(
                    rules = uiState.classificationRules,
                    onBack = ::navigateBack,
                    onAddRule = { ruleName, keyword, category, type, priority ->
                        viewModel.addClassificationRule(ruleName, keyword, category, type, priority)
                    },
                    onUpdateRule = { rule, ruleName, keyword, category, type, priority ->
                        viewModel.updateClassificationRule(rule, ruleName, keyword, category, type, priority)
                    },
                    onToggleRule = viewModel::setClassificationRuleEnabled,
                    onDeleteRule = viewModel::deleteClassificationRule
                )

                AppScreen.SCREENSHOT_PREVIEW -> {
                    val preview = screenshotPreview
                    if (preview == null) {
                        LaunchedEffect(Unit) { screen = AppScreen.LEDGER }
                    } else {
                        ScreenshotPreviewScreen(
                            preview = preview,
                            onBack = ::navigateBack,
                            onConfirmExistingPending = { id, amountCents, type, category, merchantName, note, paidAtMillis ->
                                viewModel.updatePendingAndConfirm(id, amountCents, type, category, merchantName, note, paidAtMillis)
                                screen = AppScreen.LEDGER
                            },
                            onCreateConfirmed = { amountCents, type, category, merchantName, note, paidAtMillis, sourceApp, sourceType, rawText, imageUri ->
                                viewModel.addConfirmedScreenshotRecord(
                                    amountCents = amountCents,
                                    type = type,
                                    category = category,
                                    note = note,
                                    paidAtMillis = paidAtMillis,
                                    sourceApp = sourceApp,
                                    sourceType = sourceType,
                                    rawText = rawText,
                                    imageUri = imageUri,
                                    merchantName = merchantName
                                )
                                screen = AppScreen.LEDGER
                            }
                        )
                    }
                }

                AppScreen.NOTIFICATION_RAW_DETAIL -> {
                    val log = selectedNotificationLog
                    if (log == null) {
                        LaunchedEffect(Unit) { screen = AppScreen.REAL_NOTIFICATION_TEST }
                    } else {
                        NotificationRawTextDetailScreen(
                            log = log,
                            onBack = ::navigateBack,
                            onCreatePending = { targetLog ->
                                coroutineScope.launch {
                                    viewModel.createPendingFromNotificationLog(targetLog)
                                    screen = AppScreen.PENDING
                                }
                            }
                        )
                    }
                }
            }
        }
        if (showAutoListenConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showAutoListenConfirmDialog = false },
                title = { Text("提示") },
                text = {
                    Text("开启自动监听后，App 会显示“自动记账监听中”的前台通知，用于提高微信/支付宝付款通知监听稳定性。你可以随时在监听页关闭。")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAutoListenConfirmDialog = false
                            if (!TestPaymentNotificationSender.canPostNotifications(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            KeepAliveNotificationService.start(context)
                            KeepAliveNotificationService.markBootRestorePending(context, false)
                            viewModel.recordAutoListenChanged(true)
                            autoListeningEnabled = true
                            autoListenServiceMissing = false
                            healthRefreshTick = System.currentTimeMillis()
                            coroutineScope.launch {
                                delay(1_000)
                                healthRefreshTick = System.currentTimeMillis()
                            }
                        }
                    ) {
                        Text("开启自动监听")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAutoListenConfirmDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
        if (showAutoListenStartupDialog) {
            AlertDialog(
                onDismissRequest = { showAutoListenStartupDialog = false },
                title = { Text("提示") },
                text = {
                    Text("当前未开启自动监听，付款不会自动记录。是否去监听页开启？")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAutoListenStartupDialog = false
                            selectedMainTab = MainTab.LISTENER
                            screen = AppScreen.LEDGER
                        }
                    ) {
                        Text("去开启")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAutoListenStartupDialog = false }) {
                        Text("稍后")
                    }
                }
            )
        }
        if (showAutoListenRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showAutoListenRestoreDialog = false },
                title = { Text("提示") },
                text = {
                    Text("自动监听已开启，但前台服务没有运行，可能无法自动记录付款。是否立即恢复？")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAutoListenRestoreDialog = false
                            restoreAutoListen("startupAutoListenRestore")
                        }
                    ) {
                        Text("立即恢复")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAutoListenRestoreDialog = false }) {
                        Text("稍后")
                    }
                }
            )
        }
        if (showDailyLimitAlert) {
            AlertDialog(
                onDismissRequest = { showDailyLimitAlert = false },
                title = { Text("提示") },
                text = {
                    Text("今日支出已超过 ${formatMoney(dailyLimitStatus.limitCents)}，请注意控制消费。")
                },
                confirmButton = {
                    Button(onClick = { showDailyLimitAlert = false }) {
                        Text("知道了")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            DailyLimitManager.muteToday(context)
                            dailyLimitConfig = DailyLimitManager.load(context)
                            showDailyLimitAlert = false
                        }
                    ) {
                        Text("今日不再提醒")
                    }
                }
            )
        }
    }
}

@Composable
private fun MainTabsScreen(
    uiState: BookkeepingUiState,
    selectedTab: MainTab,
    bookkeepingListState: LazyListState,
    dailyLimitListState: LazyListState,
    listenerListState: LazyListState,
    onSelectedTabChange: (MainTab) -> Unit,
    notificationAccessEnabled: Boolean,
    autoListeningEnabled: Boolean,
    autoListenServiceMissing: Boolean,
    dailyLimitStatus: DailyLimitStatus,
    healthRefreshTick: Long,
    listenerRescueState: ListenerRescueUiState,
    screenshotOcrBusy: Boolean,
    screenshotOcrError: String,
    onAdd: () -> Unit,
    onOpenPending: () -> Unit,
    onOpenDebugLogs: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onOpenBackgroundReport: () -> Unit,
    onEnableAutoListen: () -> Unit,
    onDisableAutoListen: () -> Unit,
    onRestoreAutoListen: () -> Unit,
    onOpenRealNotificationTest: () -> Unit,
    onOpenWechatScanTest: () -> Unit,
    onOpenTroubleshooting: () -> Unit,
    onOpenQuickBackfill: () -> Unit,
    onOpenWechatScanBackfill: () -> Unit,
    onOpenScreenshotPicker: () -> Unit,
    onOpenLearning: () -> Unit,
    onGoToListenerTab: () -> Unit,
    onOpenRecordDetail: (ExpenseRecord) -> Unit,
    onSaveDailyLimit: (Boolean, Long) -> Unit,
    onToggleDailyLimit: (Boolean) -> Unit,
    onOneClickRepair: () -> Unit,
    onListenerRescue: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Scaffold(
        containerColor = PageBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.BOOKKEEPING,
                    onClick = { onSelectedTabChange(MainTab.BOOKKEEPING) },
                    label = { Text("记账") },
                    icon = { Text("") }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.STATS,
                    onClick = { onSelectedTabChange(MainTab.STATS) },
                    label = { Text("统计") },
                    icon = { Text("") }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.LIMIT,
                    onClick = { onSelectedTabChange(MainTab.LIMIT) },
                    label = { Text("限额") },
                    icon = { Text("") }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.LISTENER,
                    onClick = { onSelectedTabChange(MainTab.LISTENER) },
                    label = { Text("监听") },
                    icon = { Text("") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == MainTab.BOOKKEEPING) {
                FloatingActionButton(onClick = onAdd, containerColor = Green) {
                    Text("+", color = Color.White, fontSize = 28.sp)
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            MainTab.BOOKKEEPING -> BookkeepingTabContent(
                modifier = Modifier.padding(padding),
                uiState = uiState,
                listState = bookkeepingListState,
                autoListeningEnabled = autoListeningEnabled,
                autoListenServiceMissing = autoListenServiceMissing,
                screenshotOcrBusy = screenshotOcrBusy,
                screenshotOcrError = screenshotOcrError,
                onAdd = onAdd,
                onOpenPending = onOpenPending,
                onOpenStats = onOpenStats,
                onOpenRules = onOpenRules,
                onOpenBackup = onOpenBackup,
                onOpenQuickBackfill = onOpenQuickBackfill,
                onOpenWechatScanBackfill = onOpenWechatScanBackfill,
                onOpenScreenshotPicker = onOpenScreenshotPicker,
                onOpenLearning = onOpenLearning,
                onGoToListenerTab = onGoToListenerTab,
                onRestoreAutoListen = onRestoreAutoListen,
                onOpenRecordDetail = onOpenRecordDetail
            )

            MainTab.STATS -> StatisticsScreen(
                records = uiState.confirmedRecords,
                onBack = { onSelectedTabChange(MainTab.BOOKKEEPING) },
                modifier = Modifier.padding(padding),
                showBack = false
            )

            MainTab.LIMIT -> DailyLimitTabContent(
                modifier = Modifier.padding(padding),
                status = dailyLimitStatus,
                listState = dailyLimitListState,
                onSaveDailyLimit = onSaveDailyLimit,
                onToggleDailyLimit = onToggleDailyLimit
            )

            MainTab.LISTENER -> ListenerTabContent(
                modifier = Modifier.padding(padding),
                notificationAccessEnabled = notificationAccessEnabled,
                listState = listenerListState,
                healthRefreshTick = healthRefreshTick,
                listenerRescueState = listenerRescueState,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenDebugLogs = onOpenDebugLogs,
                onOpenHealth = onOpenHealth,
                onOpenBackgroundSettings = onOpenBackgroundSettings,
                onOpenBackgroundReport = onOpenBackgroundReport,
                onEnableAutoListen = onEnableAutoListen,
                onDisableAutoListen = onDisableAutoListen,
                onRestoreAutoListen = onRestoreAutoListen,
                onOpenRealNotificationTest = onOpenRealNotificationTest,
                onOpenWechatScanTest = onOpenWechatScanTest,
                onOpenTroubleshooting = onOpenTroubleshooting,
                onOpenWechatScanBackfill = onOpenWechatScanBackfill,
                onOneClickRepair = onOneClickRepair,
                onListenerRescue = onListenerRescue
            )
        }
    }
}

@Composable
private fun BookkeepingTabContent(
    modifier: Modifier = Modifier,
    uiState: BookkeepingUiState,
    listState: LazyListState,
    autoListeningEnabled: Boolean,
    autoListenServiceMissing: Boolean,
    screenshotOcrBusy: Boolean,
    screenshotOcrError: String,
    onAdd: () -> Unit,
    onOpenPending: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenQuickBackfill: () -> Unit,
    onOpenWechatScanBackfill: () -> Unit,
    onOpenScreenshotPicker: () -> Unit,
    onOpenLearning: () -> Unit,
    onGoToListenerTab: () -> Unit,
    onRestoreAutoListen: () -> Unit,
    onOpenRecordDetail: (ExpenseRecord) -> Unit
) {
    var ledgerGroupType by remember { mutableStateOf(BillGroupType.DAY) }
    val todayStats = BillStatisticsCalculator.calculate(
        uiState.confirmedRecords,
        BillStatisticsCalculator.rangeFor(StatsRangeType.TODAY)
    )
    val weekStats = BillStatisticsCalculator.calculate(
        uiState.confirmedRecords,
        BillStatisticsCalculator.rangeFor(StatsRangeType.WEEK)
    )
    val monthStats = BillStatisticsCalculator.calculate(
        uiState.confirmedRecords,
        BillStatisticsCalculator.rangeFor(StatsRangeType.MONTH)
    )
    val ledgerGroups = BillStatisticsCalculator.calculate(
        records = uiState.confirmedRecords,
        range = TimeRange(0L, Long.MAX_VALUE, "全部"),
        groupType = ledgerGroupType
    ).groups

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("本地自动记账", color = PrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(APP_VERSION_DISPLAY, color = MutedText)
        }
        if (!autoListeningEnabled) {
            item { AutoListenDisabledWarningCard(onGoToListenerTab) }
        } else if (autoListenServiceMissing) {
            item { AutoListenIncompleteWarningCard(onRestoreAutoListen) }
        }
        item { SummaryCard(uiState, onOpenPending) }
        item { PeriodExpenseCard(todayStats, weekStats, monthStats) }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("快捷操作", color = PrimaryText, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAdd) { Text("记一笔") }
                        OutlinedButton(onClick = onOpenPending) { Text("待确认 ${uiState.pendingCount}") }
                        OutlinedButton(onClick = onOpenStats) { Text("统计") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenRules) { Text("分类规则") }
                        OutlinedButton(onClick = onOpenLearning) { Text("学习记录") }
                        OutlinedButton(onClick = onOpenQuickBackfill) { Text("补录") }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        onClick = onOpenWechatScanBackfill
                    ) {
                        Text("扫码支付后补录")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        onClick = onOpenBackup
                    ) {
                        Text("备份与恢复")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        onClick = onOpenScreenshotPicker,
                        enabled = !screenshotOcrBusy
                    ) {
                        Text(if (screenshotOcrBusy) "正在识别..." else "截图记账")
                    }
                    if (screenshotOcrError.isNotBlank()) {
                        Text(screenshotOcrError, color = Red, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("最近账单",  color = PrimaryText, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatsRangeButton("按天", ledgerGroupType == BillGroupType.DAY) { ledgerGroupType = BillGroupType.DAY }
                        StatsRangeButton("按周", ledgerGroupType == BillGroupType.WEEK) { ledgerGroupType = BillGroupType.WEEK }
                        StatsRangeButton("按月", ledgerGroupType == BillGroupType.MONTH) { ledgerGroupType = BillGroupType.MONTH }
                    }
                }
            }
        }
        if (uiState.confirmedRecords.isEmpty()) {
            item { EmptyLedgerCard(onAdd) }
        } else {
            items(ledgerGroups.take(8), key = { it.label }) { group ->
                BillGroupCard(group, onRecordClick = onOpenRecordDetail)
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

@Composable
private fun BackupScreen(
    lastBackupAt: Long,
    message: String,
    busy: Boolean,
    onBack: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("备份与恢复", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("导出 CSV、导出 JSON 备份，或从 JSON 恢复账单。", color = MutedText)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("最近一次备份", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (lastBackupAt > 0L) formatDateTime(lastBackupAt) else "暂无备份",
                        color = if (lastBackupAt > 0L) Green else MutedText
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("导出", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !busy,
                        onClick = onExportJson
                    ) {
                        Text("导出 JSON 备份")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !busy,
                        onClick = onExportCsv
                    ) {
                        Text("导出 CSV")
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("恢复", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("从 JSON 恢复，已存在的重复账单会自动跳过。", color = MutedText, fontSize = 13.sp)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !busy,
                        onClick = onImportJson
                    ) {
                        Text("从 JSON 恢复")
                    }
                }
            }
        }
        if (busy || message.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = if (busy) "处理中..." else message,
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        color = if (message.contains("失败")) Red else PrimaryText
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DailyLimitTabContent(
    modifier: Modifier = Modifier,
    status: DailyLimitStatus,
    listState: LazyListState,
    onSaveDailyLimit: (Boolean, Long) -> Unit,
    onToggleDailyLimit: (Boolean) -> Unit
) {
    var amountText by remember(status.limitCents) {
        mutableStateOf(if (status.limitCents > 0L) formatYuanInput(status.limitCents) else "")
    }
    var error by remember { mutableStateOf("") }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("今日限额",  color = PrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("设置每日限额并查看今日消费进度",  color = MutedText)
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("今日限额",  color = PrimaryText, fontWeight = FontWeight.Bold)
                            Text(if (status.enabled) "已启用" else "未启用", color = MutedText, fontSize = 13.sp)
                        }
                        Switch(
                            checked = status.enabled,
                            onCheckedChange = { enabled ->
                                if (enabled && status.limitCents <= 0L) {
                                    error = "请先设置每日限额"
                                } else {
                                    error = ""
                                    onToggleDailyLimit(enabled)
                                }
                            }
                        )
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("设置每日限额，例如 100") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        onClick = {
                            val cents = parseAmountCents(amountText)
                            if (cents == null || cents <= 0L) {
                                error = "请输入大于 0 的金额"
                            } else {
                                error = ""
                                onSaveDailyLimit(true, cents)
                            }
                        }
                    ) {
                        Text("设置每日限额")
                    }
                    if (status.enabled) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onToggleDailyLimit(false) }) {
                            Text("关闭限额")
                        }
                    }
                    if (error.isNotBlank()) {
                        Text(error, color = Red, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("今日消费", color = PrimaryText, fontWeight = FontWeight.Bold)
                    HealthStatusRow("今日已消费", formatMoney(status.spentCents))
                    if (status.exceeded) {
                        HealthStatusRow("已超额", formatMoney(status.exceededCents))
                    } else {
                        HealthStatusRow("剩余额度", if (status.enabled) formatMoney(status.remainingCents) else "未启用")
                    }
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (status.exceeded) Red else Green,
                        trackColor = PageBackground
                    )
                    Text(
                        if (status.exceeded) "已超额" else "未超额",
                        color = if (status.exceeded) Red else MutedText,
                        fontSize = 13.sp,
                        fontWeight = if (status.exceeded) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

@Composable
private fun AutoListenDisabledWarningCard(onGoToListenerTab: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SoftRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("自动监听未开启，付款不会自动记录。",  color = Red, fontWeight = FontWeight.Bold)
            Button(modifier = Modifier.fillMaxWidth(), onClick = onGoToListenerTab) {
                Text("去开启监听")
            }
        }
    }
}

@Composable
private fun AutoListenIncompleteWarningCard(onRestoreAutoListen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SoftRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("自动监听已开启，但前台服务未运行。",  color = Red, fontWeight = FontWeight.Bold)
            Button(modifier = Modifier.fillMaxWidth(), onClick = onRestoreAutoListen) {
                Text("立即恢复")
            }
        }
    }
}

@Composable
private fun PeriodExpenseCard(
    todayStats: BillStatistics,
    weekStats: BillStatistics,
    monthStats: BillStatistics
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("统计卡片",  color = PrimaryText, fontWeight = FontWeight.Bold)
            HealthStatusRow("今日支出", "${formatMoney(todayStats.totalExpenseCents)} / ${todayStats.expenseCount} 笔")
            HealthStatusRow("本周支出", "${formatMoney(weekStats.totalExpenseCents)} / ${weekStats.expenseCount} 笔")
            HealthStatusRow("本月支出", "${formatMoney(monthStats.totalExpenseCents)} / ${monthStats.expenseCount} 笔")
            HealthStatusRow("本月收入", "${formatMoney(monthStats.totalIncomeCents)} / ${monthStats.incomeCount} 笔")
            HealthStatusRow("结余", formatMoney(monthStats.balanceCents))
        }
    }
}

@Composable
private fun ListenerTabContent(
    modifier: Modifier = Modifier,
    notificationAccessEnabled: Boolean,
    listState: LazyListState,
    healthRefreshTick: Long,
    listenerRescueState: ListenerRescueUiState,
    onOpenNotificationSettings: () -> Unit,
    onOpenDebugLogs: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onOpenBackgroundReport: () -> Unit,
    onEnableAutoListen: () -> Unit,
    onDisableAutoListen: () -> Unit,
    onRestoreAutoListen: () -> Unit,
    onOpenRealNotificationTest: () -> Unit,
    onOpenWechatScanTest: () -> Unit,
    onOpenTroubleshooting: () -> Unit,
    onOpenWechatScanBackfill: () -> Unit,
    onOneClickRepair: () -> Unit,
    onListenerRescue: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("自动监听",  color = PrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("检查通知监听、前台服务和后台稳定设置",  color = MutedText)
        }
        item {
            ListenerStatusCard(
                notificationAccessEnabled = notificationAccessEnabled,
                healthRefreshTick = healthRefreshTick,
                listenerRescueState = listenerRescueState,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onEnableAutoListen = onEnableAutoListen,
                onDisableAutoListen = onDisableAutoListen,
                onRestoreAutoListen = onRestoreAutoListen,
                onOneClickRepair = onOneClickRepair,
                onListenerRescue = onListenerRescue
            )
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("监听工具", color = PrimaryText, fontWeight = FontWeight.Bold)
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenHealth) {
                        Text("监听健康状态")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenBackgroundSettings) {
                        Text("后台稳定设置")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenBackgroundReport) {
                        Text("后台诊断报告")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenDebugLogs) { Text("通知日志") }
                        Button(onClick = onOpenRealNotificationTest) { Text("测试通知") }
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenWechatScanTest) {
                        Text("微信扫码支付测试")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenWechatScanBackfill) {
                        Text("扫码支付后补录")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenTroubleshooting) {
                        Text("监听问题排查")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(84.dp)) }
    }
}

@Composable
private fun ListenerStatusCard(
    notificationAccessEnabled: Boolean,
    healthRefreshTick: Long,
    listenerRescueState: ListenerRescueUiState,
    onOpenNotificationSettings: () -> Unit,
    onEnableAutoListen: () -> Unit,
    onDisableAutoListen: () -> Unit,
    onRestoreAutoListen: () -> Unit,
    onOneClickRepair: () -> Unit,
    onListenerRescue: () -> Unit
) {
    val context = LocalContext.current
    val runtimeState = NotificationListenerState.current(context)
    healthRefreshTick.hashCode()
    val autoEnabled = KeepAliveNotificationService.isEnabled(context)
    val foregroundRunning = KeepAliveNotificationService.isRunning(context)
    val recoverySnapshot = ListenerRecoveryState.snapshot(context)
    val probeSnapshot = com.localbookkeeping.app.notification.ListenerProbeNotification.snapshot(context)
    val health = ListenerHealthEvaluator.evaluate(
        ListenerHealthInput(
            notificationPermissionEnabled = notificationAccessEnabled,
            listenerConnected = runtimeState.listenerConnected,
            rawListenerConnected = runtimeState.rawListenerConnected,
            lastDisconnectedAt = runtimeState.lastDisconnectedTime,
            lastHeartbeatAt = runtimeState.lastHeartbeatTime,
            autoListenEnabled = autoEnabled,
            foregroundServiceRunning = foregroundRunning,
            testNotificationFailed = probeSnapshot.latestProbeFailed
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("监听健康状态",  color = PrimaryText, fontWeight = FontWeight.Bold)
            HealthStatusRow("通知监听权限", if (notificationAccessEnabled) "已授权" else "未授权")
            HealthStatusRow("监听状态", listenerStatusLabel(health.status))
            HealthStatusRow("自动监听", if (autoEnabled) "已启用" else "未启用")
            HealthStatusRow("前台服务", if (foregroundRunning) "运行中" else "未运行")
            HealthStatusRow("最近健康检查", formatOptionalDateTime(recoverySnapshot.lastHealthCheckAt))
            HealthStatusRow("最近探测成功", formatOptionalDateTime(probeSnapshot.lastSuccessTime))
            HealthStatusRow("最近探测失败", formatOptionalDateTime(probeSnapshot.lastFailTime))
            HealthStatusRow("最近通知", formatOptionalDateTime(runtimeState.lastNotificationTime))
            HealthStatusRow("最近微信通知", formatOptionalDateTime(runtimeState.lastWechatNotificationTime))
            HealthStatusRow("最近支付宝通知", formatOptionalDateTime(runtimeState.lastAlipayNotificationTime))
            HealthStatusRow("最近付款通知", formatOptionalDateTime(runtimeState.lastPaymentNotificationTime))
            HealthStatusRow("最近自动修复", formatOptionalDateTime(recoverySnapshot.lastAutoRepairAt))
            HealthStatusRow("自动修复次数", "${recoverySnapshot.autoRepairCount}")
            HealthStatusRow("上次重绑结果", recoverySnapshot.lastRebindResult.ifBlank { "暂无" })
            HealthStatusRow("上次失败原因", recoverySnapshot.lastFailureReason.ifBlank { "暂无" })
            if (health.status != ListenerServiceStatus.HEALTHY) {
                Text("监听状态异常，请尝试一键修复或重新授权。",  color = Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (recoverySnapshot.needsListenerRecheck) {
                Text("系统提示需要重新检查监听连接。",  color = Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (!notificationAccessEnabled) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenNotificationSettings) {
                    Text("打开通知监听权限")
                }
            }
            if (!autoEnabled) {
                Text("自动监听未启用，付款不会自动记录。",  color = Red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Button(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = onEnableAutoListen) {
                    Text("开启自动监听")
                }
            } else if (foregroundRunning) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onDisableAutoListen) {
                    Text("关闭自动监听")
                }
            } else {
                Text("前台服务未运行，建议立即恢复。",  color = Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Button(modifier = Modifier.fillMaxWidth(), onClick = onRestoreAutoListen) {
                    Text("恢复前台服务")
                }
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOneClickRepair) {
                Text("一键修复")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !listenerRescueState.running,
                onClick = onListenerRescue
            ) {
                Text(if (listenerRescueState.running) "修复中..." else "监听急救")
            }
            if (listenerRescueState.hasResults) {
                Text("修复结果", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                HealthStatusRow("通知监听权限", listenerRescueState.permissionResult)
                HealthStatusRow("前台服务", listenerRescueState.foregroundServiceResult)
                HealthStatusRow("监听连接", listenerRescueState.listenerConnectionResult)
                HealthStatusRow("探测通知", listenerRescueState.probeResult)
                HealthStatusRow("requestRebind", listenerRescueState.rebindResult)
                Text(
                    "最终结论：${listenerRescueState.finalConclusion}",
                    color = if (listenerRescueState.finalConclusion == "监听恢复正常") Green else Orange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LedgerScreen(
    uiState: BookkeepingUiState,
    notificationAccessEnabled: Boolean,
    healthRefreshTick: Long,
    screenshotOcrBusy: Boolean,
    screenshotOcrError: String,
    onAdd: () -> Unit,
    onOpenPending: () -> Unit,
    onOpenDebugLogs: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onOpenBackgroundReport: () -> Unit,
    onEnableAutoListen: () -> Unit,
    onDisableAutoListen: () -> Unit,
    onRestoreAutoListen: () -> Unit,
    onOpenRealNotificationTest: () -> Unit,
    onOpenTroubleshooting: () -> Unit,
    onOpenQuickBackfill: () -> Unit,
    onOpenScreenshotPicker: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    var ledgerGroupType by remember { mutableStateOf(BillGroupType.DAY) }
    val ledgerGroups = BillStatisticsCalculator.calculate(
        records = uiState.confirmedRecords,
        range = TimeRange(0L, Long.MAX_VALUE, "全部"),
        groupType = ledgerGroupType
    ).groups
    Scaffold(
        containerColor = PageBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = Green) {
                Text("+", color = Color.White, fontSize = 28.sp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                Text("本地自动记账", color = PrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(APP_VERSION_DISPLAY, color = MutedText)
                Spacer(Modifier.height(16.dp))
                SummaryCard(uiState, onOpenPending)
                Spacer(Modifier.height(12.dp))
                NotificationAccessCard(
                    notificationAccessEnabled = notificationAccessEnabled,
                    healthRefreshTick = healthRefreshTick,
                    screenshotOcrBusy = screenshotOcrBusy,
                    screenshotOcrError = screenshotOcrError,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onOpenDebugLogs = onOpenDebugLogs,
                    onOpenRules = onOpenRules,
                    onOpenHealth = onOpenHealth,
                    onOpenStats = onOpenStats,
                    onOpenBackgroundSettings = onOpenBackgroundSettings,
                    onOpenBackgroundReport = onOpenBackgroundReport,
                    onEnableAutoListen = onEnableAutoListen,
                    onDisableAutoListen = onDisableAutoListen,
                    onRestoreAutoListen = onRestoreAutoListen,
                    onOpenRealNotificationTest = onOpenRealNotificationTest,
                    onOpenTroubleshooting = onOpenTroubleshooting,
                    onOpenQuickBackfill = onOpenQuickBackfill,
                    onOpenScreenshotPicker = onOpenScreenshotPicker
                )
            }

            if (uiState.confirmedRecords.isEmpty()) {
                item { EmptyLedgerCard(onAdd) }
            } else {
                item {
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("最近账单",  color = PrimaryText, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatsRangeButton("按天", ledgerGroupType == BillGroupType.DAY) { ledgerGroupType = BillGroupType.DAY }
                                StatsRangeButton("按周", ledgerGroupType == BillGroupType.WEEK) { ledgerGroupType = BillGroupType.WEEK }
                                StatsRangeButton("按月", ledgerGroupType == BillGroupType.MONTH) { ledgerGroupType = BillGroupType.MONTH }
                            }
                        }
                    }
                }
                items(ledgerGroups, key = { it.label }) { group ->
                    BillGroupCard(group)
                }
                item { Spacer(Modifier.height(84.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryCard(uiState: BookkeepingUiState, onOpenPending: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今日概览",  color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (uiState.pendingCount > 0) {
                    OutlinedButton(onClick = onOpenPending) {
                        Text("待确认 ${uiState.pendingCount}")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryValue("今日收入", uiState.incomeCents, Green)
                SummaryValue("今日支出", uiState.expenseCents, Red)
                SummaryValue("总收入", uiState.incomeCents, Green)
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, amountCents: Long, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = MutedText, fontSize = 13.sp)
        Text(formatMoney(amountCents), color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun NotificationAccessCard(
    notificationAccessEnabled: Boolean,
    healthRefreshTick: Long,
    screenshotOcrBusy: Boolean,
    screenshotOcrError: String,
    onOpenNotificationSettings: () -> Unit,
    onOpenDebugLogs: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenBackgroundSettings: () -> Unit,
    onOpenBackgroundReport: () -> Unit,
    onEnableAutoListen: () -> Unit,
    onDisableAutoListen: () -> Unit,
    onRestoreAutoListen: () -> Unit,
    onOpenRealNotificationTest: () -> Unit,
    onOpenTroubleshooting: () -> Unit,
    onOpenQuickBackfill: () -> Unit,
    onOpenScreenshotPicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val runtimeState = NotificationListenerState.current(LocalContext.current)
            healthRefreshTick.hashCode()
            val autoEnabled = KeepAliveNotificationService.isEnabled(LocalContext.current)
            val foregroundRunning = KeepAliveNotificationService.isRunning(LocalContext.current)
            val recoverySnapshot = ListenerRecoveryState.snapshot(LocalContext.current)
            val health = ListenerHealthEvaluator.evaluate(
                ListenerHealthInput(
                    notificationPermissionEnabled = notificationAccessEnabled,
                    listenerConnected = runtimeState.listenerConnected,
                    rawListenerConnected = runtimeState.rawListenerConnected,
                    lastDisconnectedAt = runtimeState.lastDisconnectedTime,
                    lastHeartbeatAt = runtimeState.lastHeartbeatTime,
                    autoListenEnabled = autoEnabled,
                    foregroundServiceRunning = foregroundRunning
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("通知监听权限",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (notificationAccessEnabled) "已授权" else "未授权",
                    color = if (notificationAccessEnabled) Green else Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("监听健康状态",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Text(
                    text = listenerStatusLabel(health.status),
                    color = listenerStatusColor(health.status),
                    fontWeight = FontWeight.SemiBold
                )
            }
            HealthStatusRow("自动监听", if (autoEnabled) "已启用" else "未启用")
            HealthStatusRow("前台服务", if (foregroundRunning) "运行中" else "未运行")
            HealthStatusRow("监听状态", listenerStatusLabel(health.status))
            HealthStatusRow("上次健康检查", formatOptionalDateTime(recoverySnapshot.lastHealthCheckAt))
            HealthStatusRow("上次自动修复", formatOptionalDateTime(recoverySnapshot.lastAutoRepairAt))
            HealthStatusRow("自动修复次数", "${recoverySnapshot.autoRepairCount}")
            HealthStatusRow("上次重绑结果", recoverySnapshot.lastRebindResult.ifBlank { "暂无" })
            HealthStatusRow("上次失败原因", recoverySnapshot.lastFailureReason.ifBlank { "暂无" })
            HealthStatusRow("最近通知", formatOptionalDateTime(runtimeState.lastNotificationTime))
            HealthStatusRow("最近心跳", formatOptionalDateTime(runtimeState.lastHeartbeatTime))
            HealthStatusRow("最近微信通知", formatOptionalDateTime(runtimeState.lastWechatNotificationTime))
            HealthStatusRow("最近支付宝通知", formatOptionalDateTime(runtimeState.lastAlipayNotificationTime))
            HealthStatusRow("最近付款通知", formatOptionalDateTime(runtimeState.lastPaymentNotificationTime))
            if (health.status != ListenerServiceStatus.HEALTHY) {
                Text("监听疑似失效，请尝试一键修复或重新授权。",  color = Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (recoverySnapshot.needsListenerRecheck) {
                Text("系统提示需要重新检查监听连接。",  color = Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (!autoEnabled) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onEnableAutoListen) {
                    Text("开启自动监听")
                }
            } else if (foregroundRunning) {
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onDisableAutoListen) {
                    Text("关闭自动监听")
                }
            } else {
                Text("前台服务未运行，建议立即恢复。",  color = Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Button(modifier = Modifier.fillMaxWidth(), onClick = onRestoreAutoListen) {
                    Text("恢复自动监听")
                }
            }
            Text("开启自动监听后，App 会显示前台通知以提升稳定性。",  color = MutedText, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!notificationAccessEnabled) {
                    Button(onClick = onOpenNotificationSettings) {
                        Text("打开通知监听权限")
                    }
                }
                OutlinedButton(onClick = onOpenDebugLogs) {
                    Text("通知日志")
                }
                OutlinedButton(onClick = onOpenHealth) {
                    Text("监听健康状态")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenStats) { Text("统计") }
                OutlinedButton(onClick = onOpenBackgroundSettings) { Text("后台稳定设置") }
                OutlinedButton(onClick = onOpenBackgroundReport) { Text("后台诊断报告") }
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenRules) {
                Text("分类规则")
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenRealNotificationTest) {
                Text("测试通知")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenTroubleshooting) {
                    Text("监听问题排查")
                }
                OutlinedButton(onClick = onOpenQuickBackfill) {
                    Text("刚刚付款补录")
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth().height(46.dp),
                onClick = onOpenScreenshotPicker,
                enabled = !screenshotOcrBusy
            ) {
                Text(if (screenshotOcrBusy) "正在识别..." else "截图记账")
            }
            if (screenshotOcrError.isNotBlank()) {
                Text(screenshotOcrError, color = Red, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EmptyLedgerCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("暂无账单",  color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("点击下方按钮手动记一笔",  color = MutedText, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd) { Text("记账") }
        }
    }
}

@Composable
private fun ExpenseRow(record: ExpenseRecord) {
    val isIncome = record.type == TransactionType.INCOME
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBubble(record.category, isIncome)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.category, color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Text(record.merchantName.ifBlank { record.note.ifBlank { "无备注" } },  color = MutedText, fontSize = 13.sp)
                Text("${record.sourceApp} / ${formatDate(record.paidAtMillis)}", color = MutedText, fontSize = 12.sp)
            }
            Text(
                text = formatMoney(if (isIncome) record.amountCents else -record.amountCents),
                color = if (isIncome) Green else Red,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ScreenshotPreviewScreen(
    preview: ScreenshotPreviewState,
    onBack: () -> Unit,
    onConfirmExistingPending: (Long, Long, TransactionType, String, String, String, Long) -> Unit,
    onCreateConfirmed: (Long, TransactionType, String, String, String, Long, String, String, String, String) -> Unit
) {
    val parsed = preview.parseResult.bill
    var amount by remember(preview.imageUri) {
        mutableStateOf(parsed?.amountCents?.let { formatYuanInput(it) }.orEmpty())
    }
    var type by remember(preview.imageUri) { mutableStateOf(parsed?.type ?: TransactionType.UNKNOWN) }
    var merchant by remember(preview.imageUri) { mutableStateOf(parsed?.merchant.orEmpty()) }
    var category by remember(preview.imageUri) {
        mutableStateOf(
            when (type) {
                TransactionType.INCOME -> ExpenseCategories.income.first()
                TransactionType.EXPENSE -> ExpenseCategories.expense.first()
                TransactionType.UNKNOWN -> "待选择"
            }
        )
    }
    var dateText by remember(preview.imageUri) {
        mutableStateOf(formatDate(parsed?.paidAtMillis ?: System.currentTimeMillis()))
    }
    var error by remember { mutableStateOf("") }
    val categories = when (type) {
        TransactionType.INCOME -> ExpenseCategories.income
        TransactionType.EXPENSE -> ExpenseCategories.expense
        TransactionType.UNKNOWN -> listOf("Unknown") + ExpenseCategories.expense + ExpenseCategories.income
    }.let { (listOf(category) + it).distinct() }

    LaunchedEffect(type) {
        if (category !in categories) {
            category = when (type) {
                TransactionType.INCOME -> ExpenseCategories.income.first()
                TransactionType.EXPENSE -> ExpenseCategories.expense.first()
                TransactionType.UNKNOWN -> "待选择"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("统计",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(preview.message, color = if (preview.parseResult.bill == null) Orange else MutedText)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ScreenshotThumbnail(preview.imageUri)
                    // repaired damaged text line
                    // repaired damaged text line
                    // repaired damaged text line

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("金额") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Text("类型",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TypeChoice("支出", TransactionType.EXPENSE, type) { type = it }
                        TypeChoice("收入", TransactionType.INCOME, type) { type = it }
                    }
                    if (type == TransactionType.UNKNOWN) {
                        Text("请选择收入或支出",  color = Orange, fontSize = 13.sp)
                    }

                    CategoryDropdown(
                        category = category,
                        categories = categories,
                        onCategoryChange = { category = it }
                    )

                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("商户") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("时间 yyyy-MM-dd") },
                        singleLine = true
                    )

                    if (error.isNotBlank()) {
                        Text(error, color = Red, fontSize = 13.sp)
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            val amountCents = parseAmountCents(amount)
                            val paidAtMillis = parseDateMillis(dateText)
                            val sourceApp = parsed?.sourceApp ?: "截图识别"
                            val sourceType = parsed?.sourceType ?: preview.parseResult.sourceType
                            val note = listOf(sourceApp, merchant.ifBlank { "截图补录" })
                                .joinToString(" / ")
                                .take(80)
                            when {
                                amountCents == null || amountCents <= 0 -> error = "请输入大于 0 的金额"
                                type == TransactionType.UNKNOWN -> error = "请选择收入或支出"
                                paidAtMillis == null -> error = "时间格式应为 yyyy-MM-dd"
                                preview.pendingRecordId != null -> onConfirmExistingPending(
                                    preview.pendingRecordId,
                                    amountCents,
                                    type,
                                    category,
                                    merchant.trim(),
                                    note,
                                    paidAtMillis
                                )
                                else -> onCreateConfirmed(
                                    amountCents,
                                    type,
                                    category,
                                    merchant.trim(),
                                    note,
                                    paidAtMillis,
                                    sourceApp,
                                    sourceType,
                                    preview.rawText,
                                    preview.imageUri
                                )
                            }
                        }
                    ) {
                        Text("确认生成账单")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ScreenshotThumbnail(imageUri: String) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageUri) {
        bitmap = loadBitmapFromUri(context, Uri.parse(imageUri))
    }
    val current = bitmap
    if (current == null) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1.7f).background(PageBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("正在加载截图",  color = MutedText)
        }
    } else {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = "截图预览",
            modifier = Modifier.fillMaxWidth().aspectRatio(1.7f).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun RecordDetailScreen(
    record: ExpenseRecord,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showRawText by remember(record.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("账单详情", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val typeLabel = when (record.type) {
                        TransactionType.INCOME -> "收入"
                        TransactionType.EXPENSE -> "支出"
                        TransactionType.UNKNOWN -> "未知"
                    }
                    HealthStatusRow("金额", formatMoney(record.amountCents))
                    HealthStatusRow("类型", typeLabel)
                    HealthStatusRow("分类", record.category)
                    HealthStatusRow("分类来源", record.categorySource.ifBlank { "未知" })
                    if (record.matchedRuleName.isNotBlank()) {
                        HealthStatusRow("匹配规则", record.matchedRuleName)
                    }
                    if (record.matchedKeyword.isNotBlank()) {
                        HealthStatusRow("匹配关键词", record.matchedKeyword)
                    }
                    HealthStatusRow("商户", record.merchantName.ifBlank { "未知" })
                    HealthStatusRow("来源", record.sourceApp)
                    HealthStatusRow("时间", formatDateTime(record.paidAtMillis))
                    HealthStatusRow("备注", record.note.ifBlank { "无" })
                    if (record.rawText.isNotBlank()) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showRawText = !showRawText }) {
                            Text(if (showRawText) "隐藏原始文本" else "查看原始文本")
                        }
                        if (showRawText) {
                            Text(record.rawText, color = PrimaryText, fontSize = 12.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onEdit) { Text("编辑") }
                        OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("删除") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("提示") },
            text = { Text("确认删除这条账单？") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) { Text("确认删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PendingBillsScreen(
    pendingRecords: List<ExpenseRecord>,
    onBack: () -> Unit,
    onConfirm: (Long) -> Unit,
    onEdit: (ExpenseRecord) -> Unit,
    onIgnore: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("待确认账单",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("确认或编辑自动识别出的账单",  color = MutedText)
        }

        if (pendingRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "暂无待确认账单",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(pendingRecords, key = { it.id }) { record ->
                PendingBillRow(record, onConfirm, onEdit, onIgnore)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PendingBillRow(
    record: ExpenseRecord,
    onConfirm: (Long) -> Unit,
    onEdit: (ExpenseRecord) -> Unit,
    onIgnore: (Long) -> Unit
) {
    val isIncome = record.type == TransactionType.INCOME
    val needsAmount = record.status == RecordStatus.NEED_AMOUNT
    val canQuickConfirm = !needsAmount &&
        record.type != TransactionType.UNKNOWN &&
        record.category != "待选择" &&
        record.merchantName.isNotBlank()
    val typeText = when (record.type) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
        TransactionType.UNKNOWN -> "未知"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBubble(record.sourceApp.take(1), isIncome)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (needsAmount) "${record.sourceApp} 通知缺少金额" else "${record.sourceApp} 通知",
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("来源：${record.sourceApp}", color = MutedText, fontSize = 13.sp)
                    Text(
                        "${typeText} / 置信度 ${record.confidence} / ${formatDate(record.notificationPostedAtMillis.takeIf { it > 0 } ?: record.paidAtMillis)}",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = if (needsAmount) "缺少金额" else formatMoney(if (isIncome) record.amountCents else -record.amountCents),
                    color = if (needsAmount) Orange else if (isIncome) Green else Red,
                    fontWeight = FontWeight.Bold
                )
            }
            DebugField("商户", record.merchantName.ifBlank { "未知" })
            DebugField("分类/类型", "${record.category} / $typeText")
            if (record.categorySource == CategoryClassifier.SOURCE_LEARNED) {
                DebugField("推荐分类", record.category)
                DebugField("命中商户", record.matchedKeyword.ifBlank { record.merchantName.ifBlank { "未知" } })
                DebugField("来源", "历史学习")
            }
            if (record.matchedRuleName.isNotBlank()) {
                DebugField("命中规则", record.matchedRuleName)
            }
            DebugField("备注", record.note.ifBlank { "无" })
            if (record.rawText.isNotBlank()) {
                Text("原始文本：${record.rawText.take(260)}", color = PrimaryText, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needsAmount) {
                    Button(onClick = { onEdit(record) }) { Text("补全金额") }
                } else if (canQuickConfirm) {
                    Button(onClick = { onConfirm(record.id) }) { Text("确认") }
                    OutlinedButton(onClick = { onEdit(record) }) { Text("编辑") }
                } else {
                    Button(onClick = { onEdit(record) }) { Text("编辑") }
                }
                OutlinedButton(onClick = { onIgnore(record.id) }) { Text("忽略") }
            }
        }
    }
}

@Composable
private fun ListenerHealthScreen(
    health: ListenerHealthStatus,
    autoListeningEnabled: Boolean,
    rebindMessage: String,
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestRebind: () -> Unit,
    onOneClickRepair: () -> Unit,
    onReauthorizeNotificationListener: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleAutoListening: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("监听健康状态",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            if (!health.listenerServiceActive) {
                Text("监听疑似失效，请检查权限和前台服务。",  color = Orange, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HealthStatusRow("通知监听权限", health.notificationListenerPermission)
                    HealthStatusRow("监听状态", health.listenerServiceStatus)
                    HealthStatusRow("监听原因", health.listenerServiceReasons.ifBlank { "无" })
                    HealthStatusRow("App 通知权限", health.appNotificationPermission)
                    HealthStatusRow("微信通知权限", health.wechatNotificationPermission)
                    HealthStatusRow("支付宝通知权限", health.alipayNotificationPermission)
                    HealthStatusRow("后台运行", health.backgroundRunStatus)
                    HealthStatusRow("前台服务", health.foregroundServiceStatus)
                    HealthStatusRow("电池优化", health.batteryOptimizationStatus)
                    HealthStatusRow("需要重新检查", yesNo(health.needsListenerRecheck))
                    HealthStatusRow("App 更新恢复", health.lastPackageReplacedAt)
                    HealthStatusRow("最近连接", health.lastConnectedAt)
                    HealthStatusRow("最近断开", health.lastDisconnectedAt)
                    HealthStatusRow("最近心跳", health.lastHeartbeatAt)
                    HealthStatusRow("最近通知", health.lastNotificationAt)
                    HealthStatusRow("最近付款通知", health.lastPaymentNotificationAt)
                    HealthStatusRow("最近通知来源", health.lastNotificationPackage)
                    HealthStatusRow("最近微信通知", health.lastWechatNotificationAt)
                    HealthStatusRow("最近支付宝通知", health.lastAlipayNotificationAt)
                    HealthStatusRow("最近自动记账", health.lastSuccessfulAccountingAt)
                    HealthStatusRow("当前健康状态", health.currentHealthStatus)
                    HealthStatusRow("最后健康检查", health.lastHealthCheckAt)
                    HealthStatusRow("最后自动修复", health.lastAutoRepairAt)
                    HealthStatusRow("自动修复次数", "${health.autoRepairCount}")
                    HealthStatusRow("最后重绑结果", health.lastRebindResult)
                    HealthStatusRow("最后失败原因", health.lastFailureReason)
                    HealthStatusRow("付款通知摘要", health.paymentNotificationSummary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("自动监听", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                            Text("开启后保持前台服务运行，提高通知监听稳定性。",  color = MutedText, fontSize = 13.sp)
                        }
                        Switch(checked = autoListeningEnabled, onCheckedChange = onToggleAutoListening)
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenNotificationSettings) {
                        Text("打开通知监听权限")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOneClickRepair) {
                        Text("一键修复")
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onRequestRebind) {
                        Text("重新绑定通知监听")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onReauthorizeNotificationListener) {
                        Text("重新授权通知监听")
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("修复建议",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Text("1. 确认通知监听权限已开启。", color = MutedText, fontSize = 13.sp)
                        Text("2. 确认自动监听和前台服务已运行。",  color = MutedText, fontSize = 13.sp)
                        Text("3. 若仍异常，请关闭再重新开启通知监听权限。",  color = MutedText, fontSize = 13.sp)
                        Text("4. 检查电池优化和后台限制设置。",  color = MutedText, fontSize = 13.sp)
                        Text("5. 必要时重启手机后再恢复自动监听。",  color = MutedText, fontSize = 13.sp)
                    }
                    if (rebindMessage.isNotBlank()) {
                        Text(rebindMessage, color = if (rebindMessage.contains("成功") || rebindMessage.contains("Connected", ignoreCase = true)) Green else Orange, fontSize = 13.sp)
                    }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenBatterySettings) {
                        Text("打开电池优化设置")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HealthStatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = PrimaryText, fontSize = 14.sp)
        Text(value, color = statusColor(value), fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatisticsScreen(
    records: List<ExpenseRecord>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true
) {
    var rangeType by remember { mutableStateOf(StatsRangeType.TODAY) }
    var groupType by remember { mutableStateOf(BillGroupType.DAY) }
    var customStart by remember { mutableStateOf(formatDate(System.currentTimeMillis())) }
    var customEnd by remember { mutableStateOf(formatDate(System.currentTimeMillis())) }
    val customStartMillis = parseDateMillis(customStart)
    val customEndMillis = parseDateMillis(customEnd)
    val range = BillStatisticsCalculator.rangeFor(
        type = rangeType,
        customStartMillis = customStartMillis,
        customEndMillis = customEndMillis
    )
    val stats = BillStatisticsCalculator.calculate(records, range, groupType)
    val weekArchives = BillStatisticsCalculator.monthWeekArchives(records)
    val monthArchives = BillStatisticsCalculator.yearMonthArchives(records)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack) {
                    TextButtonLike("返回", onBack)
                    Spacer(Modifier.width(12.dp))
                }
                Text("统计",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatsRangeButton("今日", rangeType == StatsRangeType.TODAY) { rangeType = StatsRangeType.TODAY }
                        StatsRangeButton("本周", rangeType == StatsRangeType.WEEK) { rangeType = StatsRangeType.WEEK }
                        StatsRangeButton("本月", rangeType == StatsRangeType.MONTH) { rangeType = StatsRangeType.MONTH }
                        StatsRangeButton("自定义", rangeType == StatsRangeType.CUSTOM) { rangeType = StatsRangeType.CUSTOM }
                    }
                    if (rangeType == StatsRangeType.CUSTOM) {
                        OutlinedTextField(customStart, { customStart = it }, label = { Text("开始日期") }, singleLine = true)
                        OutlinedTextField(customEnd, { customEnd = it }, label = { Text("结束日期") }, singleLine = true)
                    }
                    Text("范围：${stats.range.label}", color = MutedText, fontSize = 13.sp)
                }
            }
        }
        when (rangeType) {
            StatsRangeType.WEEK -> {
                if (weekArchives.isEmpty()) {
                    item { EmptyStatsCard("暂无周统计数据") }
                } else {
                    items(weekArchives, key = { it.label }) { archive ->
                        ArchivePeriodCard(archive)
                    }
                }
            }
            StatsRangeType.MONTH -> {
                if (monthArchives.isEmpty()) {
                    item { EmptyStatsCard("暂无月度统计数据") }
                } else {
                    items(monthArchives, key = { it.label }) { archive ->
                        ArchivePeriodCard(archive)
                    }
                }
            }
            else -> {
                item { StatisticsSummaryCard(stats) }
                item { SummaryListCard("分类汇总", stats.categoryItems, "暂无分类统计") }
                item { SummaryListCard("来源汇总", stats.sourceItems, "暂无来源统计") }
                item {
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("账单分组", color = PrimaryText, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatsRangeButton("按天", groupType == BillGroupType.DAY) { groupType = BillGroupType.DAY }
                                StatsRangeButton("按周", groupType == BillGroupType.WEEK) { groupType = BillGroupType.WEEK }
                                StatsRangeButton("按月", groupType == BillGroupType.MONTH) { groupType = BillGroupType.MONTH }
                            }
                        }
                    }
                }
                if (stats.groups.isEmpty()) {
                    item { EmptyStatsCard(if (rangeType == StatsRangeType.TODAY) "今日暂无账单" else "暂无数据") }
                } else {
                    items(stats.groups, key = { it.label }) { group ->
                        BillGroupCard(group)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun EmptyStatsCard(text: String) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            color = MutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatsRangeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

@Composable
private fun StatisticsSummaryCard(stats: BillStatistics) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("统计概览", color = PrimaryText, fontWeight = FontWeight.Bold)
            HealthStatusRow("总收入", formatMoney(stats.totalIncomeCents))
            HealthStatusRow("总支出", formatMoney(stats.totalExpenseCents))
            HealthStatusRow("结余", formatMoney(stats.balanceCents))
            HealthStatusRow("收入笔数", "${stats.incomeCount}")
            HealthStatusRow("支出笔数", "${stats.expenseCount}")
            HealthStatusRow("最大支出", stats.largestExpense?.let { "${it.note.ifBlank { it.category }} ${formatMoney(it.amountCents)}" } ?: "暂无")
            HealthStatusRow("最近账单", stats.latestRecord?.let { "${it.note.ifBlank { it.category }} ${formatMoney(it.amountCents)}" } ?: "暂无")
        }
    }
}

@Composable
private fun ArchivePeriodCard(archive: com.localbookkeeping.app.stats.ArchivePeriod) {
    var expanded by remember(archive.label) { mutableStateOf(false) }
    val stats = archive.statistics
    val totalCount = stats.expenseCount + stats.incomeCount
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${archive.label}  支出 ${formatMoney(stats.totalExpenseCents)}  收入 ${formatMoney(stats.totalIncomeCents)}  共 ${totalCount} 笔",
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text("结余 ${formatMoney(stats.balanceCents)}", color = MutedText, fontSize = 13.sp)
                }
                Text(if (expanded) "收起" else "展开",  color = Green, fontWeight = FontWeight.SemiBold)
            }
            if (expanded) {
                StatisticsSummaryCard(stats)
                SummaryListCard("分类汇总", stats.categoryItems, "暂无分类统计")
                SummaryListCard("来源汇总", stats.sourceItems, "暂无来源统计")
                if (stats.groups.isEmpty()) {
                    Text("暂无账单",  color = MutedText, fontSize = 13.sp)
                } else {
                    stats.groups.forEach { group ->
                        BillGroupCard(group)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryListCard(
    title: String,
    items: List<com.localbookkeeping.app.stats.SummaryItem>,
    emptyText: String = "暂无数据"
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = PrimaryText, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Text(emptyText, color = MutedText, fontSize = 13.sp)
            } else {
                items.forEach {
                    HealthStatusRow(it.label, "${formatMoney(it.amountCents)} / ${it.count} 笔 / ${"%.1f".format(it.percent * 100)}%")
                }
            }
        }
    }
}

@Composable
private fun BillGroupCard(
    group: com.localbookkeeping.app.stats.BillGroup,
    onRecordClick: ((ExpenseRecord) -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.label, color = PrimaryText, fontWeight = FontWeight.Bold)
            group.records.forEach { record ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onRecordClick != null) { onRecordClick?.invoke(record) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(record.note.ifBlank { record.category }, color = PrimaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        text = formatMoney(if (record.type == TransactionType.INCOME) record.amountCents else -record.amountCents),
                        color = if (record.type == TransactionType.INCOME) Green else Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text("支出 ${formatMoney(group.expenseCents)} / 收入 ${formatMoney(group.incomeCents)}", color = MutedText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BackgroundSettingsScreen(
    health: ListenerHealthStatus,
    autoListeningEnabled: Boolean,
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onToggleAutoListening: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("后台稳定设置",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("自动监听", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                            Text("保持前台服务运行，降低后台被系统回收的概率。",  color = MutedText, fontSize = 13.sp)
                        }
                        Switch(checked = autoListeningEnabled, onCheckedChange = onToggleAutoListening)
                    }
                    HealthStatusRow("自动监听", if (autoListeningEnabled) "已启用" else "未启用")
                    HealthStatusRow("通知监听权限", health.notificationListenerPermission)
                    HealthStatusRow("电池优化", health.batteryOptimizationStatus)
                    HealthStatusRow("后台运行", health.backgroundRunStatus)
                    HealthStatusRow("悬浮窗/弹窗", "手动检查")
                    HealthStatusRow("微信通知权限", health.wechatNotificationPermission)
                    HealthStatusRow("支付宝通知权限", health.alipayNotificationPermission)
                    HealthStatusRow("前台服务", health.foregroundServiceStatus)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenAppSettings) { Text("打开应用设置") }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenBatterySettings) { Text("打开电池优化设置") }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenNotificationSettings) { Text("打开通知监听权限") }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("建议检查",  color = PrimaryText, fontWeight = FontWeight.Bold)
                    listOf(
                        "系统设置 > 应用 > 本地自动记账 > 通知权限：允许",
                        "系统设置 > 电池 > 本地自动记账：不限制",
                        "通知监听权限中确认本地自动记账已开启",
                        "微信/支付宝通知权限中确认付款通知已开启",
                        "若系统清理后台，请将本应用加入白名单",
                        "开启自动监听后保持前台服务通知",
                        "后台异常时使用一键修复或重新授权"
                    ).forEach { Text(it, color = MutedText, fontSize = 13.sp) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun BackgroundReportScreen(
    logs: List<BackgroundStabilityLog>,
    notificationLogs: List<DebugNotificationLog>,
    records: List<ExpenseRecord>,
    onBack: () -> Unit
) {
    val since = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
    val recentLogs = logs.filter { it.createdAtMillis >= since }
    val recentNotifications = notificationLogs.filter { it.receivedAtMillis >= since }
    val recentRecords = records.filter { it.createdAtMillis >= since }
    val report = BackgroundDiagnosticsCalculator.calculate(logs, since)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("后台诊断报告",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("近 24 小时后台稳定性、通知监听和自动记账诊断。",  color = MutedText)
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthStatusRow("App 启动次数", "${report.appStartCount}")
                    HealthStatusRow("监听连接次数", "${report.listenerConnectedCount}")
                    HealthStatusRow("监听断开次数", "${report.listenerDisconnectedCount}")
                    HealthStatusRow("健康检查次数", "${report.healthCheckCount}")
                    HealthStatusRow("前台服务启动次数", "${report.foregroundServiceStartCount}")
                    HealthStatusRow("前台服务停止次数", "${report.foregroundServiceStopCount}")
                    HealthStatusRow("自动监听恢复次数", "${report.autoListenRestoreCount}")
                    HealthStatusRow("探测成功次数", "${report.probeSuccessCount}")
                    HealthStatusRow("探测失败次数", "${report.probeFailCount}")
                    HealthStatusRow("requestRebind 尝试次数", "${report.requestRebindAttemptCount}")
                    HealthStatusRow("requestRebind 成功次数", "${report.requestRebindConnectedCount}")
                    HealthStatusRow("requestRebind 失败次数", "${report.requestRebindFailedCount}")
                    HealthStatusRow("通知总数", "${recentNotifications.size}")
                    HealthStatusRow("微信通知数", "${recentNotifications.count { it.packageName == WECHAT_PACKAGE }}")
                    HealthStatusRow("支付宝通知数", "${recentNotifications.count { it.packageName == ALIPAY_PACKAGE }}")
                    HealthStatusRow("自动记账数", "${recentRecords.size}")
                    HealthStatusRow("付款通知捕获", "${report.paymentCapturedCount}")
                    HealthStatusRow("付款解析成功", "${report.paymentParseSuccessCount.coerceAtLeast(recentRecords.size)}")
                    HealthStatusRow("付款解析失败", "${report.paymentParseFailCount}")
                    HealthStatusRow(
                        "权限开启但 probe 失败次数",
                        "${report.listenerPermissionGrantedButProbeFailCount}"
                    )
                    HealthStatusRow(
                        "probe 失败后 requestRebind 次数",
                        "${report.requestRebindAfterProbeFailCount}"
                    )
                    HealthStatusRow("最近支付宝通知", "${recentNotifications.count { it.packageName == ALIPAY_PACKAGE }}")
                    HealthStatusRow("付款相关通知", "${report.paymentCapturedCount}")
                    HealthStatusRow("生成账单数量", "${report.paymentParseSuccessCount.coerceAtLeast(recentRecords.size)}")
                    HealthStatusRow("待确认账单", "${records.count { it.status != RecordStatus.CONFIRMED && !it.isDeleted }}")
                    HealthStatusRow("已确认账单", "${records.count { it.status == RecordStatus.CONFIRMED && !it.isDeleted }}")
                }
            }
        }
        item {
            val failures = report.failureReasons.entries
                .sortedByDescending { it.value }
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("失败原因统计",  color = PrimaryText, fontWeight = FontWeight.Bold)
                    if (failures.isEmpty()) {
                        Text("暂无失败原因", color = MutedText)
                    } else {
                        failures.forEach { (reason, count) -> HealthStatusRow(reason.ifBlank { "未知原因" }, "$count") }
                    }
                }
            }
        }
        items(recentLogs.take(50), key = { it.id }) { log ->
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(log.eventType, color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text(formatDateTime(log.createdAtMillis), color = MutedText, fontSize = 12.sp)
                    if (log.message.isNotBlank()) Text(log.message, color = MutedText, fontSize = 13.sp)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PaymentTroubleshootingScreen(
    health: ListenerHealthStatus,
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenScreenshotPicker: () -> Unit,
    onOpenQuickBackfill: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("监听问题排查", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("按步骤检查通知权限、后台设置和付款通知捕获。",  color = MutedText)
        }

        item {
            TroubleshootingStep("步骤 1", "检查通知监听权限", health.notificationListenerPermission, onOpenNotificationSettings)
            TroubleshootingStep("步骤 2", "检查 App 通知权限", "手动检查", null)
            // repaired damaged text line
            // repaired damaged text line
            TroubleshootingStep("步骤 3", "检查付款应用通知", health.paymentNotificationSummary, null)
            // repaired damaged text line
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("排查建议",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("确保微信和支付宝允许显示付款通知。",  color = MutedText, fontSize = 13.sp)
                    Text("确保本应用通知监听权限已开启。",  color = MutedText, fontSize = 13.sp)
                    Text("路径：系统设置 > 通知 > 通知监听 > 本地自动记账", color = MutedText, fontSize = 13.sp)
                    Text("如果付款通知仍未捕获，可使用截图记账或补录。",  color = Orange, fontSize = 13.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("替代方案",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("自动监听异常时，可使用截图记账或刚刚付款补录。", color = MutedText, fontSize = 13.sp)
                    Text("截图记账适合已有付款截图。", color = MutedText, fontSize = 13.sp)
                    Text("刚刚付款补录适合手动录入最近付款。", color = MutedText, fontSize = 13.sp)
                    Text("补录不会影响已有账单数据。",  color = MutedText, fontSize = 13.sp)
                    Text("备份与恢复页面仍可导出和恢复账单。",  color = MutedText, fontSize = 13.sp)
                    Text("完成设置后建议发送测试通知。",  color = MutedText, fontSize = 13.sp)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenScreenshotPicker) {
                    Text("截图记账")
                }
                OutlinedButton(onClick = onOpenQuickBackfill) {
                    Text("刚刚付款补录")
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun RealNotificationTestScreen(
    logs: List<DebugNotificationLog>,
    testNotificationMessage: String,
    onBack: () -> Unit,
    onOpenLogDetail: (DebugNotificationLog) -> Unit,
    onSendAppTestNotification: (((Boolean, String) -> Unit) -> Unit)
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var appTestPassed by remember { mutableStateOf<Boolean?>(null) }
    var wechatTestStartTime by remember { mutableStateOf<Long?>(null) }
    var alipayTestStartTime by remember { mutableStateOf<Long?>(null) }
    var paymentTestStartTime by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val wechatWindowLogs = logs.inWindow(wechatTestStartTime, WECHAT_PACKAGE, NORMAL_TEST_WINDOW_MILLIS)
    val alipayWindowLogs = logs.inWindow(alipayTestStartTime, ALIPAY_PACKAGE, NORMAL_TEST_WINDOW_MILLIS)
    val paymentWindowLogs = logs.filter { log ->
        val start = paymentTestStartTime ?: return@filter false
        isPaymentPackage(log.packageName) &&
            log.receivedAtMillis >= start &&
            log.receivedAtMillis <= start + PAYMENT_TEST_WINDOW_MILLIS
    }.sortedByDescending { it.receivedAtMillis }
    val paymentWindowActive = paymentTestStartTime?.let { now - it <= PAYMENT_TEST_WINDOW_MILLIS } == true
    val latestPaymentRelated = paymentWindowLogs.firstOrNull { it.isPaymentRelated || it.hasAmount || it.isPaymentNotification }
    val failedPaymentParse = paymentWindowLogs.firstOrNull {
        (it.isPaymentRelated || it.hasAmount || it.isPaymentNotification) && !it.pendingCreated
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("真实通知测试",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("用于验证通知监听是否能收到 App、微信和支付宝通知。",  color = MutedText)
        }

        item {
            DiagnosticStepCard(
                title = "测试本 App 通知监听",
                body = "发送一条本地测试通知，验证 NotificationListenerService 是否能收到。",
                result = when (appTestPassed) {
                    true -> "本 App 测试通知已收到"
                    false -> "本 App 测试通知未收到"
                    null -> "未开始"
                },
                actionLabel = "发送 App 测试通知",
                onAction = {
                    appTestPassed = null
                    onSendAppTestNotification { passed, _ ->
                        appTestPassed = passed
                    }
                }
            ) {
                if (testNotificationMessage.isNotBlank()) {
                    Text(testNotificationMessage, color = if (appTestPassed == false) Red else Green, fontSize = 13.sp)
                }
            }
        }

        item {
            DiagnosticStepCard(
                title = "测试微信普通通知",
                body = "请在 30 秒内让微信产生一条普通通知，检查监听是否捕获 com.tencent.mm。",
                result = normalNotificationResult(
                    startTime = wechatTestStartTime,
                    now = now,
                    logs = wechatWindowLogs,
                    capturedText = "已收到微信通知",
                    missingText = "未收到微信通知，请检查微信通知权限和系统设置"
                ),
                actionLabel = "开始微信通知测试",
                onAction = { wechatTestStartTime = System.currentTimeMillis() }
            ) {
                NotificationWindowSummary(wechatTestStartTime, now, NORMAL_TEST_WINDOW_MILLIS)
                wechatWindowLogs.firstOrNull()?.let { log ->
                    CapturedNotificationCard(log, onOpenLogDetail)
                }
            }
        }

        item {
            DiagnosticStepCard(
                title = "测试支付宝普通通知",
                body = "请在 30 秒内让支付宝产生一条普通通知，检查监听是否捕获 com.eg.android.AlipayGphone。",
                result = normalNotificationResult(
                    startTime = alipayTestStartTime,
                    now = now,
                    logs = alipayWindowLogs,
                    capturedText = "已收到支付宝通知",
                    missingText = "未收到支付宝通知，请检查支付宝通知权限和系统设置"
                ),
                actionLabel = "开始支付宝通知测试",
                onAction = { alipayTestStartTime = System.currentTimeMillis() }
            ) {
                NotificationWindowSummary(alipayTestStartTime, now, NORMAL_TEST_WINDOW_MILLIS)
                alipayWindowLogs.firstOrNull()?.let { log ->
                    CapturedNotificationCard(log, onOpenLogDetail)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("付款通知测试",  color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("开始测试后，请在 120 秒内完成一笔微信或支付宝付款。",  color = MutedText, fontSize = 13.sp)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { paymentTestStartTime = System.currentTimeMillis() }
                    ) {
                        Text("开始付款通知测试")
                    }
                    NotificationWindowSummary(paymentTestStartTime, now, PAYMENT_TEST_WINDOW_MILLIS)
                    Text(
                        paymentTestConclusion(
                            appTestPassed = appTestPassed,
                            wechatCaptured = wechatWindowLogs.isNotEmpty(),
                            paymentStarted = paymentTestStartTime != null,
                            paymentActive = paymentWindowActive,
                            paymentLogs = paymentWindowLogs,
                            latestPaymentRelated = latestPaymentRelated,
                            failedPaymentParse = failedPaymentParse
                        ),
                        color = paymentConclusionColor(appTestPassed, paymentWindowLogs, failedPaymentParse),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (paymentWindowLogs.isEmpty() && paymentTestStartTime != null && !paymentWindowActive) {
                        Text("测试窗口内未捕获到付款通知。",  color = Orange, fontSize = 13.sp)
                    }
                    paymentWindowLogs.forEach { log ->
                        CapturedNotificationCard(log, onOpenLogDetail)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("结论说明", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("A. 本 App 测试通知都收不到：请先检查通知监听权限。", color = MutedText, fontSize = 13.sp)
                    Text("B. App 测试能收到，但微信/支付宝收不到：请检查付款应用通知权限。",  color = MutedText, fontSize = 13.sp)
                    Text("C. 普通通知能收到，但付款通知收不到：付款场景可能没有产生可监听通知，可使用截图记账或补录。", color = MutedText, fontSize = 13.sp)
                    Text("D. 收到付款通知但未生成账单：请查看 failReason 和解析日志。", color = MutedText, fontSize = 13.sp)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun WechatScanPaymentTestScreen(
    logs: List<DebugNotificationLog>,
    onBack: () -> Unit,
    onOpenLogDetail: (DebugNotificationLog) -> Unit,
    onOpenQuickBackfill: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var testStartTime by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val windowLogs = logs.inWindow(testStartTime, WECHAT_PACKAGE, PAYMENT_TEST_WINDOW_MILLIS)
    val active = testStartTime?.let { now - it <= PAYMENT_TEST_WINDOW_MILLIS } == true
    val conclusion = wechatScanTestConclusion(testStartTime, active, windowLogs)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("微信扫码支付测试", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("开始后请在 2 分钟内完成一笔微信扫码支付，页面会记录所有微信通知和金额解析诊断。", color = MutedText)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("测试模式", color = PrimaryText, fontWeight = FontWeight.Bold)
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = { testStartTime = System.currentTimeMillis() }
                    ) {
                        Text("开始 2 分钟测试")
                    }
                    NotificationWindowSummary(testStartTime, now, PAYMENT_TEST_WINDOW_MILLIS)
                    Text(
                        conclusion,
                        color = wechatScanConclusionColor(conclusion),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenQuickBackfill) {
                        Text("扫码支付后补录")
                    }
                }
            }
        }

        if (windowLogs.isEmpty() && testStartTime != null && !active) {
            item {
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = SoftRed)) {
                    Text(
                        "微信扫码支付没有产生可监听通知，APP 无法自动读取，需要使用快捷补录。",
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        color = Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (windowLogs.isNotEmpty()) {
            item {
                Text("捕获到的微信通知", color = PrimaryText, fontWeight = FontWeight.Bold)
            }
            items(windowLogs, key = { it.id }) { log ->
                WechatScanDiagnosticCard(log = log, onOpenLogDetail = onOpenLogDetail)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("结论规则", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("A. 没有捕获到 com.tencent.mm 通知：微信没有产生可监听通知，只能使用快捷补录。", color = MutedText, fontSize = 13.sp)
                    Text("B. 捕获到微信通知但不是支付相关：查看 rawText 和 failReason，用于后续扩展规则。", color = MutedText, fontSize = 13.sp)
                    Text("C. 捕获到支付通知但金额解析失败：查看 amountCandidates 和 selectedReason。", color = MutedText, fontSize = 13.sp)
                    Text("D. 捕获并解析成功：会自动生成待确认账单。", color = MutedText, fontSize = 13.sp)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun WechatScanDiagnosticCard(log: DebugNotificationLog, onOpenLogDetail: (DebugNotificationLog) -> Unit) {
    val parseResult = remember(log.id, log.packageName, log.title, log.rawText, log.postTime) {
        NotificationBillParser().parse(log.packageName, log.title, log.rawText, log.postTime)
    }
    val diagnostics = parseResult.diagnostics
    val amountCandidates = log.amountCandidates.ifBlank {
        diagnostics.amountCandidates.joinToString("\n") { "${it.text} -> ${formatMoney(it.amountCents)} / score=${it.score} / ${it.reason}" }
    }
    val selectedAmount = log.selectedAmount.ifBlank {
        diagnostics.selectedAmount?.let { formatMoney(it.amountCents) }.orEmpty()
    }
    val selectedReason = log.selectedReason.ifBlank { diagnostics.selectedAmount?.reason.orEmpty() }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenLogDetail(log) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PageBackground)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("微信", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Text(formatDateTime(log.receivedAtMillis), color = MutedText, fontSize = 12.sp)
            }
            DebugField("packageName", log.packageName)
            DebugField("title", log.title)
            DebugField("text", log.text)
            DebugField("subText", log.subText)
            DebugField("bigText", log.bigText)
            DebugField("textLines", log.textLines)
            DebugField("rawText", log.rawText)
            DebugField("postTime", formatOptionalDateTime(log.postTime))
            DebugField("notificationKey", log.notificationKey)
            DebugField("amountCandidates", amountCandidates)
            DebugField("selectedAmount", selectedAmount)
            DebugField("selectedReason", selectedReason)
            DebugField("isPaymentRelated", yesNo(log.isPaymentRelated || parseResult.isPaymentNotification))
            DebugField("failReason", log.failReason.ifBlank { log.failureReason.ifBlank { parseResult.failureReason } })
            if (log.pendingCreated) {
                Text("已生成待确认账单", color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DiagnosticStepCard(
    title: String,
    body: String,
    result: String,
    actionLabel: String,
    onAction: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(body, color = MutedText, fontSize = 13.sp)
            Text(result, color = statusColor(result), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Button(modifier = Modifier.fillMaxWidth(), onClick = onAction) {
                Text(actionLabel)
            }
            content()
        }
    }
}

@Composable
private fun NotificationWindowSummary(startTime: Long?, now: Long, windowMillis: Long) {
    if (startTime == null) {
        Text("检测窗口：未开始",  color = MutedText, fontSize = 12.sp)
        return
    }
    val elapsed = (now - startTime).coerceAtLeast(0L)
    val remaining = (windowMillis - elapsed).coerceAtLeast(0L)
    Text("开始时间：${formatDateTime(startTime)}", color = MutedText, fontSize = 12.sp)
    Text("剩余时间：${remaining / 1000} 秒", color = if (remaining > 0L) Green else Orange, fontSize = 12.sp)
}

@Composable
private fun CapturedNotificationCard(log: DebugNotificationLog, onOpenLogDetail: (DebugNotificationLog) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenLogDetail(log) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PageBackground)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(packageLabel(log.packageName), color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Text(formatDateTime(log.receivedAtMillis), color = MutedText, fontSize = 12.sp)
            }
            DebugField("title", log.title)
            DebugField("text", log.text)
            DebugField("rawText", log.rawText)
            Text("付款相关：${yesNo(log.isPaymentRelated)}  金额：${extractAmount(log.rawText)}", color = MutedText, fontSize = 12.sp)
            if (isNotificationContentHidden(log)) {
                Text("系统可能隐藏了通知详情，请开启通知内容显示。",  color = Orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (log.failReason.isNotBlank() || log.failureReason.isNotBlank()) {
                Text("failReason：${log.failReason.ifBlank { log.failureReason }}", color = Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NotificationRawTextDetailScreen(
    log: DebugNotificationLog,
    onBack: () -> Unit,
    onCreatePending: (DebugNotificationLog) -> Unit
) {
    val parseResult = remember(log.id, log.packageName, log.title, log.rawText, log.postTime) {
        NotificationBillParser().parse(
            packageName = log.packageName,
            title = log.title,
            text = log.rawText,
            postTimeMillis = log.postTime
        )
    }
    val diagnostics = parseResult.diagnostics
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("通知详情", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailField("packageName", log.packageName)
                    DetailField("title", log.title)
                    DetailField("text", log.text)
                    DetailField("subText", log.subText)
                    DetailField("bigText", log.bigText)
                    DetailField("textLines", log.textLines)
                    DetailField("rawText", log.rawText)
                    DetailField("notificationKey", log.notificationKey)
                    DetailField("matchedKeywords", diagnostics.matchedKeywords.joinToString(" / "))
                    DetailField(
                        "amountCandidates",
                        log.amountCandidates.ifBlank {
                            diagnostics.amountCandidates.joinToString("\n") {
                                "${it.text} -> ${formatMoney(it.amountCents)} / score=${it.score} / ${it.reason}"
                            }
                        }
                    )
                    // repaired damaged text line
                    DetailField("最终选择金额", log.selectedAmount.ifBlank { diagnostics.selectedAmount?.let { formatMoney(it.amountCents) }.orEmpty() })
                    DetailField("selectedReason", log.selectedReason.ifBlank { diagnostics.selectedAmount?.reason.orEmpty() })
                    // repaired damaged text line
                    DetailField(
                        "商户识别",
                        diagnostics.merchantName.ifBlank { "Unknown" } + " / " + diagnostics.merchantReason
                    )
                    // repaired damaged text line
                    DetailField("isPaymentRelated", yesNo(log.isPaymentRelated))
                    DetailField("failReason", log.failReason.ifBlank { log.failureReason })
                    DetailField("parseStatus", log.parseStatus)
                    DetailField("receivedAt", formatDateTime(log.receivedAtMillis))
                    if (isNotificationContentHidden(log)) {
                        Text("系统可能隐藏了通知详情，请开启通知内容显示。",  color = Orange, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (!log.pendingCreated) {
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onCreatePending(log) }
                ) {
                    Text("从这条通知生成账单")
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MutedText, fontSize = 12.sp)
        Text(value.ifBlank { "暂无" },  color = if (value.isBlank()) Orange else PrimaryText, fontSize = 13.sp)
    }
}

@Composable
private fun TroubleshootingStep(
    step: String,
    title: String,
    result: String,
    action: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$step. $title", color = PrimaryText, fontWeight = FontWeight.SemiBold)
            Text(result, color = statusColor(result), fontSize = 13.sp)
            if (action != null) {
                OutlinedButton(onClick = action) {
                    Text("去设置")
                }
            }
        }
    }
}

@Composable
private fun QuickBackfillScreen(
    preset: QuickBackfillPreset,
    onBack: () -> Unit,
    onSave: (Long, TransactionType, String, String, String, Long, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var type by remember(preset.title) { mutableStateOf(preset.type) }
    var category by remember { mutableStateOf(ExpenseCategories.expense.first()) }
    var merchantName by remember { mutableStateOf("") }
    var sourceApp by remember(preset.title) { mutableStateOf(preset.sourceApp) }
    var note by remember(preset.title) { mutableStateOf(preset.note) }
    val initialPaidAtMillis = remember(preset.title) { System.currentTimeMillis() }
    var dateText by remember(preset.title) { mutableStateOf(formatDate(initialPaidAtMillis)) }
    var error by remember { mutableStateOf("") }
    val categories = when (type) {
        TransactionType.INCOME -> ExpenseCategories.income
        TransactionType.EXPENSE -> ExpenseCategories.expense
        TransactionType.UNKNOWN -> ExpenseCategories.expense
    }.let { (listOf(category) + it).distinct() }
    val sources = listOf("微信", "支付宝", "手动", "通知")

    LaunchedEffect(type) {
        if (category !in categories) {
            category = when (type) {
                TransactionType.INCOME -> ExpenseCategories.income.first()
                TransactionType.EXPENSE -> ExpenseCategories.expense.first()
                TransactionType.UNKNOWN -> ExpenseCategories.expense.first()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text(preset.title,  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("金额") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text("类型",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TypeChoice("支出", TransactionType.EXPENSE, type) { type = it }
                        TypeChoice("收入", TransactionType.INCOME, type) { type = it }
                    }
                    CategoryDropdown(category = category, categories = categories, onCategoryChange = { category = it })
                    OutlinedTextField(
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("商户") },
                        singleLine = true
                    )
                    SimpleDropdown(
                        label = "来源",
                        value = sourceApp,
                        options = sources,
                        onValueChange = { sourceApp = it }
                    )
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("日期 yyyy-MM-dd") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("备注") },
                        singleLine = true
                    )
                    if (error.isNotBlank()) {
                        Text(error, color = Red, fontSize = 13.sp)
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            val amountCents = parseAmountCents(amount)
                            val parsedDateMillis = parseDateMillis(dateText)
                            val paidAtMillis = if (dateText == formatDate(initialPaidAtMillis)) initialPaidAtMillis else parsedDateMillis
                            when {
                                amountCents == null || amountCents <= 0 -> error = "请输入大于 0 的金额"
                                paidAtMillis == null -> error = "时间格式应为 yyyy-MM-dd"
                                else -> onSave(amountCents, type, category, merchantName.trim(), sourceApp, paidAtMillis, note.trim())
                            }
                        }
                    ) {
                        Text("保存补录")
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassificationRulesScreen(
    rules: List<ClassificationRule>,
    onBack: () -> Unit,
    onAddRule: (String, String, String, TransactionType, Int) -> Unit,
    onUpdateRule: (ClassificationRule, String, String, String, TransactionType, Int) -> Unit,
    onToggleRule: (ClassificationRule, Boolean) -> Unit,
    onDeleteRule: (ClassificationRule) -> Unit
) {
    var editingRule by remember { mutableStateOf<ClassificationRule?>(null) }
    var ruleName by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(ExpenseCategories.expense.first()) }
    var priorityText by remember { mutableStateOf("10") }
    var error by remember { mutableStateOf("") }
    val categories = when (type) {
        TransactionType.INCOME -> ExpenseCategories.income
        TransactionType.EXPENSE -> ExpenseCategories.expense
        TransactionType.UNKNOWN -> ExpenseCategories.expense
    }.let { (listOf(category) + it).distinct() }

    LaunchedEffect(type) {
        if (category !in categories) {
            category = when (type) {
                TransactionType.INCOME -> ExpenseCategories.income.first()
                TransactionType.EXPENSE -> ExpenseCategories.expense.first()
                TransactionType.UNKNOWN -> ExpenseCategories.expense.first()
            }
        }
    }

    fun resetForm() {
        editingRule = null
        ruleName = ""
        keyword = ""
        type = TransactionType.EXPENSE
        category = ExpenseCategories.expense.first()
        priorityText = "10"
        error = ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("分类规则", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("通知内容命中关键词后，会自动填入分类和收支类型。",  color = MutedText)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (editingRule == null) "新增规则" else "编辑规则",
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = {
                            ruleName = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("规则名称") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = {
                            keyword = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("关键词") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priorityText,
                        onValueChange = {
                            priorityText = it.filter { ch -> ch.isDigit() }
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("优先级") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("收支类型",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TypeChoice("支出", TransactionType.EXPENSE, type) { type = it }
                        TypeChoice("收入", TransactionType.INCOME, type) { type = it }
                    }
                    CategoryDropdown(
                        category = category,
                        categories = categories,
                        onCategoryChange = { category = it }
                    )
                    if (error.isNotBlank()) {
                        Text(error, color = Red, fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val trimmedKeyword = keyword.trim()
                                val trimmedRuleName = ruleName.trim().ifBlank { trimmedKeyword.take(20) }
                                val priority = priorityText.toIntOrNull()
                                when {
                                    trimmedKeyword.isBlank() -> error = "请输入关键词"
                                    priority == null -> error = "请输入有效优先级"
                                    category.isBlank() -> error = "请选择分类"
                                    editingRule == null -> {
                                        onAddRule(trimmedRuleName, trimmedKeyword, category, type, priority)
                                        resetForm()
                                    }
                                    else -> {
                                        onUpdateRule(editingRule!!, trimmedRuleName, trimmedKeyword, category, type, priority)
                                        resetForm()
                                    }
                                }
                            }
                        ) {
                            Text(if (editingRule == null) "新增规则" else "保存规则")
                        }
                        if (editingRule != null) {
                            OutlinedButton(onClick = { resetForm() }) {
                                Text("取消编辑")
                            }
                        }
                    }
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "暂无分类规则",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                ClassificationRuleRow(
                    rule = rule,
                    onToggleRule = onToggleRule,
                    onEdit = {
                        editingRule = it
                        ruleName = it.ruleName
                        keyword = it.keyword
                        type = it.type
                        category = it.category
                        priorityText = it.priority.toString()
                        error = ""
                    },
                    onDeleteRule = onDeleteRule
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ClassificationRuleRow(
    rule: ClassificationRule,
    onToggleRule: (ClassificationRule, Boolean) -> Unit,
    onEdit: (ClassificationRule) -> Unit,
    onDeleteRule: (ClassificationRule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(rule.ruleName.ifBlank { rule.keyword }, color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("${transactionTypeLabel(rule.type)} / ${rule.category} / 优先级 ${rule.priority}", color = MutedText, fontSize = 13.sp)
                    Text("关键词：${rule.keyword}", color = MutedText, fontSize = 12.sp)
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggleRule(rule, it) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(rule) }) {
                    Text("编辑")
                }
                OutlinedButton(onClick = { onDeleteRule(rule) }) {
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun NotificationDebugLogsScreen(
    logs: List<DebugNotificationLog>,
    onBack: () -> Unit,
    testNotificationMessage: String,
    onSimulate: (String) -> Unit,
    onSendTestNotification: () -> Unit
) {
    var filter by remember { mutableStateOf(DebugLogFilter.ALL) }
    var simulationText by remember { mutableStateOf("微信支付 付款给 商户 ¥2.80") }
    var simulationMessage by remember { mutableStateOf("") }
    val visibleLogs = logs.filter { log ->
        filter.packageName == null || log.packageName == filter.packageName
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("通知日志",  color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("最近 100 条通知会记录在 debug_notification_logs 表。",  color = MutedText)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("模拟付款通知解析",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = simulationText,
                        onValueChange = {
                            simulationText = it
                            simulationMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("通知文本") },
                        minLines = 2
                    )
                    Text("测试案例", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    SimulationExamples { selected ->
                        simulationText = selected
                        simulationMessage = ""
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        onClick = {
                            onSimulate(simulationText)
                            simulationMessage = "已提交模拟解析，可查看日志和待确认账单。"
                        }
                    ) {
                        Text("模拟付款通知解析")
                    }
                    if (simulationMessage.isNotBlank()) {
                        Text(simulationMessage, color = Green, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        onClick = onSendTestNotification
                    ) {
                        Text("发送测试通知")
                    }
                    Text("如果测试通知发出后日志里没有出现本 App 通知，说明通知监听服务没有生效。",  color = MutedText, fontSize = 13.sp)
                    if (testNotificationMessage.isNotBlank()) {
                        Text(testNotificationMessage, color = Green, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DebugFilterButton("全部", filter == DebugLogFilter.ALL) { filter = DebugLogFilter.ALL }
                DebugFilterButton("微信", filter == DebugLogFilter.WECHAT) { filter = DebugLogFilter.WECHAT }
                DebugFilterButton("支付宝", filter == DebugLogFilter.ALIPAY) { filter = DebugLogFilter.ALIPAY }
            }
        }

        if (visibleLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "暂无通知日志",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(visibleLogs, key = { it.id }) { log ->
                NotificationDebugLogRow(log)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SimulationExamples(onSelect: (String) -> Unit) {
    val examples = listOf(
        "微信支付 付款给 商户",
        "支付宝 付款成功",
        "你有一笔收款到账",
        "微信支付 ¥2.80",
        "支付宝付款 36.00 元",
        "微信支付\n收款方 星巴克 付款 ¥8.50",
        "支付宝付款\n订单金额 36.00 元"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        examples.forEach { example ->
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onSelect(example) }) {
                Text(example)
            }
        }
    }
}

@Composable
private fun DebugFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun NotificationDebugLogRow(log: DebugNotificationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(log.packageName, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(log.parseStatus, color = parseStatusColor(log.parseStatus), fontSize = 12.sp)
            }
            Text("收到：${formatDateTime(log.receivedAtMillis)}", color = MutedText, fontSize = 12.sp)
            Text("postTime：${formatDateTime(log.postTime)}", color = MutedText, fontSize = 12.sp)
            Text("来自支付 App：${yesNo(log.isFromPaymentApp)}", color = MutedText, fontSize = 12.sp)
            Text("付款相关：${yesNo(log.isPaymentRelated)}", color = MutedText, fontSize = 12.sp)
            Text("已解析：${yesNo(log.isParsed)}", color = MutedText, fontSize = 12.sp)
            Text("服务存活：${yesNo(log.serviceAlive)}", color = MutedText, fontSize = 12.sp)
            Text("App 后台：${yesNo(log.appInBackground)}", color = MutedText, fontSize = 12.sp)
            Text("锁屏状态：${yesNo(log.screenLocked)}", color = MutedText, fontSize = 12.sp)
            Text("付款通知：${yesNo(log.isPaymentNotification)}", color = MutedText, fontSize = 12.sp)
            Text("识别金额：${yesNo(log.hasAmount)}", color = MutedText, fontSize = 12.sp)
            Text("命中规则：${yesNo(log.ruleMatched)}", color = MutedText, fontSize = 12.sp)
            // repaired damaged text line
            Text("confidence：${log.confidence}", color = MutedText, fontSize = 12.sp)
            // repaired damaged text line
            Text("生成待处理账单：${yesNo(log.pendingCreated)}", color = MutedText, fontSize = 12.sp)
            DebugField("title", log.title)
            DebugField("text", log.text)
            DebugField("subText", log.subText)
            DebugField("bigText", log.bigText)
            DebugField("textLines", log.textLines)
            if (log.rawText.isBlank()) {
                Text("rawText 为空",  color = Red, fontSize = 12.sp)
            } else {
                Text("rawText：${log.rawText.take(260)}", color = PrimaryText, fontSize = 12.sp)
            }
            if (log.parseReason.isNotBlank()) {
                Text("解析原因：${log.parseReason}", color = MutedText, fontSize = 12.sp)
            }
            if (log.failureReason.isNotBlank()) {
                Text("未生成原因：${log.failureReason}", color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (log.failReason.isNotBlank() && log.failReason != log.failureReason) {
                Text("诊断失败原因：${log.failReason}", color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DebugField(label: String, value: String) {
    if (value.isNotBlank()) {
        Text("$label: ${value.take(160)}", color = MutedText, fontSize = 12.sp)
    }
}

private fun parseStatusColor(parseStatus: String): Color = when {
    parseStatus.contains("PENDING_CREATED") || parseStatus.contains("PENDING_CONFIRM") -> Green
    parseStatus.contains("NEED_AMOUNT") || parseStatus == "need_amount" -> Orange
    parseStatus == "RECEIVED" || parseStatus == "SIMULATED" -> MutedText
    else -> Red
}

private fun yesNo(value: Boolean): String = if (value) "?" else "?"

private fun transactionTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.INCOME -> "收入"
    TransactionType.EXPENSE -> "支出"
    TransactionType.UNKNOWN -> "待选择"
}

private fun formatAmountCandidates(candidates: List<AmountCandidate>): String =
    candidates.takeIf { it.isNotEmpty() }
        ?.joinToString("\n") { candidate ->
            "${formatMoney(candidate.amountCents)}，文本 ${candidate.text}，score ${candidate.score}，原因 ${candidate.reason}"
        }
        .orEmpty()

@Composable
private fun CategoryBubble(label: String, isIncome: Boolean) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isIncome) SoftGreen else SoftRed),
        contentAlignment = Alignment.Center
    ) {
        Text(label.take(1), color = if (isIncome) Green else Red, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecordFormScreen(
    title: String,
    initialRecord: ExpenseRecord?,
    merchantLearnings: List<MerchantCategoryLearning> = emptyList(),
    onBack: () -> Unit,
    onSave: (Long, TransactionType, String, String, String, Long) -> Unit
) {
    var amount by remember(initialRecord?.id) {
        mutableStateOf(
            initialRecord
                ?.takeUnless { it.status == RecordStatus.NEED_AMOUNT && it.amountCents == 0L }
                ?.let { formatYuanInput(it.amountCents) }
                .orEmpty()
        )
    }
    var type by remember(initialRecord?.id) { mutableStateOf(initialRecord?.type ?: TransactionType.EXPENSE) }
    var category by remember(initialRecord?.id) {
        mutableStateOf(initialRecord?.category ?: ExpenseCategories.expense.first())
    }
    var merchantName by remember(initialRecord?.id) { mutableStateOf(initialRecord?.merchantName.orEmpty()) }
    var note by remember(initialRecord?.id) { mutableStateOf(initialRecord?.note.orEmpty()) }
    var dateText by remember(initialRecord?.id) {
        mutableStateOf(formatDate(initialRecord?.paidAtMillis ?: System.currentTimeMillis()))
    }
    var error by remember { mutableStateOf("") }
    val defaultCategories = when (type) {
        TransactionType.INCOME -> ExpenseCategories.income
        TransactionType.EXPENSE -> ExpenseCategories.expense
        TransactionType.UNKNOWN -> listOf("待选择") + ExpenseCategories.expense + ExpenseCategories.income
    }
    val categories = (listOf(category) + defaultCategories).distinct()
    val merchantSuggestions = remember(merchantName, merchantLearnings) {
        MerchantLearningMatcher.candidates(merchantName, merchantLearnings, limit = 4)
    }

    LaunchedEffect(type) {
        if (category !in defaultCategories) category = defaultCategories.first()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButtonLike("返回", onBack)
            Spacer(Modifier.width(12.dp))
            Text(title, color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text("类型",  color = PrimaryText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TypeChoice("支出", TransactionType.EXPENSE, type) { type = it }
                    TypeChoice("收入", TransactionType.INCOME, type) { type = it }
                }
                if (type == TransactionType.UNKNOWN) {
                    Text("请选择收入或支出",  color = Orange, fontSize = 13.sp)
                }

                CategoryDropdown(
                    category = category,
                    categories = categories,
                    onCategoryChange = { category = it }
                )

                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("商户") },
                    singleLine = true
                )
                if (merchantSuggestions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("历史商户", color = MutedText, fontSize = 13.sp)
                        merchantSuggestions.forEach { learning ->
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    merchantName = learning.merchantDisplayName
                                    category = learning.category
                                    if (type == TransactionType.UNKNOWN) type = TransactionType.EXPENSE
                                }
                            ) {
                                Text("${learning.merchantDisplayName} → ${learning.category}")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("日期 yyyy-MM-dd") },
                    singleLine = true
                )

                if (error.isNotBlank()) {
                    Text(error, color = Red, fontSize = 13.sp)
                }

                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        val amountCents = parseAmountCents(amount)
                        val paidAtMillis = parseDateMillis(dateText)
                        when {
                            amountCents == null || amountCents <= 0 -> error = "请输入大于 0 的金额"
                            type == TransactionType.UNKNOWN -> error = "请选择收入或支出"
                            paidAtMillis == null -> error = "时间格式应为 yyyy-MM-dd"
                            else -> onSave(amountCents, type, category, merchantName.trim(), note.trim(), paidAtMillis)
                        }
                    }
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun LearningRecordsScreen(
    learnings: List<MerchantCategoryLearning>,
    onBack: () -> Unit,
    onToggleLearning: (MerchantCategoryLearning, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButtonLike("返回", onBack)
                Spacer(Modifier.width(12.dp))
                Text("分类学习记录", color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("来自你确认账单时填写的商户和分类，可禁用错误记录。", color = MutedText, fontSize = 13.sp)
        }
        if (learnings.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text(
                        "暂无学习记录",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(learnings, key = { it.id }) { learning ->
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(learning.merchantDisplayName.ifBlank { learning.merchantNormalized }, color = PrimaryText, fontWeight = FontWeight.Bold)
                                Text("分类：${learning.category}", color = MutedText, fontSize = 13.sp)
                            }
                            Switch(
                                checked = learning.isEnabled,
                                onCheckedChange = { onToggleLearning(learning, it) }
                            )
                        }
                        HealthStatusRow("使用次数", "${learning.useCount}")
                        HealthStatusRow("来源", learning.sourceApp.ifBlank { "unknown" })
                        HealthStatusRow("最近使用", formatDateTime(learning.lastUsedAt))
                        HealthStatusRow("匹配关键词", learning.matchKeyword.ifBlank { "暂无" })
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TextButtonLike(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier.clickable { onClick() }.padding(vertical = 8.dp),
        color = Green,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun TypeChoice(
    label: String,
    value: TransactionType,
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected == value, onClick = { onSelected(value) })
        Text(label, color = PrimaryText, modifier = Modifier.clickable { onSelected(value) })
    }
}

@Composable
private fun CategoryDropdown(
    category: String,
    categories: List<String>,
    onCategoryChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("分类：$category")
                Text("选择")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onCategoryChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SimpleDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$label: $value")
                Text("选择")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatYuanInput(cents: Long): String = String.format(Locale.US, "%.2f", cents / 100.0)

private fun parseAmountCents(text: String): Long? =
    text.trim()
        .replace(",", "")
        .toBigDecimalOrNull()
        ?.multiply(BigDecimal(100))
        ?.setScale(0, RoundingMode.HALF_UP)
        ?.toLong()

private fun parseDateMillis(text: String): Long? =
    runCatching {
        LocalDate.parse(text.trim(), DateFormatter)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateFormatter)

private fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeDisplayFormatter)

private fun latestTodayExpenseRecord(records: List<ExpenseRecord>, nowMillis: Long = System.currentTimeMillis()): ExpenseRecord? {
    val zoneId = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val fromMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val toMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return records
        .filter { it.status == RecordStatus.CONFIRMED }
        .filterNot { it.isDeleted }
        .filter { it.type == TransactionType.EXPENSE }
        .filter { it.paidAtMillis >= fromMillis && it.paidAtMillis < toMillis }
        .maxWithOrNull(compareBy<ExpenseRecord> { it.updatedAtMillis }.thenBy { it.id })
}

private fun buildListenerHealthStatus(
    context: Context,
    logs: List<DebugNotificationLog>,
    refreshKey: Long = 0L
): ListenerHealthStatus {
    refreshKey.hashCode()
    val listenerPermission = NotificationListenerState.isPermissionEnabled(context)
    val runtimeState = NotificationListenerState.current(context)
    val autoEnabled = KeepAliveNotificationService.isEnabled(context)
    val foregroundRunning = KeepAliveNotificationService.isRunning(context)
    val probeSnapshot = com.localbookkeeping.app.notification.ListenerProbeNotification.snapshot(context)
    val evaluation = ListenerHealthEvaluator.evaluate(
        ListenerHealthInput(
            notificationPermissionEnabled = listenerPermission,
            listenerConnected = runtimeState.listenerConnected,
            rawListenerConnected = runtimeState.rawListenerConnected,
            lastDisconnectedAt = runtimeState.lastDisconnectedTime,
            lastHeartbeatAt = runtimeState.lastHeartbeatTime,
            autoListenEnabled = autoEnabled,
            foregroundServiceRunning = foregroundRunning,
            testNotificationFailed = probeSnapshot.latestProbeFailed
        )
    )
    val listenerActive = evaluation.status == ListenerServiceStatus.HEALTHY
    val recoverySnapshot = ListenerRecoveryState.snapshot(context)
    val appNotificationsEnabled = TestPaymentNotificationSender.areAppNotificationsEnabled(context) &&
        TestPaymentNotificationSender.canPostNotifications(context)
    val powerManager = context.getSystemService(PowerManager::class.java)
    val ignoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    val lastWechat = logs.firstOrNull { it.packageName == "com.tencent.mm" }
    val lastAlipay = logs.firstOrNull { it.packageName == "com.eg.android.AlipayGphone" }
    val lastSuccess = logs.firstOrNull { it.pendingCreated }
    val paymentSummary = when {
        lastWechat == null && lastAlipay == null -> "最近 100 条日志没有微信/支付宝通知"
        else -> listOfNotNull(
            lastWechat?.let { "微信：${formatDateTime(it.receivedAtMillis)}" },
            lastAlipay?.let { "支付宝：${formatDateTime(it.receivedAtMillis)}" }
        ).joinToString(" / ")
    }
    return ListenerHealthStatus(
        listenerServiceActive = listenerActive,
        notificationListenerPermission = if (listenerPermission) "已授权" else "未授权",
        listenerServiceStatus = listenerStatusLabel(evaluation.status),
        listenerServiceReasons = evaluation.reasons.joinToString(" / ").ifBlank { "?" },
        appNotificationPermission = if (appNotificationsEnabled) "已授权" else "未授权",
        wechatNotificationPermission = "需手动检查",
        alipayNotificationPermission = "需手动检查",
        backgroundRunStatus = if (autoEnabled) "已启用" else "未启用",
        foregroundServiceStatus = if (foregroundRunning) "运行中" else "未运行",
        batteryOptimizationStatus = if (ignoringBatteryOptimizations) "已忽略电池优化" else "可能受电池优化限制",
        lastConnectedAt = formatOptionalDateTime(runtimeState.lastConnectedTime),
        lastDisconnectedAt = formatOptionalDateTime(runtimeState.lastDisconnectedTime),
        lastHeartbeatAt = formatOptionalDateTime(runtimeState.lastHeartbeatTime),
        lastNotificationAt = formatOptionalDateTime(runtimeState.lastNotificationTime),
        lastPaymentNotificationAt = formatOptionalDateTime(runtimeState.lastPaymentNotificationTime),
        lastNotificationPackage = runtimeState.lastNotificationPackage.ifBlank { "暂无" },
        lastWechatNotificationAt = formatOptionalDateTime(runtimeState.lastWechatNotificationTime.takeIf { it > 0L } ?: lastWechat?.receivedAtMillis ?: 0L),
        lastAlipayNotificationAt = formatOptionalDateTime(runtimeState.lastAlipayNotificationTime.takeIf { it > 0L } ?: lastAlipay?.receivedAtMillis ?: 0L),
        lastSuccessfulAccountingAt = lastSuccess?.let { formatDateTime(it.receivedAtMillis) } ?: "暂无",
        currentHealthStatus = listenerStatusLabel(evaluation.status),
        lastHealthCheckAt = formatOptionalDateTime(recoverySnapshot.lastHealthCheckAt),
        lastAutoRepairAt = formatOptionalDateTime(recoverySnapshot.lastAutoRepairAt),
        autoRepairCount = recoverySnapshot.autoRepairCount,
        lastRebindResult = recoverySnapshot.lastRebindResult.ifBlank { "暂无" },
        lastFailureReason = recoverySnapshot.lastFailureReason.ifBlank { "暂无" },
        needsListenerRecheck = recoverySnapshot.needsListenerRecheck,
        lastPackageReplacedAt = formatOptionalDateTime(recoverySnapshot.lastPackageReplacedAt),
        paymentNotificationSummary = paymentSummary
    )
}

private fun listenerStatusLabel(status: ListenerServiceStatus): String = when (status) {
    ListenerServiceStatus.HEALTHY -> "监听正常"
    ListenerServiceStatus.SUSPICIOUS -> "监听疑似失效"
    ListenerServiceStatus.DISCONNECTED -> "已断开"
    ListenerServiceStatus.PERMISSION_MISSING -> "权限缺失"
    ListenerServiceStatus.SERVICE_UNKNOWN -> "未知"
}

private fun listenerStatusColor(status: ListenerServiceStatus): Color = when (status) {
    ListenerServiceStatus.HEALTHY -> Green
    ListenerServiceStatus.SUSPICIOUS -> Orange
    ListenerServiceStatus.DISCONNECTED -> Red
    ListenerServiceStatus.PERMISSION_MISSING -> Red
    ListenerServiceStatus.SERVICE_UNKNOWN -> Orange
}

private fun statusColor(value: String): Color = when {
    value.contains("Granted", ignoreCase = true) ||
        value.contains("Enabled", ignoreCase = true) ||
        value.contains("Running", ignoreCase = true) ||
        value.contains("Connected", ignoreCase = true) ||
        value.contains("已授权") ||
        value.contains("已启用") ||
        value.contains("运行中") ||
        value.contains("监听正常") ||
        value.contains("成功") -> Green
    value.contains("Fail", ignoreCase = true) ||
        value.contains("Missing", ignoreCase = true) ||
        value.contains("Disabled", ignoreCase = true) ||
        value.contains("Stopped", ignoreCase = true) ||
        value.contains("未授权") ||
        value.contains("未启用") ||
        value.contains("未运行") ||
        value.contains("失败") ||
        value.contains("异常") ||
        value.contains("缺失") -> Red
    else -> MutedText
}

private fun formatOptionalDateTime(millis: Long): String =
    if (millis > 0L) formatDateTime(millis) else "暂无"

private fun List<DebugNotificationLog>.inWindow(
    startTime: Long?,
    packageName: String,
    windowMillis: Long
): List<DebugNotificationLog> {
    val start = startTime ?: return emptyList()
    return filter {
        it.packageName == packageName &&
            it.receivedAtMillis >= start &&
            it.receivedAtMillis <= start + windowMillis
    }.sortedByDescending { it.receivedAtMillis }
}

private fun normalNotificationResult(
    startTime: Long?,
    now: Long,
    logs: List<DebugNotificationLog>,
    capturedText: String,
    missingText: String
): String {
    if (startTime == null) return "未开始"
    if (logs.isNotEmpty()) return capturedText
    return if (now - startTime <= NORMAL_TEST_WINDOW_MILLIS) "检测中" else missingText
}

private fun paymentTestConclusion(
    appTestPassed: Boolean?,
    wechatCaptured: Boolean,
    paymentStarted: Boolean,
    paymentActive: Boolean,
    paymentLogs: List<DebugNotificationLog>,
    latestPaymentRelated: DebugNotificationLog?,
    failedPaymentParse: DebugNotificationLog?
): String = when {
    appTestPassed == false -> "A. 本 App 测试通知未收到，请先检查通知监听权限"
    appTestPassed != true -> "请先发送 App 测试通知"
    !wechatCaptured -> "B. 本 App 测试通知能收到，但微信普通通知收不到，请检查微信通知权限或系统设置"
    !paymentStarted -> "请开始付款通知测试"
    paymentLogs.isEmpty() && paymentActive -> "等待付款通知中，请在测试窗口内完成付款"
    paymentLogs.isEmpty() -> "C. 普通通知能收到，但付款通知收不到，建议使用截图记账或刚刚付款补录"
    failedPaymentParse != null -> "D. 已收到付款通知，但未生成账单，请查看 failReason"
    latestPaymentRelated != null -> "付款通知已收到，并被识别为付款相关通知"
    else -> "收到微信/支付宝通知，但暂未判断为付款通知，请查看 rawText"
}

private fun paymentConclusionColor(
    appTestPassed: Boolean?,
    paymentLogs: List<DebugNotificationLog>,
    failedPaymentParse: DebugNotificationLog?
): Color = when {
    appTestPassed == false -> Red
    failedPaymentParse != null -> Orange
    paymentLogs.any { it.pendingCreated || it.isPaymentRelated || it.hasAmount || it.isPaymentNotification } -> Green
    else -> MutedText
}

private fun wechatScanTestConclusion(
    startTime: Long?,
    active: Boolean,
    logs: List<DebugNotificationLog>
): String {
    if (startTime == null) return "未开始"
    if (logs.isEmpty() && active) return "等待微信扫码支付通知中"
    if (logs.isEmpty()) return "微信扫码支付没有产生可监听通知，APP 无法自动读取，需要使用快捷补录。"

    val parsed = logs.firstOrNull { it.pendingCreated }
    if (parsed != null) return "D. 捕获到微信支付通知并解析成功，已生成待确认账单。"

    val paymentRelated = logs.firstOrNull { log ->
        log.isPaymentRelated || log.isPaymentNotification ||
            NotificationBillParser().parse(log.packageName, log.title, log.rawText, log.postTime).isPaymentNotification
    }
    if (paymentRelated == null) {
        val latest = logs.first()
        val failReason = latest.failReason.ifBlank { latest.failureReason.ifBlank { "未识别为支付相关通知" } }
        return "B. 捕获到微信通知，但不是支付相关。failReason：$failReason"
    }

    if (!paymentRelated.hasAmount) {
        val failReason = paymentRelated.failReason.ifBlank { paymentRelated.failureReason.ifBlank { "金额解析失败" } }
        val candidates = paymentRelated.amountCandidates.ifBlank { "无可靠金额候选" }
        return "C. 捕获到微信支付通知，但金额解析失败。amountCandidates：$candidates；failReason：$failReason"
    }

    return "已捕获微信支付通知并识别金额，但未生成待确认账单，可能被重复记录过滤；请查看 failReason。"
}

private fun wechatScanConclusionColor(conclusion: String): Color = when {
    conclusion.startsWith("D.") -> Green
    conclusion.startsWith("C.") || conclusion.startsWith("B.") -> Orange
    conclusion.contains("没有产生可监听通知") -> Red
    conclusion.contains("等待") || conclusion == "未开始" -> MutedText
    else -> Orange
}

private fun isPaymentPackage(packageName: String): Boolean =
    packageName == WECHAT_PACKAGE || packageName == ALIPAY_PACKAGE

private fun packageLabel(packageName: String): String = when (packageName) {
    WECHAT_PACKAGE -> "微信"
    ALIPAY_PACKAGE -> "支付宝"
    else -> packageName
}

private fun extractAmount(rawText: String): String =
    NotificationBillParser()
        .parse(WECHAT_PACKAGE, "", rawText)
        .diagnostics
        .selectedAmount
        ?.amountCents
        ?.let { formatMoney(it) }
        ?: "未识别"

private fun isNotificationContentHidden(log: DebugNotificationLog): Boolean {
    val raw = log.rawText.trim()
    return raw.isBlank() || (!NotificationAmountRegex.containsMatchIn(raw) && raw.length < 8)
}

private suspend fun recognizeTextFromImage(context: Context, uri: Uri): String =
    withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        recognizer.process(image).await().text
    }

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        }.getOrNull()
    }

private fun isNotificationListenerEnabled(context: Context): Boolean =
    NotificationListenerState.isPermissionEnabled(context)

private enum class MainTab {
    BOOKKEEPING,
    STATS,
    LIMIT,
    LISTENER
}

private enum class AppScreen {
    LEDGER,
    ADD,
    RECORD_DETAIL,
    EDIT_CONFIRMED,
    PENDING,
    EDIT_PENDING,
    DEBUG_LOGS,
    STATS,
    BACKUP,
    REAL_NOTIFICATION_TEST,
    WECHAT_SCAN_TEST,
    NOTIFICATION_RAW_DETAIL,
    HEALTH,
    BACKGROUND_SETTINGS,
    BACKGROUND_REPORT,
    TROUBLESHOOTING,
    QUICK_BACKFILL,
    RULES,
    LEARNING,
    SCREENSHOT_PREVIEW
}

private data class ListenerHealthStatus(
    val listenerServiceActive: Boolean,
    val notificationListenerPermission: String,
    val listenerServiceStatus: String,
    val listenerServiceReasons: String,
    val appNotificationPermission: String,
    val wechatNotificationPermission: String,
    val alipayNotificationPermission: String,
    val backgroundRunStatus: String,
    val foregroundServiceStatus: String,
    val batteryOptimizationStatus: String,
    val lastConnectedAt: String,
    val lastDisconnectedAt: String,
    val lastHeartbeatAt: String,
    val lastNotificationAt: String,
    val lastPaymentNotificationAt: String,
    val lastNotificationPackage: String,
    val lastWechatNotificationAt: String,
    val lastAlipayNotificationAt: String,
    val lastSuccessfulAccountingAt: String,
    val currentHealthStatus: String,
    val lastHealthCheckAt: String,
    val lastAutoRepairAt: String,
    val autoRepairCount: Int,
    val lastRebindResult: String,
    val lastFailureReason: String,
    val needsListenerRecheck: Boolean,
    val lastPackageReplacedAt: String,
    val paymentNotificationSummary: String
)

private data class ListenerRescueUiState(
    val running: Boolean = false,
    val permissionResult: String = "未检查",
    val foregroundServiceResult: String = "未检查",
    val listenerConnectionResult: String = "未检查",
    val probeResult: String = "未检查",
    val rebindResult: String = "未执行",
    val finalConclusion: String = "未开始"
) {
    val hasResults: Boolean
        get() = running || permissionResult != "未检查"
}

private data class ScreenshotPreviewState(
    val imageUri: String,
    val rawText: String,
    val parseResult: ScreenshotParseResult,
    val pendingRecordId: Long?,
    val message: String
)

private data class QuickBackfillPreset(
    val title: String,
    val sourceApp: String,
    val note: String,
    val type: TransactionType = TransactionType.EXPENSE
)

private fun defaultQuickBackfillPreset(): QuickBackfillPreset =
    QuickBackfillPreset(
        title = "刚刚付款补录",
        sourceApp = "手动",
        note = ""
    )

private fun wechatScanBackfillPreset(): QuickBackfillPreset =
    QuickBackfillPreset(
        title = "扫码支付后补录",
        sourceApp = "微信",
        note = "微信扫码支付补录"
    )

private fun presetNote(preset: QuickBackfillPreset, sourceApp: String, merchantName: String): String =
    preset.note.ifBlank {
        listOf(sourceApp, merchantName.ifBlank { "Unknown" })
            .joinToString(" / ")
    }

private enum class DebugLogFilter(val packageName: String?) {
    ALL(null),
    WECHAT("com.tencent.mm"),
    ALIPAY("com.eg.android.AlipayGphone")
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val DateTimeDisplayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private const val NOTIFICATION_LOG_TAG = "PaymentNotificationListener"
private const val WECHAT_PACKAGE = "com.tencent.mm"
private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
private const val NORMAL_TEST_WINDOW_MILLIS = 30_000L
private const val PAYMENT_TEST_WINDOW_MILLIS = 120_000L
private const val APP_VERSION_DISPLAY = "V1.1.2"
private val NotificationAmountRegex = Regex("""[¥￥]?\s*-?\d+(?:,\d{3})*(?:\.\d{1,2})?\s*(?:元|CNY|RMB)?""")
private val Green = Color(0xFF1B8F5A)
private val Red = Color(0xFFD85A50)
private val Orange = Color(0xFFE08924)
private val SoftGreen = Color(0xFFE5F6EC)
private val SoftRed = Color(0xFFFDEAE7)
private val PageBackground = Color(0xFFF7F8FA)
private val PrimaryText = Color(0xFF20242C)
private val MutedText = Color(0xFF77808C)

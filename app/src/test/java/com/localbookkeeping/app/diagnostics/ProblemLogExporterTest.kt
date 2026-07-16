package com.localbookkeeping.app.diagnostics

import com.localbookkeeping.app.analytics.AutoBookkeepingCounters
import com.localbookkeeping.app.analytics.AutoBookkeepingSource
import com.localbookkeeping.app.analytics.AutoBookkeepingSourceSummary
import com.localbookkeeping.app.analytics.AutoBookkeepingStatsSnapshot
import com.localbookkeeping.app.data.BackgroundDiagnosticsReport
import com.localbookkeeping.app.notification.DeviceCompatInfo
import com.localbookkeeping.app.notification.ListenerHealthEvaluation
import com.localbookkeeping.app.notification.ListenerProbeSnapshot
import com.localbookkeeping.app.notification.ListenerRecoverySnapshot
import com.localbookkeeping.app.notification.ListenerServiceStatus
import com.localbookkeeping.app.notification.NotificationListenerRuntimeState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProblemLogExporterTest {
    @Test
    fun generatedLogContainsDiagnosticsWithoutPrivateBillData() {
        val log = ProblemLogExporter.generate(
            ProblemLogSnapshot(
                appVersionName = "1.1.3",
                versionCode = 113,
                buildTime = "2026-07-06T00:00:00Z",
                exportTimeMillis = 1_000L,
                deviceCompatInfo = DeviceCompatInfo.from(
                    brand = "HONOR",
                    manufacturer = "HONOR",
                    model = "HONOR 30 Pro+",
                    device = "test",
                    osVersion = "4.2.0",
                    sdkVersion = 35,
                    harmonyOsVersion = "4.2.0"
                ),
                notificationPermissionEnabled = true,
                autoListenEnabled = true,
                foregroundServiceRunning = true,
                runtimeState = runtimeState(),
                healthEvaluation = ListenerHealthEvaluation(
                    ListenerServiceStatus.VENDOR_BLOCKED,
                    listOf("权限已授权，但监听服务未实际连接")
                ),
                recoverySnapshot = recoverySnapshot(),
                probeSnapshot = ListenerProbeSnapshot(100L, 0L, 900L),
                diagnosticsReport = diagnosticsReport(),
                enabledMonitorApps = listOf("微信(com.tencent.mm)", "支付宝(com.eg.android.AlipayGphone)", "云闪付(com.unionpay)"),
                autoBookkeepingStats = AutoBookkeepingStatsSnapshot(
                    windowDays = 14,
                    fromDate = LocalDate.of(2026, 7, 3),
                    toDate = LocalDate.of(2026, 7, 16),
                    total = AutoBookkeepingCounters(
                        notificationReceived = 12,
                        paymentRelated = 10,
                        pendingCreated = 9,
                        amountParseFailed = 1,
                        userConfirmed = 8,
                        userAmountEdited = 1,
                        userDeleted = 0,
                        duplicateFiltered = 1
                    ),
                    bySource = listOf(
                        AutoBookkeepingSourceSummary(
                            AutoBookkeepingSource.WECHAT,
                            AutoBookkeepingCounters(paymentRelated = 6, pendingCreated = 5)
                        )
                    )
                )
            )
        )

        assertTrue(log.contains("芽芽记账 - 问题日志"))
        assertTrue(log.contains("APP版本：1.1.3"))
        assertTrue(log.contains("设备型号：HONOR HONOR 30 Pro+"))
        assertTrue(log.contains("疑似厂商系统限制后台监听"))
        assertTrue(log.contains("probe 失败次数：1"))
        assertTrue(log.contains("enabledMonitorApps：微信(com.tencent.mm)、支付宝(com.eg.android.AlipayGphone)、云闪付(com.unionpay)"))
        assertTrue(log.contains("最近被忽略的 packageName：com.example.unselected"))
        assertTrue(log.contains("最近进入解析的非微信/支付宝 APP：com.unionpay"))
        assertTrue(log.contains("最近通用支付通知解析结果：appName=云闪付,parseStatus=pending_confirm"))
        assertTrue(log.contains("【匿名自动记账统计】"))
        assertTrue(log.contains("生成成功率：90%"))
        assertTrue(log.contains("微信：支付相关=6，生成账单=5，成功率=83%"))
        assertFalse(log.contains("rawText"))
        assertFalse(log.contains("v1|2026-07-16"))
        assertFalse(log.contains("星巴克"))
        assertFalse(log.contains("88.80"))
        assertFalse(log.contains("午饭备注"))
    }

    private fun runtimeState(): NotificationListenerRuntimeState =
        NotificationListenerRuntimeState(
            listenerConnected = false,
            rawListenerConnected = false,
            lastConnectedTime = 0L,
            lastDisconnectedTime = 500L,
            lastHeartbeatTime = 0L,
            lastNotificationTime = 0L,
            lastPaymentNotificationTime = 0L,
            lastWechatNotificationTime = 0L,
            lastAlipayNotificationTime = 0L,
            lastNotificationPackage = "",
            lastNotificationTitle = "付款给星巴克",
            lastNotificationText = "支付 88.80 午饭备注",
            lastRemovedTime = 0L,
            lastRemovedPackage = "",
            lastRemovedTitle = "",
            lastRemovedText = ""
        )

    private fun recoverySnapshot(): ListenerRecoverySnapshot =
        ListenerRecoverySnapshot(
            lastHealthStatus = "VENDOR_BLOCKED",
            lastHealthCheckAt = 700L,
            lastAutoRepairAt = 0L,
            autoRepairCount = 0,
            lastRebindAttemptAt = 0L,
            lastRebindResult = "vendorReauthRequired",
            lastFailureReason = "需要重新授权",
            needsListenerRecheck = false,
            lastPackageReplacedAt = 0L,
            lastVendorCompatCheckAt = 700L
        )

    private fun diagnosticsReport(): BackgroundDiagnosticsReport =
        BackgroundDiagnosticsReport(
            deviceBrand = "HONOR",
            manufacturer = "HONOR",
            model = "HONOR 30 Pro+",
            osVersion = "HarmonyOS 4.2.0",
            sdkVersion = 35,
            detectedRomType = "HarmonyOS",
            isHuaweiHonorDevice = true,
            lastListenerConnectedTime = 0L,
            lastListenerDisconnectedTime = 500L,
            lastProbeSuccessTime = 0L,
            lastProbeFailTime = 900L,
            lastVendorCompatCheckTime = 700L,
            vendorCompatSuggestion = "请重新授权通知监听",
            appStartCount = 1,
            listenerConnectedCount = 0,
            listenerDisconnectedCount = 1,
            requestRebindCount = 0,
            foregroundServiceStartCount = 1,
            foregroundServiceStopCount = 0,
            healthCheckCount = 2,
            healthHealthyCount = 0,
            healthSuspiciousCount = 0,
            healthDisconnectedCount = 1,
            requestRebindAttemptCount = 0,
            requestRebindConnectedCount = 0,
            requestRebindFailedCount = 0,
            packageReplacedCount = 0,
            probeNotificationSentCount = 1,
            probeSuccessCount = 0,
            probeFailCount = 1,
            autoListenRestoreCount = 0,
            foregroundServiceMissingCount = 0,
            listenerPermissionGrantedButProbeFailCount = 1,
            requestRebindAfterProbeFailCount = 0,
            paymentCapturedCount = 0,
            paymentParseSuccessCount = 0,
            paymentParseFailCount = 0,
            recentlyIgnoredPackageName = "com.example.unselected",
            recentGenericPaymentAppPackageName = "com.unionpay",
            recentGenericPaymentParseResult = "appName=云闪付,parseStatus=pending_confirm",
            failureReasons = emptyMap()
        )
}

package com.localbookkeeping.app.diagnostics

import com.localbookkeeping.app.analytics.AutoBookkeepingStatsSnapshot
import com.localbookkeeping.app.data.BackgroundDiagnosticsReport
import com.localbookkeeping.app.notification.DeviceCompatInfo
import com.localbookkeeping.app.notification.ListenerHealthEvaluation
import com.localbookkeeping.app.notification.ListenerProbeSnapshot
import com.localbookkeeping.app.notification.ListenerRecoverySnapshot
import com.localbookkeeping.app.notification.ListenerServiceStatus
import com.localbookkeeping.app.notification.NotificationListenerRuntimeState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ProblemLogSnapshot(
    val appVersionName: String,
    val versionCode: Long,
    val buildTime: String,
    val exportTimeMillis: Long,
    val deviceCompatInfo: DeviceCompatInfo,
    val notificationPermissionEnabled: Boolean,
    val autoListenEnabled: Boolean,
    val foregroundServiceRunning: Boolean,
    val runtimeState: NotificationListenerRuntimeState,
    val healthEvaluation: ListenerHealthEvaluation,
    val recoverySnapshot: ListenerRecoverySnapshot,
    val probeSnapshot: ListenerProbeSnapshot,
    val diagnosticsReport: BackgroundDiagnosticsReport,
    val enabledMonitorApps: List<String> = emptyList(),
    val autoBookkeepingStats: AutoBookkeepingStatsSnapshot? = null
)

object ProblemLogExporter {
    fun generate(snapshot: ProblemLogSnapshot): String {
        val conclusion = conclusionFor(snapshot)
        val suggestion = suggestionFor(snapshot, conclusion)
        return buildString {
            appendLine("芽芽记账 - 问题日志")
            appendLine("导出时间：${formatTime(snapshot.exportTimeMillis)}")
            appendLine("APP版本：${snapshot.appVersionName}")
            appendLine("versionCode：${snapshot.versionCode}")
            appendLine("构建时间：${snapshot.buildTime.ifBlank { "unknown" }}")
            appendLine("设备型号：${snapshot.deviceCompatInfo.deviceName}")
            appendLine("系统版本：${snapshot.deviceCompatInfo.systemLabel}")
            appendLine()
            appendLine("【APP 信息】")
            appendLine("APP版本：${snapshot.appVersionName}")
            appendLine("versionCode：${snapshot.versionCode}")
            appendLine("构建时间：${snapshot.buildTime.ifBlank { "unknown" }}")
            appendLine()
            appendLine("【设备信息】")
            appendLine("品牌：${snapshot.deviceCompatInfo.brand}")
            appendLine("厂商：${snapshot.deviceCompatInfo.manufacturer}")
            appendLine("型号：${snapshot.deviceCompatInfo.model}")
            appendLine("设备代号：${snapshot.deviceCompatInfo.device}")
            appendLine("系统版本：${snapshot.deviceCompatInfo.systemLabel}")
            appendLine("Android版本：${snapshot.deviceCompatInfo.osVersion}")
            appendLine("SDK版本：${snapshot.deviceCompatInfo.sdkVersion}")
            appendLine("ROM类型：${snapshot.deviceCompatInfo.detectedRomType}")
            appendLine("华为/荣耀设备：${yesNo(snapshot.deviceCompatInfo.isHuaweiHonorDevice)}")
            appendLine()
            appendLine("【监听状态】")
            appendLine("通知监听权限：${enabledLabel(snapshot.notificationPermissionEnabled)}")
            appendLine("自动监听：${enabledLabel(snapshot.autoListenEnabled)}")
            appendLine("前台服务：${runningLabel(snapshot.foregroundServiceRunning)}")
            appendLine("监听连接：${connectedLabel(snapshot.runtimeState.listenerConnected)}")
            appendLine("rawListenerConnected：${connectedLabel(snapshot.runtimeState.rawListenerConnected)}")
            appendLine("健康状态：${statusLabel(snapshot.healthEvaluation.status)}")
            appendLine("健康原因：${snapshot.healthEvaluation.reasons.joinToString(" / ").ifBlank { "无" }}")
            appendLine("最近健康检查：${formatTime(snapshot.recoverySnapshot.lastHealthCheckAt)}")
            appendLine("最近 requestRebind 结果：${snapshot.recoverySnapshot.lastRebindResult.ifBlank { "暂无" }}")
            appendLine("最近失败原因：${snapshot.recoverySnapshot.lastFailureReason.ifBlank { "暂无" }}")
            appendLine("enabledMonitorApps：${snapshot.enabledMonitorApps.joinToString("、").ifBlank { "暂无" }}")
            appendLine()
            appendLine("【最近记录】")
            appendLine("最近通知时间：${formatTime(snapshot.runtimeState.lastNotificationTime)}")
            appendLine("最近微信通知：${formatTime(snapshot.runtimeState.lastWechatNotificationTime)}")
            appendLine("最近支付宝通知：${formatTime(snapshot.runtimeState.lastAlipayNotificationTime)}")
            appendLine("最近付款通知：${formatTime(snapshot.runtimeState.lastPaymentNotificationTime)}")
            appendLine("最近 probe 成功：${formatTime(snapshot.probeSnapshot.lastSuccessTime)}")
            appendLine("最近 probe 失败：${formatTime(snapshot.probeSnapshot.lastFailTime)}")
            appendLine("最近被忽略的 packageName：${snapshot.diagnosticsReport.recentlyIgnoredPackageName.ifBlank { "暂无" }}")
            appendLine("最近进入解析的非微信/支付宝 APP：${snapshot.diagnosticsReport.recentGenericPaymentAppPackageName.ifBlank { "暂无" }}")
            appendLine("最近通用支付通知解析结果：${snapshot.diagnosticsReport.recentGenericPaymentParseResult.ifBlank { "暂无" }}")
            appendLine()
            appendLine("【后台诊断统计】")
            appendLine("最近24小时健康检查次数：${snapshot.diagnosticsReport.healthCheckCount}")
            appendLine("probe 成功次数：${snapshot.diagnosticsReport.probeSuccessCount}")
            appendLine("probe 失败次数：${snapshot.diagnosticsReport.probeFailCount}")
            appendLine("requestRebind 次数：${snapshot.diagnosticsReport.requestRebindAttemptCount}")
            appendLine("监听断开次数：${snapshot.diagnosticsReport.listenerDisconnectedCount}")
            appendLine("前台服务启动次数：${snapshot.diagnosticsReport.foregroundServiceStartCount}")
            appendLine("前台服务停止次数：${snapshot.diagnosticsReport.foregroundServiceStopCount}")
            snapshot.autoBookkeepingStats?.let { stats ->
                appendLine()
                appendLine("【匿名自动记账统计】")
                appendLine("统计窗口：最近${stats.windowDays}天")
                appendLine("收到监听通知：${stats.total.notificationReceived}")
                appendLine("支付相关通知：${stats.total.paymentRelated}")
                appendLine("自动生成账单：${stats.total.pendingCreated}")
                appendLine("金额解析失败：${stats.total.amountParseFailed}")
                appendLine("重复通知过滤：${stats.total.duplicateFiltered}")
                appendLine("用户确认：${stats.total.userConfirmed}")
                appendLine("用户修改金额：${stats.total.userAmountEdited}")
                appendLine("用户删除：${stats.total.userDeleted}")
                appendLine("生成成功率：${rateLabel(stats.total.generationSuccessRatePercent)}")
                stats.bySource.forEach { source ->
                    appendLine(
                        "${source.source.displayName}：支付相关=${source.counters.paymentRelated}，生成账单=${source.counters.pendingCreated}，成功率=${rateLabel(source.counters.generationSuccessRatePercent)}"
                    )
                }
            }
            appendLine()
            appendLine("【诊断结论】")
            appendLine(conclusion.joinToString("；"))
            appendLine()
            appendLine("【建议操作】")
            appendLine(suggestion.joinToString("；"))
            appendLine()
            appendLine("【隐私说明】")
            appendLine("本日志不包含完整账单、通知正文、商户名称、金额明细或备注。")
        }.trimEnd()
    }

    private fun conclusionFor(snapshot: ProblemLogSnapshot): List<String> {
        val result = mutableListOf<String>()
        when {
            !snapshot.notificationPermissionEnabled -> result += "通知监听权限未开启"
            !snapshot.autoListenEnabled -> result += "自动监听未开启"
            !snapshot.foregroundServiceRunning -> result += "前台服务未运行"
            snapshot.healthEvaluation.status == ListenerServiceStatus.HEALTHY -> result += "监听正常"
            snapshot.healthEvaluation.status == ListenerServiceStatus.VENDOR_BLOCKED -> result += "疑似厂商系统限制后台监听"
            snapshot.healthEvaluation.status == ListenerServiceStatus.PERMISSION_GRANTED_BUT_NOT_CONNECTED ->
                result += "权限已授权但监听服务未连接"
            snapshot.healthEvaluation.status == ListenerServiceStatus.FOREGROUND_RUNNING_BUT_LISTENER_DEAD ->
                result += "前台服务运行中但监听服务未连接"
            snapshot.healthEvaluation.status == ListenerServiceStatus.PROBE_FAILED -> result += "探测通知失败"
            snapshot.healthEvaluation.status == ListenerServiceStatus.DISCONNECTED -> result += "监听服务已断开"
            else -> result += statusLabel(snapshot.healthEvaluation.status)
        }
        if (snapshot.runtimeState.lastWechatNotificationTime <= 0L && snapshot.runtimeState.lastAlipayNotificationTime <= 0L) {
            result += "最近没有收到微信 / 支付宝通知"
        }
        return result.distinct()
    }

    private fun suggestionFor(snapshot: ProblemLogSnapshot, conclusion: List<String>): List<String> {
        val result = mutableListOf<String>()
        if (!snapshot.notificationPermissionEnabled || conclusion.any { it.contains("权限") || it.contains("未连接") }) {
            result += "打开通知监听权限，关闭本 APP 权限，等待 5 秒后重新开启"
        }
        if (!snapshot.autoListenEnabled) {
            result += "返回监听页开启自动监听"
        }
        if (!snapshot.foregroundServiceRunning) {
            result += "点击恢复自动监听，确认前台服务通知保持运行"
        }
        if (snapshot.deviceCompatInfo.isHuaweiHonorDevice || conclusion.any { it.contains("厂商") }) {
            result += "检查应用启动管理、后台活动、电池优化和通知权限"
        }
        if (snapshot.runtimeState.lastWechatNotificationTime <= 0L && snapshot.runtimeState.lastAlipayNotificationTime <= 0L) {
            result += "确认微信 / 支付宝通知权限、锁屏通知和横幅通知已允许"
        }
        if (result.isEmpty()) result += "继续观察；如付款未记录，请使用监听页测试通知和付款测试"
        return result.distinct()
    }

    private fun statusLabel(status: ListenerServiceStatus): String = when (status) {
        ListenerServiceStatus.HEALTHY -> "监听正常"
        ListenerServiceStatus.STALE -> "长时间没有事件，未确认失效"
        ListenerServiceStatus.SUSPICIOUS -> "监听疑似失效"
        ListenerServiceStatus.DISCONNECTED -> "监听已断开"
        ListenerServiceStatus.PERMISSION_MISSING -> "通知监听权限未开启"
        ListenerServiceStatus.PERMISSION_GRANTED_BUT_NOT_CONNECTED -> "权限已授权但监听服务未连接"
        ListenerServiceStatus.FOREGROUND_RUNNING_BUT_LISTENER_DEAD -> "前台服务运行中但监听服务未连接"
        ListenerServiceStatus.VENDOR_BLOCKED -> "疑似厂商系统限制后台监听"
        ListenerServiceStatus.PROBE_FAILED -> "探测通知失败"
        ListenerServiceStatus.SERVICE_UNKNOWN -> "监听状态未知"
    }

    private fun enabledLabel(enabled: Boolean): String = if (enabled) "已开启" else "未开启"

    private fun runningLabel(running: Boolean): String = if (running) "运行中" else "未运行"

    private fun connectedLabel(connected: Boolean): String = if (connected) "已连接" else "未连接"

    private fun yesNo(value: Boolean): String = if (value) "是" else "否"

    private fun rateLabel(value: Int?): String = value?.let { "$it%" } ?: "暂无"

    private fun formatTime(millis: Long): String =
        if (millis > 0L) {
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            "暂无"
        }
}

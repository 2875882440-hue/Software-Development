package com.localbookkeeping.app.notification

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class MonitoredAppInfo(
    val packageName: String,
    val appName: String,
    val enabled: Boolean,
    val recommended: Boolean,
    val addedAtMillis: Long,
    val lastSeenAtMillis: Long
)

object MonitoredAppConfig {
    const val WECHAT_PACKAGE = "com.tencent.mm"
    const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"

    private const val PREFS = "monitored_app_config"
    private const val KEY_ENABLED_PACKAGES = "enabled_packages"
    private const val KEY_APP_NAME_PREFIX = "app_name_"
    private const val KEY_ADDED_AT_PREFIX = "added_at_"
    private const val KEY_LAST_SEEN_AT_PREFIX = "last_seen_at_"

    fun defaultEnabledPackages(): Set<String> = setOf(WECHAT_PACKAGE, ALIPAY_PACKAGE)

    fun enabledPackages(context: Context): Set<String> {
        val prefs = prefs(context)
        return if (prefs.contains(KEY_ENABLED_PACKAGES)) {
            prefs.getStringSet(KEY_ENABLED_PACKAGES, emptySet()).orEmpty()
        } else {
            defaultEnabledPackages()
        }
    }

    fun isEnabled(context: Context, packageName: String): Boolean =
        packageName in enabledPackages(context)

    fun setEnabled(
        context: Context,
        packageName: String,
        appName: String,
        enabled: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val appContext = context.applicationContext
        val current = enabledPackages(appContext)
        val next = toggleEnabled(current, packageName, enabled)
        val prefs = prefs(appContext)
        val editor = prefs.edit()
            .putStringSet(KEY_ENABLED_PACKAGES, next)
            .putString(KEY_APP_NAME_PREFIX + packageName, appName)
        if (enabled && prefs.getLong(KEY_ADDED_AT_PREFIX + packageName, 0L) <= 0L) {
            editor.putLong(KEY_ADDED_AT_PREFIX + packageName, nowMillis)
        }
        editor.apply()
    }

    fun markSeen(
        context: Context,
        packageName: String,
        appName: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        prefs(context).edit()
            .putString(KEY_APP_NAME_PREFIX + packageName, appName)
            .putLong(KEY_LAST_SEEN_AT_PREFIX + packageName, nowMillis)
            .apply()
    }

    fun enabledCount(context: Context): Int = enabledPackages(context).size

    fun enabledSummary(context: Context, maxItems: Int = 4): String {
        val appContext = context.applicationContext
        val enabled = enabledPackages(appContext).toList()
        if (enabled.isEmpty()) return "暂无"
        val labels = enabled.map { packageName -> appName(appContext, packageName) }
        val shown = labels.take(maxItems).joinToString("、")
        val remaining = labels.size - maxItems
        return if (remaining > 0) "$shown 等 $remaining 个" else shown
    }

    fun enabledSummaryForLog(context: Context): List<String> =
        enabledPackages(context).map { packageName ->
            "${appName(context, packageName)}($packageName)"
        }

    fun appName(context: Context, packageName: String): String {
        val appContext = context.applicationContext
        val saved = prefs(appContext).getString(KEY_APP_NAME_PREFIX + packageName, "").orEmpty()
        if (saved.isNotBlank()) return saved
        return when (packageName) {
            WECHAT_PACKAGE -> "微信"
            ALIPAY_PACKAGE -> "支付宝"
            else -> loadAppLabel(appContext, packageName).ifBlank { packageName }
        }
    }

    fun installedApps(context: Context): List<MonitoredAppInfo> {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val installedApplications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
        val enabled = enabledPackages(appContext)
        val prefs = prefs(appContext)
        return installedApplications
            .asSequence()
            .filter { info -> isUserInstalledApp(info) }
            .mapNotNull { info ->
                val packageName = info.packageName ?: return@mapNotNull null
                if (packageName == appContext.packageName) return@mapNotNull null
                val appName = packageManager.getApplicationLabel(info)?.toString()?.trim().orEmpty()
                    .ifBlank { packageName }
                MonitoredAppInfo(
                    packageName = packageName,
                    appName = appName,
                    enabled = packageName in enabled,
                    recommended = isRecommendedApp(packageName, appName),
                    addedAtMillis = prefs.getLong(KEY_ADDED_AT_PREFIX + packageName, 0L),
                    lastSeenAtMillis = prefs.getLong(KEY_LAST_SEEN_AT_PREFIX + packageName, 0L)
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<MonitoredAppInfo> { it.enabled }
                    .thenByDescending { it.recommended }
                    .thenBy { it.appName.lowercase() }
            )
            .toList()
    }

    fun isUserInstalledAppFlags(flags: Int): Boolean {
        val isSystem = flags and ApplicationInfo.FLAG_SYSTEM != 0
        val isUpdatedSystem = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        return !isSystem && !isUpdatedSystem
    }

    private fun isUserInstalledApp(info: ApplicationInfo): Boolean =
        isUserInstalledAppFlags(info.flags)

    fun toggleEnabled(current: Set<String>, packageName: String, enabled: Boolean): Set<String> =
        if (enabled) current + packageName else current - packageName

    fun isRecommendedApp(packageName: String, appName: String): Boolean {
        val combined = "$packageName $appName".lowercase()
        return recommendedKeywords.any { combined.contains(it) }
    }

    fun isDefaultPackage(packageName: String): Boolean =
        packageName == WECHAT_PACKAGE || packageName == ALIPAY_PACKAGE

    private fun loadAppLabel(context: Context, packageName: String): String =
        runCatching {
            val packageManager = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault("")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val recommendedKeywords = listOf(
        "com.tencent.mm",
        "com.eg.android.alipaygphone",
        "unionpay",
        "云闪付",
        "meituan",
        "美团",
        "jingdong",
        "jd",
        "京东",
        "douyin",
        "抖音",
        "pinduoduo",
        "拼多多",
        "bank",
        "银行",
        "招商",
        "工商",
        "建设",
        "农业",
        "中国银行",
        "交通银行",
        "邮储"
    )
}

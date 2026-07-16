# V1.2.1 Stabilization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 交付 V1.2.1 稳定化维护版，统一文档、加入最近 30 天匿名自动记账计数、提供 14 天成功率报告、多机型测试模板和可校验 APK。

**Architecture:** 使用独立 SharedPreferences 存储按自然日、来源和事件拆分的整数计数，纯 Kotlin 聚合器生成 14 天报告。通知服务记录解析漏斗，ViewModel 记录用户确认和纠正行为，Compose 监听页提供报告入口，问题日志只导出匿名汇总。

**Tech Stack:** Kotlin, Android SharedPreferences, Jetpack Compose, JUnit 4, Gradle Android Plugin

---

### Task 1: 建立匿名统计核心

**Files:**
- Create: `app/src/main/java/com/localbookkeeping/app/analytics/AutoBookkeepingStats.kt`
- Test: `app/src/test/java/com/localbookkeeping/app/analytics/AutoBookkeepingStatsTest.kt`

**Step 1: Write the failing test**

覆盖以下行为：

```kotlin
@Test fun aggregatesLast14DaysBySource()
@Test fun prunesEntriesOlderThan30Days()
@Test fun returnsNullRateWhenDenominatorIsZero()
@Test fun persistedKeysContainOnlyDateSourceAndEvent()
```

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "*AutoBookkeepingStatsTest"`

Expected: FAIL，因为统计类型和存储尚不存在。

**Step 3: Write minimal implementation**

实现：

```kotlin
enum class AutoBookkeepingSource { WECHAT, ALIPAY, OTHER }
enum class AutoBookkeepingEvent {
    NOTIFICATION_RECEIVED,
    PAYMENT_RELATED,
    PENDING_CREATED,
    AMOUNT_PARSE_FAILED,
    USER_CONFIRMED,
    USER_AMOUNT_EDITED,
    USER_DELETED,
    DUPLICATE_FILTERED
}
```

存储键格式固定为：

```text
v1|2026-07-16|WECHAT|PENDING_CREATED
```

实现 30 天清理、14 天聚合、总体与来源分项成功率计算。

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "*AutoBookkeepingStatsTest"`

Expected: PASS。

### Task 2: 接入真实通知解析漏斗

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/BookkeepingApplication.kt`
- Modify: `app/src/main/java/com/localbookkeeping/app/notification/PaymentNotificationListenerService.kt`

**Step 1: Add store to application and listener**

应用和通知服务均通过 application context 创建统计存储。

**Step 2: Record monitored notification events**

只对用户已启用的监听应用记录：

```kotlin
stats.record(NOTIFICATION_RECEIVED, sbn.packageName)
stats.record(PAYMENT_RELATED, sbn.packageName)
stats.record(AMOUNT_PARSE_FAILED, sbn.packageName)
stats.record(PENDING_CREATED, sbn.packageName)
stats.record(DUPLICATE_FILTERED, sbn.packageName)
```

probe、本 App 通知、未勾选应用和模拟通知不计入。

**Step 3: Compile**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: BUILD SUCCESSFUL。

### Task 3: 接入用户确认与纠正行为

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/ui/BookkeepingViewModel.kt`
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`

**Step 1: Inject stats store**

`BookkeepingViewModelFactory` 接收统计存储。

**Step 2: Record user actions**

仅当账单具有非空 `notificationPackageName` 时记录：

```kotlin
USER_CONFIRMED
USER_AMOUNT_EDITED
USER_DELETED
```

编辑待确认并确认时同时记录确认；金额确实变化时才记录改金额。

**Step 3: Compile**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: BUILD SUCCESSFUL。

### Task 4: 增加成功率报告页和匿名问题日志

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Modify: `app/src/main/java/com/localbookkeeping/app/diagnostics/ProblemLogExporter.kt`
- Modify: `app/src/test/java/com/localbookkeeping/app/diagnostics/ProblemLogExporterTest.kt`

**Step 1: Add report navigation**

新增 `AppScreen.AUTO_BOOKKEEPING_STATS`，在监听页诊断工具区加入“成功率”按钮。

**Step 2: Build report UI**

报告显示最近 14 天：

- 收到监听通知
- 支付相关通知
- 自动生成账单
- 金额解析失败
- 重复过滤
- 用户确认
- 用户修改金额
- 用户删除
- 生成成功率和通知转化率
- 微信、支付宝、其他应用分项

**Step 3: Extend privacy-safe problem log**

问题日志只追加上述匿名数字。测试断言日志仍不包含通知正文、商户、金额、备注和统计存储键。

**Step 4: Run focused tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "*ProblemLogExporterTest"`

Expected: PASS。

### Task 5: 统一版本和项目文档

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `README.md`
- Modify: `PROJECT_STATUS.md`
- Modify: `NEXT_TASK.md`
- Modify: `BUG_LOG.md`
- Modify: `CHANGELOG.md`
- Modify: `TEST_REPORT.md`
- Create: `V1.2.1_DEVICE_TEST_PLAN.md`
- Create: `V1.2.1_RELEASE_NOTES.md`

**Step 1: Bump version**

```kotlin
versionCode = 121
versionName = "1.2.1"
```

**Step 2: Align documentation**

所有当前版本统一为 V1.2.1，下一阶段统一为 V1.2.2 多机型长期测试；保留“不重写 UI、不随意迁移 Room、不破坏监听/解析/备份”的边界。

**Step 3: Add device test matrix**

记录小米/Redmi、华为/荣耀、OPPO、vivo、三星及 Android 8-15 的统一步骤和结果表。

### Task 6: 完整验证与本地封存

**Files:**
- Create locally: `release/V1.2.1/芽芽记账-V1.2.1-debug.apk`
- Create locally: `release/V1.2.1/SHA256SUMS.txt`
- Create locally: `release/V1.2.1/BUILD_REPORT.md`

**Step 1: Run full unit suite**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: BUILD SUCCESSFUL。

**Step 2: Compile and build APK**

Run:

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat assembleDebug
```

Expected: BUILD SUCCESSFUL，生成 `app/build/outputs/apk/debug/app-debug.apk`。

**Step 3: Archive and hash**

复制 APK 到固定版本目录，使用 `Get-FileHash -Algorithm SHA256` 生成校验值。

**Step 4: Check device availability**

Run Android SDK 中的 `adb devices`。

Expected: 有设备时执行覆盖安装和页面/支付流程；无设备时在报告中标记真机验证待完成。

**Step 5: Review repository state**

Run:

```powershell
git status --short
git diff --check
git diff --stat
```

Expected: 只包含 V1.2.1 范围内文件，无空白错误。

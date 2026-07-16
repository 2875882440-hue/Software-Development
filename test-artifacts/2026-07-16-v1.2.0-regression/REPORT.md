# V1.2.0 UI Refresh 回归与封存报告

## 封存结论

V1.2.0 UI Refresh 已完成静态入口审计、自动测试、真机非破坏性回归、问题修复和最终覆盖安装。可以封存为 V1.2.0 UI Refresh。

## 版本与设备

- 应用：芽芽记账 / `com.localbookkeeping.app`
- `versionName`：`1.2.0`
- `versionCode`：`120`
- Room schema：`12`（未修改）
- 设备：Xiaomi 2510DRK44C（annibale）
- 系统：Android 16 / SDK 36
- 屏幕：1156 × 2510
- 安装方式：`adb install -r`，保留应用数据
- 最终启动耗时：347 ms

## UI 替换后发现并修复的问题

1. 统计页遗漏“来源统计”，旧版的非空周/月归档也没有接入新版页面。
   - 修复：复用现有 `stats.sourceItems`、`monthWeekArchives()`、`yearMonthArchives()` 和既有汇总卡片。
   - 结果：真机显示 4 个来源、3 个有数据周、3 个有数据月份；无账单周期继续不生成归档卡片。
2. 限额圆环保留了今日消费和总限额，但遗漏明确的“剩余额度/已超额金额”。
   - 修复：复用现有 `DailyLimitStatus.remainingCents` 和 `exceededCents` 恢复展示。
   - 结果：当前真机显示“剩余额度 ¥30.00”。

此前同一轮 UI 回归已修复首页今日支出口径和首次引导弹窗遮挡问题，最终包一并包含这些修复。

## 功能入口回归矩阵

| 范围 | 检查结果 | 验证方式 |
| --- | --- | --- |
| 底部 Tab：记账 / 统计 / 限额 / 监听 | 全部保留 | 真机逐页打开 |
| 最近账单、手动记账、待确认 | 全部保留 | 真机打开；未保存或确认数据 |
| 账单详情、编辑、删除 | 全部保留 | 真机打开详情和编辑页；未保存、未删除 |
| 备份与恢复入口 | 保留 | 首页全部工具进入备份页 |
| 今日 / 周 / 月 / 自定义统计 | 全部保留 | 真机切换今日、周、月；自定义入口静态核对 |
| 分类统计 | 保留 | 真机显示分类环图和真实分类占比 |
| 来源统计 | 已恢复 | 真机显示 4 个真实来源 |
| 空数据逻辑 | 正常 | 今日无账单显示空态；归档计算测试只保留有数据周/月 |
| 限额开关、每日限额 | 保留 | 真机显示已开启开关与 30.00 元设置；未切换、未保存 |
| 今日消费、剩余额度、超额提醒 | 保留 | 真机显示今日 ¥0.00、剩余 ¥30.00；超额分支由单元测试覆盖 |
| 今天不再提醒 | 保留 | 真机入口存在；未改变当前设置 |
| 自动监听开关 | 保留 | 真机开关为开启状态 |
| 通知监听权限 | 正常 | 真机显示已开启 / 已授权 |
| 前台服务、监听健康 | 正常 | `isForeground=true`、`startRequested=true`，健康页显示监听正常 |
| 监听急救 / probe | 保留 | 一键修复、重新绑定、立即检查入口存在；报告记录最近 probe 成功 |
| 管理监听应用 | 保留 | 真机打开，显示 10 个已启用应用；未切换 |
| 导出问题日志 | 保留 | 复制与分享入口存在；未触发外部分享 |
| 后台诊断报告 | 保留 | 真机打开并读取设备、ROM、监听与 probe 状态 |
| 导出 CSV | 保留 | 真机按钮存在；CSV 转义单元测试通过 |
| 导出 JSON | 保留 | 真机“立即完整备份”按钮存在；JSON round-trip 单元测试通过 |
| JSON 恢复 | 保留 | 真机选择文件入口存在；未执行恢复以保护现有数据 |
| 恢复跳过重复 | 保留 | UI 明示自动跳过；Repository 仍以 `backupDuplicateKeys()` 返回 `skippedCount` |
| 系统返回 | 正常 | 从监听健康页返回监听父页 |
| 顶部返回 | 正常 | 从监听应用页返回监听父页 |
| 子页面退出保护 | 正常 | 新增、详情、编辑、待确认、监听子页均回父页，未直接退出 App |

## 数据与核心逻辑保护

- 最终覆盖安装前后 `bookkeeping-v01.db` 均存在，首页仍显示原有账单，备份页识别 131 条记录。
- 测试未执行保存账单、确认/忽略待确认、编辑保存、删除、恢复、限额开关或监听应用开关。
- `AppDatabase.kt` 无差异，Room schema 仍为 12，迁移链 1→12 保留。
- `LocalBackupManager.kt`、`NotificationBillParser.kt`、`PaymentNotificationListenerService.kt`、`KeepAliveNotificationService.kt` 无差异。
- 未修改金额解析、通知监听、重复过滤或备份恢复核心实现。
- 数据库文件大小会因正常监听诊断日志和 WAL 写入发生变化，不代表 schema 或账单被清空。

## 自动验证

- `compileDebugKotlin`：BUILD SUCCESSFUL
- `testDebugUnitTest`：84 项，0 失败，0 错误，0 跳过
- `assembleDebug`：BUILD SUCCESSFUL
- `git diff --check`：通过
- Logcat：未发现应用 `FATAL EXCEPTION` 或 ANR

## 最终 APK

- 路径：`F:\Software Development\MobileBookkeepingApp\app\build\outputs\apk\debug\app-debug.apk`
- 大小：55,551,486 bytes
- SHA-256：`2BCFE16BBF5CD126355E9268B988EE2BD61EB351D557C139214201C4E23CB8C9`
- 真机安装：成功
- 真机包信息：`versionCode=120`、`versionName=1.2.0`

## 主要证据

- `02-home.png`：记账首页与四个 Tab
- `04-stats-today.png`：今日空数据状态
- `07-limit.png`：限额页基线
- `08-listener.png`：监听页与运行状态
- `09-health.png`：监听健康详情
- `12-listener-tools.png`：监听急救、日志和后台报告入口
- `13-apps.png`：监听应用管理
- `15-report.png`：后台诊断报告
- `18-detail.png`：账单详情、编辑和删除入口
- `20-pending.png`：待确认空态
- `23-stats-fixed.png`：来源统计与月归档修复
- `24-limit-fixed.png`：剩余额度修复
- `27-final-home.png`：V1.2.0 最终覆盖安装后的首页

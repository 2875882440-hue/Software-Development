# MobileBookkeepingApp 项目日志

更新时间：2026-06-17  
当前版本：V6.6 Test Release  
`versionName`：`0.6.6-test`  
`versionCode`：`12`

## 项目定位

MobileBookkeepingApp 是一款安卓本地记账 App，核心目标是通过本地账单记录和通知监听，尽量减少手动记账成本。

当前版本保持本地化设计，不包含登录、云同步、服务器、会员系统、无障碍服务或新的截图截屏模式。账单数据保存在手机本地，Room 数据库迁移要求保护已有账单数据。

## 版本演进记录

### V6.1：通知监听重绑定修复

重点处理 NotificationListenerService 在系统层面断开后无法及时恢复的问题。

完成内容：

- 梳理通知监听服务连接状态。
- 修复通知监听服务重绑定相关问题。
- 为后续后台稳定性增强打基础。

### V6.2：真实通知测试入口

重点是让真机测试更可验证，不再只依赖模拟通知。

完成内容：

- 增加真实通知测试入口。
- 支持验证本 App 测试通知是否能被监听。
- 支持验证微信普通通知是否能被监听。
- 支持验证支付宝通知是否能被监听。
- 支持观察真实付款通知捕获情况。
- 增加通知调试记录，便于查看 rawText 和通知来源。

### V6.3：真实付款通知解析增强

重点增强微信/支付宝付款通知解析能力。

完成内容：

- 解析真实通知 rawText。
- 提取金额候选。
- 根据关键词判断收入、支出或未知类型。
- 尝试提取商户信息。
- 识别微信、支付宝来源。
- 生成待确认账单。
- 支持无金额通知进入待补录流程。
- 支持从通知调试记录手动生成账单。

### V6.4：后台稳定增强 + 日/周/月统计

重点是提高自动记账稳定性，并补齐账单统计页。

完成内容：

- 增加后台稳定日志结构、DAO、Repository 和 Room migration。
- 记录 appStart、appStop、foregroundServiceStart、foregroundServiceStop、listenerConnected、listenerDisconnected、requestRebind 等事件。
- 增加自动监听状态字段。
- 增加 KeepAliveNotificationService 前台服务。
- 增加 BOOT_COMPLETED 启动广播接收器。
- 增加后台稳定设置页。
- 增加后台诊断报告页。
- 增加日、周、月、自定义范围统计。
- 增加分类汇总、来源汇总、账单按天/周/月分组。
- 首页增加监听状态卡片和相关入口。

测试状态：

- App 可安装并打开。
- 首页显示 V6.4 相关卡片。
- 前台服务需要用户主动开启，不做无感启动。

### V6.5：监听服务自恢复增强版

重点是解决 Android / MIUI 上 NotificationListenerService 仍可能失效的问题。

完成内容：

- 增加 ListenerHealthEvaluator，统一判断监听状态：
  - healthy
  - suspicious
  - disconnected
  - permissionMissing
  - serviceUnknown
- 在 KeepAliveNotificationService 中增加监听看门狗。
- 默认 10 分钟检查一次监听健康状态。
- App 启动、前台服务启动、首页刷新时触发一次健康检查。
- 对 suspicious / disconnected 状态自动尝试 requestRebind。
- 增加 requestRebind 冷却逻辑，避免短时间重复调用。
- 在 onListenerDisconnected 中记录断开日志，并延迟尝试恢复。
- 增加 MY_PACKAGE_REPLACED 接收器，App 更新后标记需要重新检查监听权限。
- 前台通知根据健康状态动态显示：
  - 自动记账监听中
  - 监听可能失效，点击修复
  - 监听已断开，请打开 App 修复
  - 通知监听权限未开启
- 首页状态卡增强，显示最近健康检查、自动修复次数、上次 requestRebind 结果和失败原因。
- 后台诊断报告增加 healthCheck、healthy、suspicious、disconnected、requestRebind、packageReplaced 等统计。
- 增加相关单元测试。

测试状态：

- compileDebugKotlin 通过。
- testDebugUnitTest 通过。
- assembleDebug 通过。
- 真机安装成功。
- 监听健康检查、前台通知和自动恢复流程可用。

### V6.6 Test Release：正式测试版封存

重点是做测试版封存，不新增复杂功能。

完成内容：

- 版本更新为：
  - `versionName = 0.6.6-test`
  - `versionCode = 12`
- 首页调整为底部双选项卡结构：
  - 【记账】
  - 【监听】
- 【记账】页集中展示：
  - 总览
  - 今日支出
  - 本周支出
  - 本月支出
  - 总收入
  - 总支出
  - 结余
  - 最近账单
  - 待确认账单入口
  - 手动添加账单入口
  - 截图记账入口
  - 分类规则入口
  - 统计入口
- 【监听】页集中展示：
  - 自动监听状态
  - 前台服务状态
  - 通知监听权限状态
  - 监听健康状态
  - 最近健康检查时间
  - 最近收到通知时间
  - 最近微信/支付宝通知时间
  - 一键修复按钮
  - 后台稳定设置入口
  - 后台诊断报告入口
  - 真实通知测试入口
- 更新并生成封存文档：
  - README.md
  - CHANGELOG.md
  - PROJECT_STATUS.md
  - TEST_REPORT.md
- 生成测试 APK。
- 生成 release 文件夹和 ZIP 封存包。
- 安装到 Redmi K60 真机验证。

构建结果：

- `compileDebugKotlin`：通过
- `testDebugUnitTest`：通过
- `assembleDebug`：通过

真机验证结果：

- 测试手机：Redmi K60
- 连接方式：USB 调试
- APK 覆盖安装：成功，`adb install -r` 返回 `Success`
- App 可启动。
- 底部【记账】页可打开。
- 底部【监听】页可打开。
- 【记账】页显示 V0.6.6 Test Release、总览、今日/本周/本月统计、记账入口和最近账单。
- 统计页可打开，显示今日、本周、本月、自定义范围、核心数据、分类统计、来源统计和账单分组。
- 【监听】页显示通知监听权限、监听健康状态、自动监听、前台服务、最近通知、最近微信/支付宝通知、一键修复等信息。
- 自动监听恢复成功。
- 真机 UI 显示：
  - 自动监听：已开启
  - 前台服务：运行中
  - 监听健康状态：healthy
- 通知栈确认本 App 前台通知存在，标题为“自动记账监听中”。
- 后台稳定设置页可打开。
- 后台诊断报告页可打开，并显示最近 24 小时健康检查、requestRebind、微信/支付宝通知、付款通知、解析失败等统计。

封存产物：

- APK：
  - `F:\Software Development\MobileBookkeepingApp\app\build\outputs\apk\debug\app-debug.apk`
- Release 文件夹：
  - `F:\Software Development\MobileBookkeepingApp\release\LocalBookkeeping_V6.6_Test`
- ZIP：
  - `F:\Software Development\MobileBookkeepingApp\release\LocalBookkeeping_V6.6_Test.zip`

## 当前已完成功能总览

- 本地账单记录。
- 手动添加收入、支出。
- 待确认账单确认、编辑、忽略。
- 微信/支付宝真实付款通知监听。
- 付款通知金额、商户、收入/支出类型解析。
- 无金额通知待补录。
- 手动从通知生成账单。
- 通知调试与真实通知测试。
- 自动监听前台服务。
- 监听健康检查。
- requestRebind 自恢复尝试。
- App 更新后监听权限复查提示。
- 后台稳定设置页。
- 后台诊断报告页。
- 今日、本周、本月、自定义时间范围统计。
- 分类统计。
- 来源统计。
- 账单按天/周/月分组查看。
- 底部双选项卡 UI。

## 当前已知问题

- 部分通知可能重复记录，当前测试版暂不影响核心记账流程。
- Android / MIUI 仍可能在强省电策略下限制或回收 NotificationListenerService。
- requestRebind 只能作为恢复尝试，不能保证系统一定重新连接。
- 小米 / Redmi / MIUI / HyperOS 仍需要用户手动设置后台无限制、自启动和通知权限。
- 微信、支付宝通知文案由第三方 App 和系统共同决定，未来文案变化可能导致解析规则需要继续补充。
- 如果微信/支付宝隐藏通知内容，App 无法从通知中解析金额和商户。

## 后续版本建议

### V6.7 建议方向

- 优先处理重复通知记录问题。
- 增加通知去重策略：
  - 按 packageName
  - 按 rawText hash
  - 按金额
  - 按商户
  - 按短时间窗口
- 对重复通知只保留一条待确认账单。
- 在诊断报告中增加重复通知统计。

### 后续优化方向

- 继续收集真实微信/支付宝付款通知样本。
- 增强解析规则覆盖率。
- 优化待确认账单列表的筛选和批量处理。
- 优化统计页筛选体验。
- 增加更多机型测试记录。
- 增加更清晰的用户引导，帮助完成 MIUI 后台设置。

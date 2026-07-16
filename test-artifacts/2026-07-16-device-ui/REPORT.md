# Xiaomi 真机测试报告

## 测试环境

- 设备：Xiaomi 2510DRK44C（annibale）
- 系统：Android 16
- 屏幕：1156 × 2510
- 安装方式：`adb install -r` 覆盖安装，保留应用数据
- 测试包：`app/build/outputs/apk/debug/app-debug.apk`
- APK SHA-256：`EE6488FB914812AF11FAD6BF5BCD24324AC4C43A564FF27E8C7E93C31A1A1127`

## 真机结论

- 覆盖安装成功，主 Activity 启动成功，实测启动耗时 385–490 ms。
- 首页、统计、每日限额、监听、新增账单、待确认、账单详情、全部工具均可正常打开。
- 页面在 1156 × 2510 屏幕上无横向溢出，标题、表单、图表、卡片和底部导航未重叠。
- 统计页本月支出、收入、结余、趋势和分类占比均由现有真实账单计算。
- 新增账单页仅打开和返回，未保存测试账单；待确认页未确认、忽略或编辑数据；详情页未编辑或删除数据。
- 原 Room 数据库仍存在，首页账单、1 条待确认记录和统计数据在覆盖安装后完整保留。
- 自动监听恢复后 `KeepAliveNotificationService` 为 `isForeground=true`、`startRequested=true`，通知监听连接显示稳定。
- Logcat 未发现 `FATAL EXCEPTION`、应用 ANR 或 AndroidRuntime 崩溃。

## 真机发现并修复

1. 首页“今日支出”原先误用了累计支出，曾显示 `¥5052.19`；已改为按 `TODAY` 时间范围计算，复测正确显示 `¥0.00`。
2. 未完成首次使用引导时，自动监听启动/恢复弹窗可能遮挡引导；现已在引导页面禁止执行该启动提示检查。

## 自动验证

- Kotlin 编译与 Debug APK 生成成功。
- 单元测试：84 项，0 失败，0 错误，0 跳过。
- Android Lint：0 错误、50 条非阻断警告、4 条信息。
- `git diff --check`：通过。

## 截图索引

- `04-home.png`：修复前首页，用于复现今日支出口径问题。
- `05-stats.png`：统计页。
- `06-limit.png`：每日限额页。
- `07-listener.png`、`08-listener-top.png`：监听运行与诊断区域。
- `09-add.png`：新增账单页。
- `10-pending-app.png`：待确认账单页。
- `11-record-detail-app.png`：账单详情页。
- `12-tools-app.png`：全部工具页。
- `13-relaunch.png`：覆盖安装后的监听恢复提示。
- `14-fixed-home.png`：修复并恢复监听后的最终首页，今日支出为 `¥0.00`。

说明：`10-pending.png`、`11-record-detail.png`、`12-tools.png` 是早期坐标脚本退到系统桌面后的无效过程截图，不计入测试结论；带 `-app` 后缀的文件才是对应应用页面的有效截图。

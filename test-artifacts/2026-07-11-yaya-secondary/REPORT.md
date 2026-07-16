# 芽芽记账青芽日常 UI 接入验收报告

日期：2026-07-11
设备：Xiaomi 2510DRK44C（1156 × 2510）
版本：V1.1.4 / versionCode 114

## 接入范围

- 使用 `design-prototypes` 中第一个“青芽日常”绿色方案。
- 软件名称统一为“芽芽记账”，并更新绿色嫩芽启动图标。
- 首页、统计、限额、监听四个主入口和中央记账按钮完成迁移。
- 待确认、账单详情、新增/编辑、截图识别、备份恢复、分类规则、学习记录、监听设置、诊断与测试页面统一使用青芽配色、圆角、标题、状态横幅和空状态。
- 保留 Room、通知监听、支付解析、限额、备份恢复与账单数据流，不改变业务语义。
- 补齐浅色系统状态栏和导航栏图标适配。

## 自动验证

- `testDebugUnitTest`：84 项通过，0 失败。
- `assembleDebug`：通过。
- `git diff --check`：通过。
- APK：`app/build/outputs/apk/debug/app-debug.apk`。

## 真机验证

- `adb install -r`：成功，保留原有应用数据。
- 首页、中央记账入口、新增账单、监听状态和监听工具可正常打开。
- 页面滚动、底部导航、返回操作正常。
- 状态栏时间/信号与底部系统导航在浅色背景上清晰可见。
- 复测期间未发现崩溃或 ANR。
- 真机当前监听状态为“监听疑似失效”，这是设备后台服务状态，不是 UI 接入故障；页面已提供恢复前台服务、一键修复和深度修复入口。

## 截图

- `12-final-add-system-bars.png`：最终首页与深色系统栏图标。
- `13-final-add-system-bars.png`：新增账单页。
- `14-final-listener-system-bars.png`：自动监听页。
- `09-listener-bottom.png`：监听工具入口。

## 原型忠实度修正

收到反馈后，不再仅使用绿色主题，而是按 `design-prototypes/index.html` 的“青芽日常”页面结构重新迁移。最终真机截图保存在 `test-artifacts/2026-07-11-prototype-faithful/`：

- `04-final-home.png`：原型结构首页。
- `05-final-tools.png`：独立全部工具页。
- `06-final-add.png`：原型结构记账表单。
- `07-stats.png`：统计页。
- `08-limit.png`：每日限额页。
- `09-listener.png`：自动监听页面。

# NEXT_TASK

## 后续维护策略

- V1.0.1：只修小 Bug。
- V1.0.2：只做 UI 文案小调整。
- V1.1.0：如有必要再做小功能更新。

## 开发前必读

每次新开发或维护前，必须先阅读：

- `PROJECT_STATUS.md`
- `BUG_LOG.md`
- `NEXT_TASK.md`

## 维护边界

- 不新增大型功能。
- 不重写 UI。
- 不修改 Room 数据库结构。
- 不清空账单数据。
- 不改动通知监听核心逻辑。
- 不改动支付解析核心逻辑。
- 不改动限额计算核心逻辑。
- 不改动备份恢复核心逻辑。

## 推荐节奏

- 优先处理真机反馈的小问题。
- 每次维护都保留 APK 和文档封存包。
- 每次发版都运行 `compileDebugKotlin`、`testDebugUnitTest`、`assembleDebug`。
- 有 USB 调试设备时执行 `adb install -r app-debug.apk` 并做真机页面验证。

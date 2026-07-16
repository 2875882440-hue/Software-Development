# TEST_REPORT

## 测试版本

- 版本：V1.2.1 稳定化维护版
- `versionName`：`1.2.1`
- `versionCode`：`121`
- 构建类型：Debug APK
- 测试日期：2026-07-16
- Room schema：12，未迁移

## 自动验证

- 匿名统计聚合测试：通过
- 匿名统计 30 天清理测试：通过
- 零分母成功率测试：通过
- 统计键隐私约束测试：通过
- 问题日志匿名统计与隐私测试：通过
- `compileDebugKotlin`：通过
- 完整 `testDebugUnitTest`：通过，16 个测试套件、89 项测试、0 失败、0 错误、0 跳过
- `assembleDebug`：通过
- APK：`Yaya-Bookkeeping-V1.2.1-debug.apk`
- APK 大小：55,584,258 bytes（53.01 MB）
- APK SHA-256：`FC33726329C402A3DD4EEEE9074415C1B7D65801BD83720604B47C7C41058B5D`

## V1.2.1 回归范围

- 通知服务只统计已勾选监听应用。
- 本 App probe、未勾选应用、模拟通知、截图和手动记账不计入成功率。
- 微信、支付宝、其他应用按来源分组。
- 用户确认、实际改金额和删除只对通知来源账单计数。
- 成功率页面显示最近 14 天总体与来源分项。
- 问题日志只包含匿名数字，不包含通知正文、商户、金额、备注或账单内容。
- README、PROJECT_STATUS、NEXT_TASK、BUG_LOG 和 CHANGELOG 版本一致。

## 真机验证

- 设备：Xiaomi 2510DRK44C，ADB 序列号 `f56d6e13`
- ADB 设备连接：通过
- 覆盖安装：通过，`adb install -r` 返回 `Success`
- 安装版本：通过，设备报告 `versionName=1.2.1`、`versionCode=121`
- APP 启动：通过
- 中文 UI 与四个底部 Tab：通过
- 原有账单保留：通过，首页仍显示既有账单和月度数据
- 通知监听权限：已开启
- 自动监听：已开启
- NotificationListenerService：已绑定
- KeepAliveNotificationService：前台运行
- 监听页状态：监听正常、连接稳定
- 成功率页面打开：通过
- 成功率空数据状态：通过
- 总体 / 微信 / 支付宝 / 其他应用分项：通过
- 隐私说明：通过
- 启动与页面验证期间 FATAL EXCEPTION：未发现
- 微信付款码支付：待真机
- 微信扫码支付：待真机
- 支付宝支付：待真机
- 锁屏一小时后支付：待真机
- 重启后支付：待真机
- 次日监听：待长期测试

## 结论

V1.2.1 的统计核心、事件接入、报告页面、匿名问题日志、文档整理、89 项单元测试、APK 构建、SHA-256、覆盖安装和主要页面真机验证均已通过。真实微信 / 支付宝支付、锁屏一小时、重启、次日和多品牌长期监听仍必须按 `V1.2.1_DEVICE_TEST_PLAN.md` 人工完成。

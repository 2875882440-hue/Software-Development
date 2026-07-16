# 导出 UI 替换验证报告

## 范围

- UI 基准：`F:\Software Development\1\01-首次使用.png` 至 `14-系统设置.png`
- 原型代码基准：`design-prototypes/index.html`、`styles.css`、`app.js`、`assets/mascot-yaya.svg`
- 正式工程：`app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- 核心约束：不修改 Room 表结构、通知支付解析、账单仓库、备份格式或统计计算算法。

## 已完成

- 新增首次使用引导并接入通知监听授权。
- 保留并校准首页、待确认、新增账单、账单详情、截图识别、全部工具、分类规则、备份恢复和故障诊断页面。
- 重做统计、每日限额、自动监听与系统设置页面。
- 所有金额、趋势、分类、监听状态和应用列表均来自真实应用状态；没有写入设计稿示例数据。
- 新增每日限额“今天不再提醒”的可逆设置。
- 将原型的账本芽芽 SVG 迁移为 Android 矢量资源并用于引导、监听与空状态。
- 截图识别页补齐“换张截图”；分类规则编辑器默认折叠；全部工具按原型收敛为 8 个入口。
- 备份页按 JSON 完整备份、CSV 表格导出、JSON 恢复三张卡片重新校准。

## 原型运行审计

- 使用本地 HTTP 服务实际运行 `design-prototypes` 的“青芽日常”主题。
- 依次打开 14 个代表页面，页面标题均与页面地图匹配。
- 14 页 `scrollWidth` 均等于 `clientWidth`，未发现横向溢出。
- 浏览器控制台：0 条 error，0 条 warning。

## 自动验证

- `gradlew.bat compileDebugKotlin testDebugUnitTest --no-daemon`：通过。
- 单元测试：84 项，0 失败，0 错误。
- `gradlew.bat assembleDebug`：通过，产物 `app/build/outputs/apk/debug/app-debug.apk`。
- APK SHA-256：`5F6D0D3678A2096152CB27C3B0A0CBD2AEB0194BC7217D5B989863BC163DE4F4`。
- `gradlew.bat lintDebug`：0 错误、50 条非阻断警告。
- `git diff --check`：通过。

## 真机限制

本轮执行时 `adb devices` 未发现连接设备。发现的 `BookkeepingTestApi35` AVD 因未安装 Android Emulator Hypervisor Driver 无法硬件加速启动，纯软件加速也未能在验证窗口内连接 ADB，因此无法生成本轮 14 页设备截图。代码保留了现有真机测试入口；连接设备后应重点核对首次使用页、统计图、限额页、监听开关、监听应用设置和系统返回行为。

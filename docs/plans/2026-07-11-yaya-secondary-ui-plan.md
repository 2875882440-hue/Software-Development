# 芽芽记账二级页面完整优化 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将仍停留在旧式卡片堆叠状态的二级页面完整迁移到“青芽日常”设计语言，并统一高频流程、工具页面、诊断页面和状态反馈。

**Architecture:** 保持现有 `MainActivity.kt` 的业务状态、导航枚举和回调不变，新增一组私有 Compose 语义组件：二级页标题、状态横幅、空态、分组标题、金额主视觉和危险操作区。逐页替换重复布局，但不改 Room、通知解析、限额、备份或监听逻辑。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、现有青芽设计令牌、ADB 真机验证。

---

### Task 1: 高频记账流程

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`

**Steps:**
1. 新增统一二级页标题、空态、状态横幅和分组标题组件。
2. 重构待确认、账单详情、截图识别、快速补录、新增/编辑账单。
3. 修正截图识别页错误标题“统计”。
4. 将删除、忽略等危险操作与主操作视觉分离。
5. 运行 `compileDebugKotlin`。

### Task 2: 数据工具与规则

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`

**Steps:**
1. 重构备份恢复、分类规则、学习记录、监控应用和后台设置。
2. 用说明卡解释本地隐私、重复跳过、分类学习和权限影响。
3. 统一正常、处理中、成功、失败和空数据状态。
4. 运行 `testDebugUnitTest`。

### Task 3: 诊断页面、回归与真机

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Modify: `CHANGELOG.md`
- Create: `test-artifacts/2026-07-11-yaya-secondary/REPORT.md`

**Steps:**
1. 统一监听健康、后台报告、问题排查、真实通知测试、扫码测试和通知日志页面。
2. 默认展示用户结论，把原始通知和技术字段放入折叠详情。
3. 运行 `testDebugUnitTest assembleDebug`。
4. 使用 `adb install -r` 覆盖安装并保留数据。
5. 真机检查高频页、工具页和诊断页，修复后重新验证。

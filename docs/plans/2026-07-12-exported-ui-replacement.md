# 导出 UI 全量替换 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 `F:\Software Development\1` 中 14 张导出页面稿忠实迁移到现有 Android 应用，同时保持记账、通知监听、统计、限额、备份恢复和诊断等核心业务逻辑不变。

**Architecture:** 保留现有 `MainViewModel`、Room、Repository、通知解析和系统权限回调，以现有 `Screen` 路由为骨架，仅重构 `MainActivity.kt` 中的 Compose 页面和共享视觉组件。页面数据继续来自真实状态；设计稿中的示例金额与商户不写入应用。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Android Gradle Plugin、JUnit、ADB（可用时）。

---

### Task 1: 建立基线与页面映射

**Files:**
- Inspect: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Inspect: `app/src/main/java/com/localbookkeeping/app/MainViewModel.kt`
- Inspect: `F:\Software Development\1\01-首次使用.png` 至 `14-系统设置.png`

**Steps:**
1. 运行 `gradlew.bat compileDebugKotlin testDebugUnitTest`，记录当前基线。
2. 将 14 张设计稿映射到现有路由、状态和回调。
3. 标记设计稿中没有对应业务能力的纯展示元素，避免制造假功能或假数据。

### Task 2: 实现共享设计系统与应用骨架

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Modify when required: `app/src/main/res/values/colors.xml`
- Modify when required: `app/src/main/res/values/styles.xml`

**Steps:**
1. 统一浅米白背景、绿色主色、圆角卡片、标题、状态横幅、按钮和表单视觉。
2. 统一系统栏、安全区、底部导航、返回按钮和页面边距。
3. 编译 `compileDebugKotlin`，修复布局或 API 使用错误。

### Task 3: 替换核心记账与数据页面

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`

**Steps:**
1. 对照设计稿替换首次使用、首页、待确认、新增账单、账单详情和截图识别页。
2. 保留新增、编辑、确认、忽略、删除、截图解析和保存回调。
3. 对照设计稿替换统计与每日限额页，所有金额和图表均使用真实状态。
4. 运行 `compileDebugKotlin testDebugUnitTest`。

### Task 4: 替换监听、工具与设置页面

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`

**Steps:**
1. 对照设计稿替换自动监听、全部工具、分类规则、备份恢复、故障诊断和系统设置页。
2. 保留通知权限、监听开关、应用过滤、规则管理、导出恢复与诊断回调。
3. 检查所有工具入口可达、返回路径正确、禁用和错误状态可见。
4. 运行 `compileDebugKotlin testDebugUnitTest`。

### Task 5: 回归验证与交付

**Files:**
- Modify: `CHANGELOG.md`
- Create: `test-artifacts/2026-07-12-exported-ui/REPORT.md`

**Steps:**
1. 运行 `gradlew.bat testDebugUnitTest assembleDebug lintDebug`。
2. 若 ADB 设备可用，覆盖安装 debug APK 并逐页截图核对 14 个页面与关键流程。
3. 检查崩溃日志、返回行为、表单保存、空状态、权限状态和现有数据保留。
4. 记录测试结果、产物路径和仍需真机人工验证的限制。

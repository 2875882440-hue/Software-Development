# 芽芽记账青芽日常主题 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将用户选定的“青芽日常”视觉方案迁移到正式 Android APP，并把软件名称统一改为“芽芽记账”。

**Architecture:** 保留现有单 Activity、Jetpack Compose 页面和全部业务回调，只替换产品命名、全局 Material 设计令牌、主页信息层级、导航图标、卡片外观与可见状态文案。原型资源仅作为视觉参考，不接入真实数据层。

**Tech Stack:** Kotlin、Jetpack Compose Material 3、Android VectorDrawable、Gradle。

---

### Task 1: 产品命名与青芽设计基础

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Modify: `app/src/main/java/com/localbookkeeping/app/diagnostics/ProblemLogExporter.kt`
- Modify: `app/src/test/java/com/localbookkeeping/app/diagnostics/ProblemLogExporterTest.kt`
- Modify: `README.md`
- Modify: `USER_GUIDE.md`

**Steps:**
1. 将桌面标签、首页标题、问题日志和系统设置指引中的产品名改为“芽芽记账”。
2. 更新启动图标为绿色账本与嫩芽图形。
3. 在 Compose 中定义青芽配色、圆角、按钮和表面令牌。
4. 运行 `compileDebugKotlin`，预期成功。

### Task 2: 全页面视觉迁移与首页重构

**Files:**
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`

**Steps:**
1. 让所有卡片、页面背景和 Material 控件使用青芽设计令牌。
2. 为底部导航补充明确图形标识与选中态。
3. 重构首页标题、监听状态、月度概览、快捷操作和最近账单层级。
4. 增加可复用“芽芽”矢量化 Compose 吉祥物组件。
5. 保持全部业务参数、回调和数据计算不变。
6. 运行 `compileDebugKotlin`，预期成功。

### Task 3: 回归验证与交付

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `PROJECT_LOG.md`

**Steps:**
1. 运行 `testDebugUnitTest`，预期全部通过。
2. 运行 `assembleDebug`，预期生成 debug APK。
3. 检查 APK、应用标签和修改文件清单。
4. 记录“芽芽记账”命名与青芽日常 UI 更新。

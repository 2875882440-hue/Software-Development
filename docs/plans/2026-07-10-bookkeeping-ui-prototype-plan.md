# 自动记账 APP 三主题交互原型 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建覆盖整个自动记账 APP 的三主题可交互 UI 原型，供用户在修改 Android 业务界面前比较和选择。

**Architecture:** 在 `design-prototypes/` 中创建无构建步骤的静态原型。HTML 提供桌面展示框架，JavaScript 按页面配置渲染手机界面，CSS 使用语义化设计令牌实现“青芽日常、阳光票据、薄荷数据”三套主题。原型与 Android 源码完全隔离。

**Tech Stack:** HTML5、CSS3、原生 JavaScript、内联 SVG。

---

### Task 1: 原型外壳与主题令牌

**Files:**
- Create: `design-prototypes/index.html`
- Create: `design-prototypes/styles.css`
- Create: `design-prototypes/app.js`

**Step 1:** 创建桌面评审区、主题切换器、页面导航与 390px 手机画板。

**Step 2:** 定义三套主题的颜色、圆角、阴影、字号和间距令牌。

**Step 3:** 在 390px、768px 和 1440px 宽度检查无横向溢出。

### Task 2: 品牌组件与吉祥物

**Files:**
- Create: `design-prototypes/assets/mascot-yaya.svg`
- Modify: `design-prototypes/styles.css`
- Modify: `design-prototypes/app.js`

**Step 1:** 绘制“小账本 + 绿芽”矢量吉祥物。

**Step 2:** 建立状态横幅、金额卡、功能图标、账单行、标签和主按钮组件。

**Step 3:** 验证 SVG 在亮色卡片和三套主题下均清晰。

### Task 3: 核心记账流程

**Files:**
- Modify: `design-prototypes/app.js`
- Modify: `design-prototypes/styles.css`

**Step 1:** 实现首次使用、记账首页、待确认、新增账单、账单详情和截图识别页面。

**Step 2:** 接通首页到新增、待确认和详情的点击路径。

**Step 3:** 验证金额、分类、日期、来源和操作状态均完整显示。

### Task 4: 数据与系统页面

**Files:**
- Modify: `design-prototypes/app.js`
- Modify: `design-prototypes/styles.css`

**Step 1:** 实现统计、限额、监听和全部工具页面。

**Step 2:** 实现分类规则、备份恢复、监听诊断和设置类页面。

**Step 3:** 验证正常、异常、空白和成功状态。

### Task 5: 浏览器验证与交付

**Files:**
- Create: `design-prototypes/README.md`
- Modify: `design-prototypes/*`（仅用于修复验证问题）

**Step 1:** 使用本地 HTTP 服务打开原型。

**Step 2:** 逐一切换三套主题和代表页面，检查控制台错误、文本溢出、点击导航与响应式布局。

**Step 3:** 保存代表性截图并在 README 中记录预览方式、页面覆盖和已知边界。

**Step 4:** 运行现有 Android 单元测试，确认独立原型未影响项目构建。

const screenMeta = [
  { id: "onboarding", label: "首次使用", title: "首次使用引导" },
  { id: "home", label: "记账首页", title: "记账首页" },
  { id: "pending", label: "待确认", title: "待确认账单" },
  { id: "add", label: "新增账单", title: "记一笔" },
  { id: "detail", label: "账单详情", title: "账单详情" },
  { id: "scan", label: "截图识别", title: "截图记账" },
  { id: "statistics", label: "统计分析", title: "统计分析" },
  { id: "limit", label: "每日限额", title: "每日限额" },
  { id: "listener", label: "自动监听", title: "自动监听" },
  { id: "tools", label: "全部工具", title: "全部工具" },
  { id: "rules", label: "分类规则", title: "分类规则" },
  { id: "backup", label: "备份恢复", title: "备份与恢复" },
  { id: "diagnostics", label: "故障诊断", title: "自动记账诊断" },
  { id: "settings", label: "系统设置", title: "设置与监控" }
];

const themeNotes = {
  seed: "最贴近使用说明的品牌气质，用圆润卡片和清晰状态降低自动记账的理解成本。",
  receipt: "把账单变成生活票据，黄色与珊瑚色提升情绪感，适合更有个性的个人账本。",
  mint: "保留绿色亲和力，同时收紧卡片和数据密度，统计、限额与诊断页面效率更高。"
};

const iconPaths = {
  home: '<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10.5V20h13v-9.5"/><path d="M9.5 20v-6h5v6"/>',
  chart: '<path d="M4 19V9"/><path d="M10 19V5"/><path d="M16 19v-7"/><path d="M22 19H2"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/>',
  wallet: '<path d="M4 6h14a2 2 0 0 1 2 2v10H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h12"/><path d="M15 11h7v4h-7a2 2 0 0 1 0-4Z"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  scan: '<path d="M4 8V4h4M16 4h4v4M20 16v4h-4M8 20H4v-4"/><path d="M8 12h8"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  tools: '<path d="m14 6 4-4 4 4-4 4"/><path d="m3 21 8.5-8.5"/><path d="M8 4H3v5l12 12h5v-5Z"/>',
  chevron: '<path d="m9 18 6-6-6-6"/>',
  back: '<path d="m15 18-6-6 6-6"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  alert: '<path d="M12 8v5"/><path d="M12 17h.01"/><path d="M10.3 3.7 2.5 18a2 2 0 0 0 1.8 3h15.4a2 2 0 0 0 1.8-3L13.7 3.7a2 2 0 0 0-3.4 0Z"/>',
  shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/><path d="m9 12 2 2 4-4"/>',
  image: '<rect x="3" y="4" width="18" height="16" rx="3"/><circle cx="9" cy="9" r="2"/><path d="m21 15-5-5L5 20"/>',
  rule: '<path d="M4 6h16M4 12h16M4 18h16"/><circle cx="9" cy="6" r="2"/><circle cx="15" cy="12" r="2"/><circle cx="7" cy="18" r="2"/>',
  backup: '<path d="M20 15a4 4 0 0 0-4-4h-1a7 7 0 1 0-7 8h8"/><path d="m12 15 4 4 4-4M16 19v-8"/>',
  apps: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
  edit: '<path d="M12 20h9"/><path d="m16.5 3.5 4 4L8 20l-5 1 1-5Z"/>',
  trash: '<path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14"/>',
  lock: '<rect x="4" y="10" width="16" height="11" rx="3"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 10h18"/>',
  spark: '<path d="m12 3 1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5ZM19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8Z"/>',
  camera: '<path d="M4 7h3l2-3h6l2 3h3v13H4Z"/><circle cx="12" cy="13" r="4"/>',
  bug: '<path d="M8 8h8v8a4 4 0 0 1-8 0Z"/><path d="M9 8a3 3 0 0 1 6 0M4 13h4M16 13h4M5 8l3 2M19 8l-3 2M5 19l3-2M19 19l-3-2M12 11v8"/>'
};

let currentScreen = "home";
let currentTheme = "seed";
let toastTimer;

function icon(name, className = "") {
  return `<svg class="icon ${className}" viewBox="0 0 24 24" aria-hidden="true">${iconPaths[name] || iconPaths.spark}</svg>`;
}

function header(title, subtitle = "", backTo = "") {
  return `<header class="app-header">
    <div>${subtitle ? `<p class="subhead">${subtitle}</p>` : ""}<h3>${title}</h3></div>
    ${backTo ? `<button class="icon-button" type="button" data-screen="${backTo}" aria-label="返回">${icon("back")}</button>` : `<button class="icon-button" type="button" data-screen="tools" aria-label="全部工具">${icon("apps")}</button>`}
  </header>`;
}

function bottomNav(active) {
  const item = (id, label, iconName) => `<button class="nav-item ${active === id ? "is-active" : ""}" type="button" data-screen="${id}">${icon(iconName)}<span>${label}</span></button>`;
  return `<nav class="bottom-nav" aria-label="主要导航">
    ${item("home", "记账", "home")}
    ${item("statistics", "统计", "chart")}
    <button class="nav-item nav-fab" type="button" data-screen="add" aria-label="记一笔">${icon("plus")}</button>
    ${item("limit", "限额", "wallet")}
    ${item("listener", "监听", "bell")}
  </nav>`;
}

function appView(content, active = "", noTabs = false) {
  return `<div class="app-view ${noTabs ? "no-tabs" : ""}" data-active-tab="${noTabs ? "" : active}">${content}</div>`;
}

function billRow(iconText, iconClass, merchant, meta, amount, type = "expense", action = "detail") {
  return `<div class="bill-row" data-screen="${action}" role="button" tabindex="0">
    <div class="category-icon ${iconClass}">${iconText}</div>
    <div class="bill-main"><strong>${merchant}</strong><span>${meta}</span></div>
    <div class="bill-amount ${type}">${type === "expense" ? "−" : "+"}${amount}<small>${type === "expense" ? "支出" : "收入"}</small></div>
  </div>`;
}

function onboardingScreen() {
  return appView(`<div class="onboarding">
    <div>
      <div class="onboarding-art"><img class="mascot large" src="assets/mascot-yaya.svg" alt="账本芽芽吉祥物"></div>
      <h3>你好，我是芽芽</h3>
      <p>打开自动监听后，微信和支付宝付款就会自动生成账单。账单只保存在你的手机里。</p>
      <div class="feature-pills"><span>纯本地</span><span>自动记录</span><span>隐私安全</span></div>
      <div class="app-card timeline" style="text-align:left">
        <div class="timeline-step"><span class="step-dot">1</span><div><strong>开启通知监听权限</strong><p>只读取你选择的支付应用通知。</p></div></div>
        <div class="timeline-step"><span class="step-dot">2</span><div><strong>打开自动监听</strong><p>通知栏会显示“自动记账监听中”。</p></div></div>
        <div class="timeline-step"><span class="step-dot">3</span><div><strong>正常付款就会记账</strong><p>有待确认账单时，回到首页补充分类即可。</p></div></div>
      </div>
    </div>
    <button class="primary-button full-width" type="button" data-screen="listener">开始设置自动记账</button>
  </div>`, "", true);
}

function homeScreen() {
  return appView(`${header("早上好，今天也要轻松记账", "7 月 10 日 · 星期五")}
    <div class="listen-strip"><div class="strip-icon">${icon("shield")}</div><div><strong>自动记账运行正常</strong><p>微信、支付宝通知正在监听</p></div><button data-screen="listener">查看</button></div>
    <section class="hero-card">
      <p class="overline">本月还可以放心花</p>
      <p class="money"><small>¥</small> 3,820.00</p>
      <div class="hero-meta"><div><span>本月支出</span><strong>¥2,680.00</strong></div><div><span>本月预算</span><strong>¥6,500.00</strong></div><div><span>剩余天数</span><strong>21 天</strong></div></div>
    </section>
    <div class="section-title"><h4>快捷记账</h4><button data-screen="tools">全部工具</button></div>
    <div class="quick-grid">
      <button class="quick-action" data-screen="add"><span class="action-icon">${icon("edit")}</span><span>手动记账</span></button>
      <button class="quick-action" data-screen="pending"><span class="action-icon">${icon("clock")}</span><span>待确认 · 2</span></button>
      <button class="quick-action" data-screen="scan"><span class="action-icon">${icon("scan")}</span><span>截图记账</span></button>
      <button class="quick-action" data-toast="已打开扫码支付后补录"><span class="action-icon">${icon("camera")}</span><span>扫码补录</span></button>
    </div>
    <div class="section-title"><h4>最近账单</h4><button data-toast="已切换到全部账单">查看全部</button></div>
    <div class="bill-list">
      ${billRow("餐", "food", "瑞幸咖啡", "餐饮 · 微信 · 10:26", "18.00")}
      ${billRow("行", "travel", "滴滴出行", "交通 · 支付宝 · 昨天", "32.50")}
      ${billRow("购", "shopping", "盒马鲜生", "购物 · 支付宝 · 7 月 8 日", "126.80")}
    </div>`, "home");
}

function pendingScreen() {
  return appView(`${header("待确认账单", "付款通知已识别，请核对后入账", "home")}
    <div class="notice"><div>${icon("spark")}</div><div><strong>芽芽找到了 2 笔新账单</strong><p>金额已经识别完成，只需确认分类和商户。</p></div></div>
    <div class="section-title"><h4>今天</h4><span class="status-pill warn"><i class="status-dot"></i>2 笔待确认</span></div>
    <div class="card-stack">
      <article class="app-card">
        <div class="card-head"><div><h4>微信支付</h4><p>今天 12:36 · 自动识别</p></div><strong class="bill-amount expense">−¥36.00</strong></div>
        <div class="chips"><button class="chip is-active">餐饮</button><button class="chip">购物</button><button class="chip">交通</button><button class="chip">其他</button></div>
        <div class="field" style="margin-top:12px"><label>商户名称 <span>识别结果</span></label><input value="兰州牛肉面"></div>
        <div class="button-row"><button class="secondary-button" data-toast="已暂时忽略这笔账单">稍后处理</button><button class="primary-button" data-toast="账单已确认">确认入账</button></div>
      </article>
      <article class="app-card">
        <div class="card-head"><div><h4>支付宝</h4><p>今天 09:12 · 金额待补充</p></div><strong class="bill-amount expense">−¥12.00</strong></div>
        <p style="margin:0 0 12px;color:var(--muted);font-size:9px">通知内容较少，请选择分类后确认。</p>
        <div class="button-row"><button class="secondary-button" data-toast="已删除待确认账单">删除</button><button class="primary-button" data-screen="add">补充信息</button></div>
      </article>
    </div>`, "home");
}

function addScreen() {
  return appView(`${header("记一笔", "数据只保存在本机", "home")}
    <div class="segmented type-switch"><button class="is-active">支出</button><button>收入</button></div>
    <div class="amount-input"><span>¥</span><input value="36.00" inputmode="decimal" aria-label="金额"></div>
    <section class="app-card form-card">
      <div class="field"><label>分类 <span>常用分类</span></label><div class="chips"><button class="chip is-active">🍜 餐饮</button><button class="chip">🛒 购物</button><button class="chip">🚕 交通</button><button class="chip">🏠 居家</button><button class="chip">＋ 更多</button></div></div>
      <div class="field"><label for="merchant">商户</label><input id="merchant" value="兰州牛肉面"></div>
      <div class="field"><label for="date">日期与时间</label><input id="date" value="2026-07-10 12:36"></div>
      <div class="field"><label for="source">来源</label><select id="source"><option>手动记账</option><option>微信</option><option>支付宝</option></select></div>
      <div class="field"><label for="note">备注 <span>选填</span></label><input id="note" placeholder="写点什么吧"></div>
    </section>
    <button class="primary-button full-width" style="margin-top:12px" data-toast="已保存：餐饮 ¥36.00">保存账单</button>`, "", true);
}

function detailScreen() {
  return appView(`${header("账单详情", "已自动记账", "home")}
    <div class="detail-amount"><div class="category-icon food">餐</div><span>瑞幸咖啡</span><strong>−¥18.00</strong></div>
    <div class="detail-list">
      <div class="detail-line"><span>分类</span><strong>餐饮</strong></div>
      <div class="detail-line"><span>支付时间</span><strong>2026-07-10 10:26</strong></div>
      <div class="detail-line"><span>记录来源</span><strong>微信 · 自动记账</strong></div>
      <div class="detail-line"><span>确认状态</span><strong style="color:var(--primary)">已确认</strong></div>
      <div class="detail-line"><span>备注</span><strong>冰生椰拿铁</strong></div>
    </div>
    <div class="page-intro" style="margin-top:12px"><div><h4>分类越来越准啦</h4><p>你上次把“瑞幸咖啡”归为餐饮，芽芽已经记住了。</p></div><img class="mascot" src="assets/mascot-yaya.svg" alt="芽芽"></div>
    <div class="button-row"><button class="danger-button" data-toast="删除操作需要再次确认">${icon("trash")} 删除</button><button class="primary-button" data-screen="add">${icon("edit")} 编辑账单</button></div>`, "", true);
}

function scanScreen() {
  return appView(`${header("截图记账", "芽芽已识别截图中的付款信息", "home")}
    <div class="scan-preview"><div class="receipt-paper">微信支付<br><br>付款给：盒马鲜生<hr>商品：日用百货<br>时间：07-08 19:42<hr><strong>¥126.80</strong></div></div>
    <div class="listen-strip"><div class="strip-icon">${icon("check")}</div><div><strong>识别完成</strong><p>金额与商户置信度较高，请确认分类</p></div><span class="status-pill">98%</span></div>
    <section class="app-card form-card">
      <div class="field"><label>识别金额</label><input value="126.80"></div>
      <div class="field"><label>识别商户</label><input value="盒马鲜生"></div>
      <div class="field"><label>推荐分类 <span>根据历史账单</span></label><div class="chips"><button class="chip is-active">购物</button><button class="chip">餐饮</button><button class="chip">居家</button></div></div>
    </section>
    <div class="button-row" style="margin-top:12px"><button class="secondary-button" data-toast="已重新选择截图">换张截图</button><button class="primary-button" data-toast="截图账单已保存">确认入账</button></div>`, "", true);
}

function statisticsScreen() {
  return appView(`${header("统计", "每一笔都算得明明白白")}
    <div class="segmented"><button>今日</button><button>本周</button><button class="is-active">本月</button><button>自定义</button></div>
    <div class="metric-grid" style="margin-top:10px"><div class="metric expense"><span>本月支出</span><strong>¥2,680</strong></div><div class="metric income"><span>本月收入</span><strong>¥8,200</strong></div><div class="metric"><span>结余</span><strong>¥5,520</strong></div></div>
    <div class="section-title"><h4>支出趋势</h4><button data-toast="已切换图表维度">按周查看</button></div>
    <section class="app-card chart-card">
      <div class="bar-chart">
        <div class="bar-group"><i class="bar expense" style="height:42%"></i><i class="bar" style="height:66%"></i></div>
        <div class="bar-group"><i class="bar expense" style="height:75%"></i><i class="bar" style="height:43%"></i></div>
        <div class="bar-group"><i class="bar expense" style="height:55%"></i><i class="bar" style="height:79%"></i></div>
        <div class="bar-group"><i class="bar expense" style="height:88%"></i><i class="bar" style="height:59%"></i></div>
      </div><div class="chart-labels"><span>第 1 周</span><span>第 2 周</span><span>第 3 周</span><span>第 4 周</span></div>
    </section>
    <div class="section-title"><h4>钱花在哪里</h4><button data-toast="已打开分类明细">明细</button></div>
    <section class="app-card donut-wrap"><div class="donut"></div><div class="legend"><div class="legend-row"><i></i><span>餐饮</span><strong>38%</strong></div><div class="legend-row"><i></i><span>购物</span><strong>23%</strong></div><div class="legend-row"><i></i><span>交通</span><strong>18%</strong></div><div class="legend-row"><i></i><span>其他</span><strong>21%</strong></div></div></section>`, "statistics");
}

function limitScreen() {
  return appView(`${header("每日限额", "把每一天都花在计划里")}
    <section class="app-card">
      <div class="card-head"><div><h4>今天的预算</h4><p>已使用 63%，还剩 ¥74.00</p></div><span class="status-pill"><i class="status-dot"></i>提醒已开启</span></div>
      <div class="budget-ring"><div><span>今日已花</span><strong>¥126</strong><span>/ ¥200</span></div></div>
      <div class="progress-row"><div class="progress-label"><span>餐饮</span><strong>¥76</strong></div><div class="progress-track"><div class="progress-fill" style="--value:60%"></div></div></div>
      <div class="progress-row"><div class="progress-label"><span>交通</span><strong>¥32</strong></div><div class="progress-track"><div class="progress-fill accent" style="--value:25%"></div></div></div>
      <div class="progress-row"><div class="progress-label"><span>其他</span><strong>¥18</strong></div><div class="progress-track"><div class="progress-fill" style="--value:14%"></div></div></div>
    </section>
    <div class="section-title"><h4>限额设置</h4></div>
    <section class="app-card">
      <div class="field"><label>每日最多花多少钱</label><input value="200.00"></div>
      <div class="toggle-row"><div class="row-icon">${icon("bell")}</div><div class="toggle-copy"><strong>接近限额时提醒</strong><span>消费达到 80% 时通知我</span></div><button class="toggle is-on" data-toggle aria-label="切换提醒"></button></div>
      <div class="toggle-row"><div class="row-icon">${icon("calendar")}</div><div class="toggle-copy"><strong>今天不再提醒</strong><span>明天会自动恢复提醒</span></div><button class="toggle" data-toggle aria-label="今天不再提醒"></button></div>
      <button class="primary-button full-width" style="margin-top:12px" data-toast="每日限额已保存">保存限额</button>
    </section>`, "limit");
}

function listenerScreen() {
  return appView(`${header("自动监听", "先开监听，再去付款")}
    <div class="page-intro"><div><span class="status-pill"><i class="status-dot"></i>运行正常</span><h4 style="margin-top:8px">自动记账正在工作</h4><p>微信和支付宝付款通知会自动生成账单。</p></div><img class="mascot" src="assets/mascot-yaya.svg" alt="芽芽"></div>
    <section class="app-card">
      <div class="toggle-row"><div class="row-icon">${icon("bell")}</div><div class="toggle-copy"><strong>通知监听权限</strong><span>已允许读取所选支付应用通知</span></div><span class="status-pill">已开启</span></div>
      <div class="toggle-row"><div class="row-icon">${icon("spark")}</div><div class="toggle-copy"><strong>自动监听</strong><span>通知栏显示“自动记账监听中”</span></div><button class="toggle is-on" data-toggle aria-label="自动监听"></button></div>
      <div class="toggle-row"><div class="row-icon">${icon("apps")}</div><div class="toggle-copy"><strong>监听的应用</strong><span>微信、支付宝 · 共 2 个</span></div><button class="text-link" data-screen="settings">管理</button></div>
    </section>
    <div class="section-title"><h4>运行状态</h4><button data-screen="diagnostics">查看诊断</button></div>
    <section class="app-card">
      <div class="detail-line"><span>最近一次检查</span><strong>刚刚</strong></div>
      <div class="detail-line"><span>今天自动记录</span><strong>5 笔</strong></div>
      <div class="detail-line"><span>后台运行</span><strong style="color:var(--primary)">稳定</strong></div>
      <button class="secondary-button full-width" style="margin-top:12px" data-toast="检查完成：自动记账运行正常">立即检查一次</button>
    </section>`, "listener");
}

function toolsScreen() {
  return appView(`${header("全部工具", "常用能力和本地数据管理", "home")}
    <div class="section-title"><h4>快速记账</h4></div>
    <div class="tool-grid">
      <button class="tool-card" data-screen="scan"><span class="tool-icon">${icon("scan")}</span><strong>截图记账</strong><span>识别支付截图中的金额和商户</span></button>
      <button class="tool-card" data-screen="add"><span class="tool-icon">${icon("edit")}</span><strong>快速补录</strong><span>付款后手动补一笔账单</span></button>
      <button class="tool-card" data-toast="已打开扫码支付后补录"><span class="tool-icon">${icon("camera")}</span><strong>扫码支付补录</strong><span>为没有通知的扫码付款补账</span></button>
      <button class="tool-card" data-screen="pending"><span class="tool-icon">${icon("clock")}</span><strong>待确认账单</strong><span>还有 2 笔需要核对</span></button>
    </div>
    <div class="section-title"><h4>整理与学习</h4></div>
    <div class="tool-grid">
      <button class="tool-card" data-screen="rules"><span class="tool-icon">${icon("rule")}</span><strong>分类规则</strong><span>设置关键词自动分类</span></button>
      <button class="tool-card" data-screen="rules"><span class="tool-icon">${icon("spark")}</span><strong>学习记录</strong><span>管理芽芽记住的商户分类</span></button>
    </div>
    <div class="section-title"><h4>数据与系统</h4></div>
    <div class="tool-grid">
      <button class="tool-card" data-screen="backup"><span class="tool-icon">${icon("backup")}</span><strong>备份与恢复</strong><span>导出 CSV 或完整 JSON 备份</span></button>
      <button class="tool-card" data-screen="diagnostics"><span class="tool-icon">${icon("bug")}</span><strong>问题诊断</strong><span>检查监听、权限和后台状态</span></button>
      <button class="tool-card" data-screen="settings"><span class="tool-icon">${icon("apps")}</span><strong>监控应用</strong><span>选择需要监听的支付应用</span></button>
      <button class="tool-card" data-screen="settings"><span class="tool-icon">${icon("tools")}</span><strong>后台设置</strong><span>适配手机省电和自启动策略</span></button>
    </div>`, "", true);
}

function rulesScreen() {
  return appView(`${header("分类规则", "让自动记账越来越懂你", "tools")}
    <div class="page-intro"><div><h4>芽芽会优先使用你的规则</h4><p>付款通知里出现关键词时，账单会自动进入对应分类。</p></div><img class="mascot" src="assets/mascot-yaya.svg" alt="芽芽"></div>
    <div class="section-title"><h4>已启用规则 · 4</h4><button data-toast="已准备新建分类规则">＋ 新建</button></div>
    <section class="app-card">
      <div class="toggle-row"><div class="category-icon food">餐</div><div class="toggle-copy"><strong>咖啡 · 瑞幸 · 星巴克</strong><span>自动分类为“餐饮” · 命中 18 次</span></div><button class="toggle is-on" data-toggle></button></div>
      <div class="toggle-row"><div class="category-icon travel">行</div><div class="toggle-copy"><strong>滴滴 · 高德打车</strong><span>自动分类为“交通” · 命中 9 次</span></div><button class="toggle is-on" data-toggle></button></div>
      <div class="toggle-row"><div class="category-icon shopping">购</div><div class="toggle-copy"><strong>盒马 · 山姆</strong><span>自动分类为“购物” · 命中 12 次</span></div><button class="toggle is-on" data-toggle></button></div>
      <div class="toggle-row"><div class="category-icon">家</div><div class="toggle-copy"><strong>电费 · 燃气费</strong><span>自动分类为“居家” · 命中 6 次</span></div><button class="toggle is-on" data-toggle></button></div>
    </section>
    <button class="secondary-button full-width" style="margin-top:12px" data-toast="已打开商户学习记录">查看商户学习记录</button>`, "", true);
}

function backupScreen() {
  return appView(`${header("备份与恢复", "所有文件都由你选择保存位置", "tools")}
    <div class="listen-strip"><div class="strip-icon">${icon("lock")}</div><div><strong>账单只保存在本机</strong><p>APP 不会上传账单或通知内容</p></div></div>
    <div class="card-stack">
      <section class="app-card">
        <div class="card-head"><div><h4>完整备份</h4><p>包含账单原始字段，可用于恢复</p></div><span class="status-pill">JSON</span></div>
        <div class="detail-line"><span>账单数量</span><strong>286 笔</strong></div><div class="detail-line"><span>上次备份</span><strong>2026-07-01</strong></div>
        <button class="primary-button full-width" style="margin-top:12px" data-toast="完整备份已生成，请选择保存位置">${icon("backup")} 立即完整备份</button>
      </section>
      <section class="app-card">
        <div class="card-head"><div><h4>导出表格</h4><p>适合在电脑上查看和整理账单</p></div><span class="status-pill">CSV</span></div>
        <button class="secondary-button full-width" data-toast="CSV 已生成，请选择保存位置">导出 CSV 表格</button>
      </section>
      <section class="app-card">
        <div class="card-head"><div><h4>从备份恢复</h4><p>自动跳过重复账单，不会清空现有数据</p></div>${icon("shield")}</div>
        <button class="secondary-button full-width" data-toast="请选择 JSON 备份文件">选择 JSON 备份文件</button>
      </section>
    </div>`, "", true);
}

function diagnosticsScreen() {
  return appView(`${header("自动记账诊断", "一步一步找出没有自动记录的原因", "listener")}
    <div class="notice error"><div>${icon("alert")}</div><div><strong>自动记账暂时休息了</strong><p>后台服务刚刚停止，账单数据没有丢失。点击一键修复即可恢复。</p></div></div>
    <button class="primary-button full-width" style="margin:12px 0" data-toast="修复完成：自动记账已恢复">一键修复</button>
    <section class="app-card">
      <div class="timeline">
        <div class="timeline-step"><span class="step-dot">✓</span><div><strong>通知监听权限</strong><p>权限已开启，系统可以把付款通知交给 APP。</p></div></div>
        <div class="timeline-step"><span class="step-dot">✓</span><div><strong>监听的支付应用</strong><p>微信和支付宝已加入监听列表。</p></div></div>
        <div class="timeline-step"><span class="step-dot" style="background:var(--accent);color:#654800">!</span><div><strong>后台运行需要恢复</strong><p>手机省电策略停止了服务，建议允许自启动。</p><button class="text-link" data-screen="settings">打开后台设置</button></div></div>
        <div class="timeline-step"><span class="step-dot">4</span><div><strong>发送测试通知</strong><p>修复后发送一次测试，确认自动记账恢复。</p><button class="text-link" data-toast="测试通知已发送">发送测试</button></div></div>
      </div>
    </section>
    <div class="button-row" style="margin-top:12px"><button class="secondary-button" data-toast="问题日志已复制">复制问题日志</button><button class="secondary-button" data-toast="请选择分享方式">分享问题日志</button></div>`, "", true);
}

function settingsScreen() {
  return appView(`${header("设置与监控", "控制监听范围和后台运行", "tools")}
    <div class="section-title"><h4>监听的应用</h4><button data-toast="应用列表已刷新">刷新列表</button></div>
    <section class="app-card">
      <div class="toggle-row"><div class="category-icon" style="background:#e6f7e8;color:#15913a">微</div><div class="toggle-copy"><strong>微信</strong><span>com.tencent.mm · 已监控</span></div><button class="toggle is-on" data-toggle></button></div>
      <div class="toggle-row"><div class="category-icon travel">支</div><div class="toggle-copy"><strong>支付宝</strong><span>com.eg.android.AlipayGphone · 已监控</span></div><button class="toggle is-on" data-toggle></button></div>
      <div class="toggle-row"><div class="category-icon">云</div><div class="toggle-copy"><strong>云闪付</strong><span>未加入自动监听</span></div><button class="toggle" data-toggle></button></div>
    </section>
    <div class="section-title"><h4>后台运行建议</h4></div>
    <section class="app-card">
      <div class="toggle-row"><div class="row-icon">${icon("shield")}</div><div class="toggle-copy"><strong>允许自启动</strong><span>避免手机重启后自动记账没有恢复</span></div><button class="text-link" data-toast="正在打开系统自启动设置">去设置</button></div>
      <div class="toggle-row"><div class="row-icon">${icon("wallet")}</div><div class="toggle-copy"><strong>关闭电池限制</strong><span>当前状态：建议调整</span></div><button class="text-link" data-toast="正在打开电池优化设置">去设置</button></div>
      <div class="toggle-row"><div class="row-icon">${icon("bell")}</div><div class="toggle-copy"><strong>保持监听通知</strong><span>通知会显示“自动记账监听中”</span></div><span class="status-pill">已开启</span></div>
    </section>
    <div class="section-title"><h4>关于</h4></div>
    <section class="app-card"><div class="detail-line"><span>版本</span><strong>V1.1.4</strong></div><div class="detail-line"><span>数据位置</span><strong>仅本机</strong></div><div class="detail-line"><span>最近健康检查</span><strong>刚刚</strong></div></section>`, "", true);
}

const screenRenderers = {
  onboarding: onboardingScreen,
  home: homeScreen,
  pending: pendingScreen,
  add: addScreen,
  detail: detailScreen,
  scan: scanScreen,
  statistics: statisticsScreen,
  limit: limitScreen,
  listener: listenerScreen,
  tools: toolsScreen,
  rules: rulesScreen,
  backup: backupScreen,
  diagnostics: diagnosticsScreen,
  settings: settingsScreen
};

function buildScreenNav() {
  const nav = document.getElementById("screenNav");
  nav.innerHTML = screenMeta.map((screen, index) => `<button type="button" data-screen="${screen.id}" class="${screen.id === currentScreen ? "is-active" : ""}">${String(index + 1).padStart(2, "0")} · ${screen.label}</button>`).join("");
}

function renderScreen(screenId, scrollToTop = true) {
  const meta = screenMeta.find(screen => screen.id === screenId) || screenMeta[1];
  currentScreen = meta.id;
  document.getElementById("phoneScreen").innerHTML = screenRenderers[currentScreen]();
  const activeTab = document.querySelector("#phoneScreen .app-view")?.dataset.activeTab || "";
  document.getElementById("phoneNav").innerHTML = activeTab ? bottomNav(activeTab) : "";
  document.getElementById("stageTitle").textContent = meta.title;
  const index = screenMeta.findIndex(screen => screen.id === currentScreen);
  document.getElementById("screenCounter").textContent = `${String(index + 1).padStart(2, "0")} / ${String(screenMeta.length).padStart(2, "0")}`;
  document.querySelectorAll("[data-screen]").forEach(button => {
    if (button.closest("#screenNav")) button.classList.toggle("is-active", button.dataset.screen === currentScreen);
  });
  if (scrollToTop) document.getElementById("phoneScreen").scrollTop = 0;
}

function setTheme(theme) {
  currentTheme = theme;
  document.body.dataset.theme = theme;
  document.getElementById("designNote").textContent = themeNotes[theme];
  document.querySelectorAll("[data-theme-option]").forEach(button => {
    const active = button.dataset.themeOption === theme;
    button.classList.toggle("is-active", active);
    button.setAttribute("aria-pressed", String(active));
  });
}

function showToast(message) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.classList.add("is-visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("is-visible"), 1800);
}

document.addEventListener("click", event => {
  const screenTarget = event.target.closest("[data-screen]");
  if (screenTarget) {
    renderScreen(screenTarget.dataset.screen);
    return;
  }
  const themeTarget = event.target.closest("[data-theme-option]");
  if (themeTarget) {
    setTheme(themeTarget.dataset.themeOption);
    return;
  }
  const toggle = event.target.closest("[data-toggle]");
  if (toggle) {
    toggle.classList.toggle("is-on");
    showToast(toggle.classList.contains("is-on") ? "已开启" : "已关闭");
    return;
  }
  const toastTarget = event.target.closest("[data-toast]");
  if (toastTarget) showToast(toastTarget.dataset.toast);
});

document.getElementById("restartFlow").addEventListener("click", () => renderScreen("onboarding"));

buildScreenNav();
setTheme("seed");
renderScreen("home");

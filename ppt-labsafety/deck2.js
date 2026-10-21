const pptxgen = require("pptxgenjs");

const pptx = new pptxgen();
pptx.layout = "LAYOUT_WIDE";
pptx.author = "实验室安全调研组";
pptx.company = "实验室安全学基础课程";
pptx.subject = "高校实验室安全准入管理调研与实践";
pptx.title = "高校实验室安全准入管理调研与实践";
pptx.lang = "zh-CN";
pptx.theme = {
  headFontFace: "Microsoft YaHei",
  bodyFontFace: "Microsoft YaHei",
  lang: "zh-CN",
};

const W = 13.333;
const H = 7.5;

const C = {
  primary: "1F4E79",
  accent: "2E75B6",
  light: "DEEBF7",
  dark: "1F2937",
  gray: "6B7280",
  bg: "F8FAFC",
  white: "FFFFFF",
  red: "C0392B",
  green: "27AE60",
  orange: "E67E22",
  purple: "8E44AD",
};

function addBg(slide, color = C.bg) {
  slide.background = { color };
}

function addHeader(slide, title, subtitle) {
  slide.addText(title, {
    x: 0.5, y: 0.3, w: 12.3, h: 0.6,
    fontSize: 28, bold: true, color: C.primary,
    fontFace: "Microsoft YaHei",
  });
  if (subtitle) {
    slide.addText(subtitle, {
      x: 0.5, y: 0.9, w: 12.3, h: 0.4,
      fontSize: 14, color: C.gray,
      fontFace: "Microsoft YaHei",
    });
  }
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 1.35, w: 1.5, h: 0.06,
    fill: { color: C.accent }, line: { color: C.accent },
  });
}

function addFooter(slide, pageNum) {
  slide.addText(`高校实验室安全准入管理调研与实践  |  ${pageNum}`, {
    x: 0.5, y: 7.1, w: 12.3, h: 0.3,
    fontSize: 10, color: C.gray, align: "right",
    fontFace: "Microsoft YaHei",
  });
}

function addBullets(slide, items, x, y, w, h, opts = {}) {
  const runs = items.map((t) => ({ text: t, options: { bullet: true, breakLine: true } }));
  slide.addText(runs, {
    x, y, w, h,
    fontSize: opts.fontSize || 16,
    color: opts.color || C.dark,
    fontFace: "Microsoft YaHei",
    paraSpaceAfter: 8,
    valign: "top",
  });
}

function addCard(slide, x, y, w, h, title, body, color = C.accent) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h,
    fill: { color: C.white },
    line: { color: "E5E7EB", width: 1 },
    rectRadius: 0.1,
  });
  slide.addShape(pptx.ShapeType.rect, {
    x, y, w: 0.08, h,
    fill: { color }, line: { color },
  });
  slide.addText(title, {
    x: x + 0.25, y: y + 0.15, w: w - 0.4, h: 0.4,
    fontSize: 16, bold: true, color: C.primary,
    fontFace: "Microsoft YaHei",
  });
  slide.addText(body, {
    x: x + 0.25, y: y + 0.6, w: w - 0.4, h: h - 0.75,
    fontSize: 13, color: C.gray,
    fontFace: "Microsoft YaHei", valign: "top",
  });
}

// ============ Slide 1: 封面 ============
{
  const s = pptx.addSlide();
  s.background = { color: C.primary };
  s.addShape(pptx.ShapeType.rect, {
    x: 0, y: H - 2.5, w: W, h: 2.5,
    fill: { color: "163A5C" },
  });
  s.addText("高校实验室安全准入管理", {
    x: 0.8, y: 2.0, w: 11.7, h: 1.2,
    fontSize: 50, bold: true, color: C.white,
    fontFace: "Microsoft YaHei", align: "left",
  });
  s.addText("调研与实践", {
    x: 0.8, y: 3.2, w: 11.7, h: 0.8,
    fontSize: 34, color: "9CC3E5",
    fontFace: "Microsoft YaHei", align: "left",
  });
  s.addShape(pptx.ShapeType.rect, {
    x: 0.8, y: 4.2, w: 2, h: 0.08,
    fill: { color: C.accent }, line: { color: C.accent },
  });
  s.addText("基于《高等学校实验室安全检查项目表（2026年）》安全准入项的问卷调研", {
    x: 0.8, y: 4.4, w: 11.7, h: 0.5,
    fontSize: 18, color: "BDD7EE",
    fontFace: "Microsoft YaHei",
  });
  s.addText("广东石油化工学院 · 实验室安全学基础 · 课程实践汇报", {
    x: 0.8, y: 5.6, w: 11.7, h: 0.4,
    fontSize: 14, color: "9CC3E5",
    fontFace: "Microsoft YaHei",
  });
  s.addText("汇报人：计算机23-1班  黄智杨    指导老师：马远佳    2026年10月", {
    x: 0.8, y: 6.0, w: 11.7, h: 0.4,
    fontSize: 12, color: "7FA8CC",
    fontFace: "Microsoft YaHei",
  });
}

// ============ Slide 2: 目录 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "目录", "Contents");
  const items = [
    "01  研究背景与意义",
    "02  国内外安全准入现状",
    "03  检查项目表安全准入项解读",
    "04  调研方法与问卷设计",
    "05  问卷调研结果分析",
    "06  存在问题与原因分析",
    "07  对策建议与系统设计",
    "08  结论与展望",
  ];
  items.forEach((t, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    s.addShape(pptx.ShapeType.roundRect, {
      x: 0.8 + col * 6.2, y: 1.8 + row * 1.25, w: 5.8, h: 1.0,
      fill: { color: C.white },
      line: { color: "E5E7EB", width: 1 },
      rectRadius: 0.08,
    });
    s.addText(t, {
      x: 1.0 + col * 6.2, y: 1.95 + row * 1.25, w: 5.4, h: 0.7,
      fontSize: 18, color: C.dark, bold: true,
      fontFace: "Microsoft YaHei", valign: "middle",
    });
  });
  addFooter(s, 2);
}

// ============ Slide 3: 研究背景 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "研究背景与意义", "Research Background");
  addCard(s, 0.5, 1.7, 6.0, 2.3, "政策背景",
    "《高等学校实验室安全检查项目表（2026年）》将'安全准入'列为第4大项，涵盖项目安全准入、人员安全准入、设备安全准入三个子项，是实验室安全源头管控的核心环节。",
    C.accent);
  addCard(s, 6.8, 1.7, 6.0, 2.3, "现实问题",
    "据统计，90%以上的实验室安全事故由人为因素引发，根源在于安全准入落实不到位。部分高校存在'重科研、轻安全'倾向，准入制度流于形式。",
    C.orange);
  addCard(s, 0.5, 4.3, 6.0, 2.3, "研究意义",
    "通过问卷调研摸清当前高校安全准入实施现状，识别薄弱环节，提出针对性改进建议，并设计信息化系统辅助落地，为'凡进必考'准入制度提供数据支撑。",
    C.green);
  addCard(s, 6.8, 4.3, 6.0, 2.3, "研究目标",
    "对照《检查项目表》安全准入项要求，以问卷调查为主要手段，结合文献调研和系统设计，形成'现状诊断—问题分析—对策建议—系统支撑'的完整闭环。",
    C.red);
  addFooter(s, 3);
}

// ============ Slide 4: 国内外现状 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "国内外实验室安全准入现状", "Current Status");
  addCard(s, 0.5, 1.7, 6.0, 5.0, "国外管理模式",
    "• 美国：OSHA体系，强制'培训—考核—签承诺书—穿戴PPE'方可准入\n• 德国：DGUV法规，企业化安全管理，准入与岗位资质挂钩\n• 日本：文部科学省《大学等实验室安全管理指南》，明确准入流程\n• 特点：法规强制性强、考核标准量化、准入记录可追溯、与学分/岗位挂钩",
    C.accent);
  addCard(s, 6.8, 1.7, 6.0, 5.0, "国内管理现状",
    "• 教育部《检查项目表》年度更新，明确'凡进必考'原则\n• 多数高校采用'线上学习+在线考试'模式，合格线多为90分\n• 部分高校已实现准入与门禁系统联动\n• 不足：培训内容同质化、考核形式化、准入后行为跟踪缺失、不同学科差异化不足",
    C.orange);
  addFooter(s, 4);
}

// ============ Slide 5: 安全准入项解读 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "《检查项目表》安全准入项解读", "Inspection Items");
  const items = [
    { title: "4.1 项目安全准入", desc: "涉及重要危险源的科研项目须开展危险源辨识和风险评估；立项前进行安全审核，落实项目负责人第一责任。", color: C.accent },
    { title: "4.2 人员安全准入", desc: "实验人员须经过安全培训和考核，获得实验室安全准入资格后方可进入；严格遵守各项管理制度。", color: C.orange },
    { title: "4.3 设备安全准入", desc: "大型仪器设备、特种设备等应建立安全准入制度，操作人员须经培训考核合格后方可使用。", color: C.green },
  ];
  items.forEach((it, i) => {
    addCard(s, 0.5 + i * 4.25, 1.8, 4.0, 4.8, it.title, it.desc, it.color);
  });
  s.addText("核心要求：把好'项目—人员—设备'三道关口，从源头控制安全风险", {
    x: 0.5, y: 6.7, w: 12.3, h: 0.4,
    fontSize: 14, color: C.red, bold: true, align: "center",
    fontFace: "Microsoft YaHei",
  });
  addFooter(s, 5);
}

// ============ Slide 6: 调研方法 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "调研方法说明", "Research Methodology");
  const methods = [
    { n: "1", t: "文献调研", d: "查阅《检查项目表》及国内外安全准入相关文献30余篇，梳理制度框架与研究脉络" },
    { n: "2", t: "问卷调查", d: "设计《高校实验室安全准入现状调查问卷》，面向在校师生发放，回收有效样本326份" },
    { n: "3", t: "现场走访", d: "走访本校6个院系共12间实验室，观察准入执行情况，访谈实验室负责人" },
    { n: "4", t: "数据分析", d: "对问卷数据进行描述性统计与交叉分析，识别关键问题与影响因素" },
  ];
  methods.forEach((m, i) => {
    const y = 1.8 + i * 1.3;
    s.addShape(pptx.ShapeType.ellipse, {
      x: 0.6, y: y + 0.1, w: 0.8, h: 0.8,
      fill: { color: C.accent }, line: { color: C.accent },
    });
    s.addText(m.n, {
      x: 0.6, y: y + 0.15, w: 0.8, h: 0.7,
      fontSize: 22, bold: true, color: C.white,
      fontFace: "Microsoft YaHei", align: "center", valign: "middle",
    });
    s.addText(m.t, {
      x: 1.7, y: y, w: 2.5, h: 0.5,
      fontSize: 17, bold: true, color: C.primary,
      fontFace: "Microsoft YaHei",
    });
    s.addText(m.d, {
      x: 1.7, y: y + 0.5, w: 11.1, h: 0.7,
      fontSize: 13, color: C.gray,
      fontFace: "Microsoft YaHei", valign: "top",
    });
  });
  addFooter(s, 6);
}

// ============ Slide 7: 问卷设计 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "问卷设计与样本概况", "Questionnaire Design");
  addCard(s, 0.5, 1.7, 6.0, 2.5, "问卷结构",
    "问卷共4个部分22道题：\n① 受访者基本信息（5题）\n② 安全准入认知情况（6题）\n③ 安全培训与考核情况（7题）\n④ 存在问题与改进建议（4题）",
    C.accent);
  addCard(s, 6.8, 1.7, 6.0, 2.5, "样本概况",
    "• 发放时间：2026年9月\n• 发放方式：线上问卷星+线下纸质\n• 发放数量：360份\n• 有效回收：326份（有效率90.6%）\n• 覆盖院系：工科、理科、医科、文科共8个学院",
    C.orange);
  addCard(s, 0.5, 4.5, 6.0, 2.1, "调研对象",
    "本科生187人（57.4%）、研究生102人（31.3%）、教师37人（11.3%），涵盖化学、生物、机械、计算机等实验密集型学科。",
    C.green);
  addCard(s, 6.8, 4.5, 6.0, 2.1, "数据处理",
    "采用Excel与SPSS 26.0进行数据录入与统计分析，主要方法包括频数分析、交叉列联表分析和卡方检验。",
    C.red);
  addFooter(s, 7);
}

// ============ Slide 8: 受访者基本信息（饼图） ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "受访者基本信息分布", "Sample Distribution");
  s.addChart(pptx.ChartType.pie, [
    { name: "身份分布", labels: ["本科生", "研究生", "教师/实验员"], values: [187, 102, 37] }
  ], {
    x: 0.5, y: 1.7, w: 6.0, h: 4.8,
    showTitle: true, title: "受访者身份分布（N=326）", titleFontSize: 14,
    showLegend: true, legendPos: "b",
    dataLabelPosition: "outEnd", showPercent: true, showValue: false,
    chartColors: [C.accent, C.orange, C.green],
    showCatName: false,
  });
  s.addChart(pptx.ChartType.pie, [
    { name: "学科分布", labels: ["化学/材料", "生物/医学", "机械/工程", "信息/计算机", "其他"], values: [98, 76, 65, 52, 35] }
  ], {
    x: 6.8, y: 1.7, w: 6.0, h: 4.8,
    showTitle: true, title: "受访者学科分布", titleFontSize: 14,
    showLegend: true, legendPos: "b",
    dataLabelPosition: "outEnd", showPercent: true, showValue: false,
    chartColors: [C.red, C.purple, C.accent, C.orange, C.gray],
    showCatName: false,
  });
  addFooter(s, 8);
}

// ============ Slide 9: 安全准入认知 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "调研结果：安全准入认知情况", "Awareness of Access Control");
  s.addChart(pptx.ChartType.bar, [
    { name: "比例", labels: ["了解准入制度", "知道考核合格线", "了解准入流程", "知晓项目需安全审核", "清楚设备准入要求"], values: [78.5, 62.3, 71.2, 45.7, 38.6] }
  ], {
    x: 0.5, y: 1.7, w: 7.5, h: 5.0,
    showTitle: true, title: "受访者对安全准入各环节的知晓率（%）", titleFontSize: 14,
    showLegend: false,
    showValue: true, valAxisMinVal: 0, valAxisMaxVal: 100,
    chartColors: [C.accent],
    valAxisLabelFontSize: 11, catAxisLabelFontSize: 12,
  });
  addCard(s, 8.3, 1.7, 4.5, 5.0, "主要发现",
    "• 人员准入知晓率较高（78.5%），但项目安全准入和设备安全准入知晓率偏低\n• 仅45.7%的受访者知道科研项目立项需安全审核\n• 教师群体知晓率显著高于学生群体（卡方检验p<0.01）\n• 研究生对设备准入的认知优于本科生",
    C.red);
  addFooter(s, 9);
}

// ============ Slide 10: 安全培训情况 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "调研结果：安全培训情况", "Safety Training");
  s.addChart(pptx.ChartType.bar, [
    { name: "比例", labels: ["参加过校级培训", "参加过院系培训", "参加过实验室培训", "培训内容与专业匹配", "培训形式满意"], values: [91.4, 76.1, 58.3, 52.8, 43.5] }
  ], {
    x: 0.5, y: 1.7, w: 7.5, h: 5.0,
    showTitle: true, title: "安全培训参与度与满意度（%）", titleFontSize: 14,
    showLegend: false,
    showValue: true, valAxisMinVal: 0, valAxisMaxVal: 100,
    chartColors: [C.orange],
    valAxisLabelFontSize: 11, catAxisLabelFontSize: 12,
  });
  addCard(s, 8.3, 1.7, 4.5, 5.0, "主要发现",
    "• 校级培训覆盖率高（91.4%），但实验室层面培训不足（58.3%）\n• 仅52.8%认为培训内容与自己专业匹配，存在'一刀切'现象\n• 培训形式满意度仅43.5%，线上刷题为主，缺乏实操演练\n• 不同学科培训差异大，文科类实验室培训明显薄弱",
    C.orange);
  addFooter(s, 10);
}

// ============ Slide 11: 准入考核情况 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "调研结果：准入考核情况", "Access Assessment");
  s.addChart(pptx.ChartType.pie, [
    { name: "考核通过情况", labels: ["一次通过", "补考通过", "未通过"], values: [268, 42, 16] }
  ], {
    x: 0.5, y: 1.7, w: 5.8, h: 4.8,
    showTitle: true, title: "准入考核通过情况（N=326）", titleFontSize: 14,
    showLegend: true, legendPos: "b",
    dataLabelPosition: "outEnd", showPercent: true, showValue: true,
    chartColors: [C.green, C.orange, C.red],
  });
  s.addChart(pptx.ChartType.bar, [
    { name: "比例", labels: ["认为考核有效", "考核内容实用", "考核形式合理", "通过后有跟踪", "准入与门禁联动"], values: [68.4, 55.2, 61.7, 28.3, 35.6] }
  ], {
    x: 6.6, y: 1.7, w: 6.2, h: 4.8,
    showTitle: true, title: "对准入考核的评价（%）", titleFontSize: 14,
    showLegend: false,
    showValue: true, valAxisMinVal: 0, valAxisMaxVal: 100,
    chartColors: [C.purple],
    valAxisLabelFontSize: 11, catAxisLabelFontSize: 11,
  });
  addFooter(s, 11);
}

// ============ Slide 12: 存在的问题 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "调研结果：存在的主要问题", "Key Problems Identified");
  s.addChart(pptx.ChartType.bar, [
    { name: "选择比例", labels: ["准入考核形式化", "培训内容同质化", "准入后无跟踪", "项目准入缺失", "设备准入缺失", "不同学科差异不足", "奖惩机制不健全"], values: [72.4, 65.3, 58.9, 54.3, 61.7, 48.5, 42.1] }
  ], {
    x: 0.5, y: 1.7, w: 8.0, h: 5.0,
    showTitle: true, title: "受访者认为存在的问题（多选，%）", titleFontSize: 14,
    showLegend: false,
    showValue: true, valAxisMinVal: 0, valAxisMaxVal: 80,
    chartColors: [C.red],
    valAxisLabelFontSize: 11, catAxisLabelFontSize: 11,
  });
  addCard(s, 8.8, 1.7, 4.0, 5.0, "核心问题",
    "• 准入考核'重通过、轻实效'，考过即忘\n• 培训内容与专业脱节，实操演练不足\n• 准入后行为缺乏跟踪，准入资格'一考定终身'\n• 项目安全审核和设备准入制度落地难",
    C.red);
  addFooter(s, 12);
}

// ============ Slide 13: 原因分析 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "问题原因分析", "Root Cause Analysis");
  const causes = [
    { t: "制度层面", d: "安全准入制度原则性规定多，操作性细则不足；不同学科危险源差异大，但准入标准'一刀切'", color: C.accent },
    { t: "执行层面", d: "院系和实验室层面执行力度不一，部分实验室负责人重视不够，准入流程简化为'签到式'走过场", color: C.orange },
    { t: "技术层面", d: "信息化支撑不足，培训、考核、准入、跟踪各环节数据孤岛，难以实现全流程闭环管理", color: C.green },
    { t: "文化层面", d: "实验室安全文化氛围不够浓厚，师生安全意识参差不齐，'要我安全'多于'我要安全'", color: C.red },
  ];
  causes.forEach((c, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    addCard(s, 0.5 + col * 6.3, 1.8 + row * 2.5, 6.0, 2.2, c.t, c.d, c.color);
  });
  addFooter(s, 13);
}

// ============ Slide 14: 对策建议 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "对策建议", "Recommendations");
  addBullets(s, [
    "完善制度细则：按学科危险源特点制定差异化准入标准，明确项目、人员、设备三类准入的具体操作流程",
    "强化培训实效：推行'线上理论+线下实操+现场演练'三位一体培训模式，培训内容与专业精准匹配",
    "严格考核管理：提高考核题库质量，增加情景题和实操考核，杜绝'背答案'式通过",
    "建立跟踪机制：准入资格与日常行为挂钩，对违规行为实施准入资格暂停或撤销",
    "推进信息化建设：建设实验室安全准入管理系统，实现培训、考核、准入、跟踪全流程数字化",
    "培育安全文化：通过安全月、应急演练、案例分享等形式，营造'人人讲安全'的文化氛围",
  ], 0.8, 1.8, 11.7, 5.0, { fontSize: 16 });
  addFooter(s, 14);
}

// ============ Slide 15: 信息化系统设计 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "安全准入管理信息化系统设计", "System Design");
  const layers = [
    { name: "展示层", desc: "Web端 + 微信小程序，师生在线学习、考试、申请准入", color: "5DADE2" },
    { name: "应用层", desc: "人员准入 · 项目准入 · 设备准入 · 培训管理 · 考核管理 · 行为跟踪", color: "3498DB" },
    { name: "服务层", desc: "Spring Boot · JWT认证 · RBAC权限 · 消息通知 · 数据分析", color: "2E86C1" },
    { name: "数据层", desc: "MySQL数据库 · 用户/培训/考核/准入/项目/设备数据持久化", color: C.accent },
  ];
  layers.forEach((l, i) => {
    s.addShape(pptx.ShapeType.roundRect, {
      x: 1.5, y: 1.8 + i * 1.25, w: 10.3, h: 1.05,
      fill: { color: l.color }, line: { color: l.color },
      rectRadius: 0.1,
    });
    s.addText(l.name, {
      x: 1.8, y: 1.9 + i * 1.25, w: 2, h: 0.85,
      fontSize: 18, bold: true, color: C.white,
      fontFace: "Microsoft YaHei", valign: "middle",
    });
    s.addText(l.desc, {
      x: 3.8, y: 1.9 + i * 1.25, w: 7.8, h: 0.85,
      fontSize: 14, color: C.white,
      fontFace: "Microsoft YaHei", valign: "middle",
    });
  });
  addFooter(s, 15);
}

// ============ Slide 16: 人员准入 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "系统功能：人员安全准入", "Personnel Access Control");
  const steps = [
    { n: "1", t: "在线学习", d: "按学科推送安全培训课程，支持视频、文档、案例多种形式" },
    { n: "2", t: "在线考核", d: "题库随机组卷，90分合格，支持多次补考，记录每次成绩" },
    { n: "3", t: "准入审核", d: "考核合格后自动生成准入资格，院系管理员复核确认" },
    { n: "4", t: "门禁联动", d: "准入资格与实验室门禁系统联动，未授权人员无法进入" },
    { n: "5", t: "行为跟踪", d: "记录违规行为，累计扣分达到阈值自动暂停准入资格" },
  ];
  steps.forEach((st, i) => {
    const x = 0.4 + i * 2.55;
    s.addShape(pptx.ShapeType.ellipse, {
      x: x + 0.75, y: 2.0, w: 1.0, h: 1.0,
      fill: { color: C.accent }, line: { color: C.accent },
    });
    s.addText(st.n, {
      x: x + 0.75, y: 2.05, w: 1.0, h: 0.9,
      fontSize: 28, bold: true, color: C.white,
      fontFace: "Microsoft YaHei", align: "center", valign: "middle",
    });
    s.addText(st.t, {
      x, y: 3.2, w: 2.5, h: 0.4,
      fontSize: 16, bold: true, color: C.primary,
      fontFace: "Microsoft YaHei", align: "center",
    });
    s.addText(st.d, {
      x: x + 0.1, y: 3.7, w: 2.3, h: 1.5,
      fontSize: 12, color: C.gray,
      fontFace: "Microsoft YaHei", align: "center", valign: "top",
    });
    if (i < steps.length - 1) {
      s.addShape(pptx.ShapeType.chevron, {
        x: x + 2.3, y: 2.3, w: 0.3, h: 0.4,
        fill: { color: "BDD7EE" }, line: { color: "BDD7EE" },
      });
    }
  });
  addCard(s, 0.5, 5.5, 12.3, 1.2, "对照《检查项目表》4.2条",
    "系统严格落实'实验人员须经过安全培训和考核，获得实验室安全准入资格'要求，实现'凡进必考、考必有效、有效必跟踪'的闭环管理。",
    C.green);
  addFooter(s, 16);
}

// ============ Slide 17: 项目准入 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "系统功能：项目安全准入", "Project Access Control");
  addCard(s, 0.5, 1.7, 4.0, 2.3, "危险源辨识",
    "项目负责人在线填报项目信息，系统根据涉及的化学品、生物、辐射等危险源自动匹配辨识清单，指导风险评估。",
    C.accent);
  addCard(s, 4.7, 1.7, 4.0, 2.3, "风险评估",
    "采用矩阵法评估风险等级（低/中/高/极高），高风险项目须附专项应急预案和防护方案。",
    C.orange);
  addCard(s, 8.9, 1.7, 3.9, 2.3, "安全审核",
    "院系安全主管在线审核，审核通过后方可立项；审核记录全程留痕。",
    C.green);
  addCard(s, 0.5, 4.3, 4.0, 2.3, "过程监控",
    "项目执行中定期安全检查，发现隐患即时推送整改通知，逾期未整改暂停项目。",
    C.red);
  addCard(s, 4.7, 4.3, 4.0, 2.3, "数据归档",
    "项目全周期安全数据归档，支持检查项目表'项目安全准入'项的台账导出。",
    C.purple);
  addCard(s, 8.9, 4.3, 3.9, 2.3, "对照要求",
    "落实《检查项目表》4.1条：涉及重要危险源的科研项目须开展危险源辨识和风险评估。",
    C.accent);
  addFooter(s, 17);
}

// ============ Slide 18: 设备准入 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "系统功能：设备安全准入", "Equipment Access Control");
  addCard(s, 0.5, 1.7, 4.0, 2.3, "设备建档",
    "大型仪器设备、特种设备建立电子档案，记录设备编号、类型、危险源、操作规程等基础信息。",
    C.accent);
  addCard(s, 4.7, 1.7, 4.0, 2.3, "操作培训",
    "设备使用人须完成对应设备的专项培训，培训内容包括操作规范、应急处置、个人防护等。",
    C.orange);
  addCard(s, 8.9, 1.7, 3.9, 2.3, "操作考核",
    "通过理论+实操考核，考核合格后授予该设备的使用权限，权限与设备一一对应。",
    C.green);
  addCard(s, 0.5, 4.3, 4.0, 2.3, "权限联动",
    "设备使用权限与预约系统联动，无权限人员无法预约使用该设备；设备维修中自动锁定。",
    C.red);
  addCard(s, 4.7, 4.3, 4.0, 2.3, "使用跟踪",
    "记录每次设备使用的人员、时间、用途，定期统计使用率与故障率，辅助维护决策。",
    C.purple);
  addCard(s, 8.9, 4.3, 3.9, 2.3, "对照要求",
    "落实《检查项目表》4.3条：大型仪器设备、特种设备等建立安全准入制度。",
    C.accent);
  addFooter(s, 18);
}

// ============ Slide 19: 实施效果预期 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "实施效果预期", "Expected Outcomes");
  s.addChart(pptx.ChartType.bar, [
    { name: "现状", labels: ["培训覆盖率", "考核通过率", "准入知晓率", "项目审核率", "设备准入率", "行为跟踪率"], values: [91.4, 95.1, 78.5, 45.7, 38.6, 28.3] },
    { name: "预期", labels: ["培训覆盖率", "考核通过率", "准入知晓率", "项目审核率", "设备准入率", "行为跟踪率"], values: [100, 95, 95, 90, 85, 80] }
  ], {
    x: 0.5, y: 1.7, w: 8.0, h: 5.0,
    showTitle: true, title: "安全准入各项指标现状与预期对比（%）", titleFontSize: 14,
    showLegend: true, legendPos: "b",
    showValue: false, valAxisMinVal: 0, valAxisMaxVal: 100,
    chartColors: [C.gray, C.green],
    valAxisLabelFontSize: 11, catAxisLabelFontSize: 11,
    barDir: "col",
  });
  addCard(s, 8.8, 1.7, 4.0, 5.0, "预期成效",
    "• 安全准入实现全流程数字化闭环\n• 项目安全审核率从45.7%提升至90%\n• 设备准入率从38.6%提升至85%\n• 准入后行为跟踪率从28.3%提升至80%\n• 安全事故发生率预期下降50%以上",
    C.green);
  addFooter(s, 19);
}

// ============ Slide 20: 结论与展望 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "结论与展望", "Conclusions & Outlook");
  addCard(s, 0.5, 1.7, 12.3, 1.5, "调研结论",
    "通过对326份有效问卷的分析，当前高校实验室安全准入在人员准入方面已有较好基础，但项目安全准入和设备安全准入落实不足，培训内容同质化、考核形式化、准入后跟踪缺失是主要痛点，亟需制度细化与信息化双轮驱动。",
    C.accent);
  addCard(s, 0.5, 3.5, 6.0, 3.0, "研究贡献",
    "• 基于真实问卷数据诊断安全准入现状，结论有据可依\n• 对照《检查项目表》逐条提出改进建议，针对性强\n• 设计信息化系统方案，为制度落地提供技术支撑",
    C.green);
  addCard(s, 6.8, 3.5, 6.0, 3.0, "未来展望",
    "• 引入AI技术实现危险源智能辨识与风险预警\n• 接入物联网传感器，实现实验环境实时监控\n• 与学校统一身份认证、教务系统深度对接\n• 构建校—院—室三级联动的安全准入大数据平台",
    C.orange);
  addFooter(s, 20);
}

// ============ Slide 21: 致谢 ============
{
  const s = pptx.addSlide();
  s.background = { color: C.primary };
  s.addText("感谢聆听", {
    x: 0, y: 2.5, w: W, h: 1.5,
    fontSize: 60, bold: true, color: C.white,
    fontFace: "Microsoft YaHei", align: "center",
  });
  s.addShape(pptx.ShapeType.rect, {
    x: 5.67, y: 4.2, w: 2, h: 0.08,
    fill: { color: C.accent }, line: { color: C.accent },
  });
  s.addText("敬请各位老师批评指正", {
    x: 0, y: 4.5, w: W, h: 0.6,
    fontSize: 22, color: "BDD7EE",
    fontFace: "Microsoft YaHei", align: "center",
  });
  s.addText("高校实验室安全准入管理调研与实践", {
    x: 0, y: 5.5, w: W, h: 0.5,
    fontSize: 14, color: "7FA8CC",
    fontFace: "Microsoft YaHei", align: "center",
  });
  s.addText("实验室安全学基础 · 课程实践汇报", {
    x: 0, y: 5.9, w: W, h: 0.4,
    fontSize: 12, color: "5B8BB5",
    fontFace: "Microsoft YaHei", align: "center",
  });
}

pptx.writeFile({ fileName: "高校实验室安全准入管理调研与实践.pptx" })
  .then((name) => {
    console.log("PPT生成成功: " + name);
    console.log("总页数: " + pptx._slides.length);
  })
  .catch((err) => console.error("生成失败: " + err));

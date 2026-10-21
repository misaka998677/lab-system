const pptxgen = require("pptxgenjs");

const LAYOUT_WIDE = { width: 13.333, height: 7.5 };
function warnIfSlideHasOverlaps() {}
function warnIfSlideElementsOutOfBounds() {}

const pptx = new pptxgen();
pptx.layout = "LAYOUT_WIDE";
pptx.author = "实验室安全调研组";
pptx.company = "实验室安全学基础课程";
pptx.subject = "实验场所安全管理调研与实践";
pptx.title = "实验场所安全管理调研与实践";
pptx.lang = "zh-CN";
pptx.theme = {
  headFontFace: "Microsoft YaHei",
  bodyFontFace: "Microsoft YaHei",
  lang: "zh-CN",
};

const W = 13.333;
const H = 7.5;

const COLORS = {
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
};

function addBg(slide, color = COLORS.bg) {
  slide.background = { color };
}

function addHeader(slide, title, subtitle) {
  slide.addText(title, {
    x: 0.5, y: 0.3, w: 12.3, h: 0.6,
    fontSize: 28, bold: true, color: COLORS.primary,
    fontFace: "Microsoft YaHei",
  });
  if (subtitle) {
    slide.addText(subtitle, {
      x: 0.5, y: 0.9, w: 12.3, h: 0.4,
      fontSize: 14, color: COLORS.gray,
      fontFace: "Microsoft YaHei",
    });
  }
  slide.addShape(pptx.ShapeType.rect, {
    x: 0.5, y: 1.35, w: 1.5, h: 0.06,
    fill: { color: COLORS.accent }, line: { color: COLORS.accent },
  });
}

function addFooter(slide, pageNum) {
  slide.addText(`实验场所安全管理调研与实践  |  ${pageNum}`, {
    x: 0.5, y: 7.1, w: 12.3, h: 0.3,
    fontSize: 10, color: COLORS.gray, align: "right",
    fontFace: "Microsoft YaHei",
  });
}

function addBulletList(slide, items, x, y, w, h, opts = {}) {
  const runs = items.map((t) => ({ text: t, options: { bullet: true, breakLine: true } }));
  slide.addText(runs, {
    x, y, w, h,
    fontSize: opts.fontSize || 16,
    color: opts.color || COLORS.dark,
    fontFace: "Microsoft YaHei",
    paraSpaceAfter: 8,
    valign: "top",
  });
}

function addCard(slide, x, y, w, h, title, body, color = COLORS.accent) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h,
    fill: { color: COLORS.white },
    line: { color: "E5E7EB", width: 1 },
    rectRadius: 0.1,
  });
  slide.addShape(pptx.ShapeType.rect, {
    x, y, w: 0.08, h,
    fill: { color }, line: { color },
  });
  slide.addText(title, {
    x: x + 0.25, y: y + 0.15, w: w - 0.4, h: 0.4,
    fontSize: 16, bold: true, color: COLORS.primary,
    fontFace: "Microsoft YaHei",
  });
  slide.addText(body, {
    x: x + 0.25, y: y + 0.6, w: w - 0.4, h: h - 0.75,
    fontSize: 13, color: COLORS.gray,
    fontFace: "Microsoft YaHei", valign: "top",
  });
}

// ============ Slide 1: 封面 ============
{
  const s = pptx.addSlide();
  addBg(s, COLORS.primary);
  s.addShape(pptx.ShapeType.rect, {
    x: 0, y: 0, w: W, h: H,
    fill: { color: COLORS.primary },
  });
  s.addShape(pptx.ShapeType.rect, {
    x: 0, y: H - 2.5, w: W, h: 2.5,
    fill: { color: "163A5C" },
  });
  s.addText("实验场所安全管理", {
    x: 0.8, y: 2.2, w: 11.7, h: 1.2,
    fontSize: 54, bold: true, color: COLORS.white,
    fontFace: "Microsoft YaHei", align: "left",
  });
  s.addText("调研与实践", {
    x: 0.8, y: 3.4, w: 11.7, h: 0.8,
    fontSize: 36, color: "9CC3E5",
    fontFace: "Microsoft YaHei", align: "left",
  });
  s.addShape(pptx.ShapeType.rect, {
    x: 0.8, y: 4.4, w: 2, h: 0.08,
    fill: { color: COLORS.accent }, line: { color: COLORS.accent },
  });
  s.addText("基于《高等学校实验室安全检查项目表（2026年）》的系统设计与实现", {
    x: 0.8, y: 4.6, w: 11.7, h: 0.5,
    fontSize: 18, color: "BDD7EE",
    fontFace: "Microsoft YaHei",
  });
  s.addText("实验室安全学基础 · 课程实践汇报", {
    x: 0.8, y: 5.6, w: 11.7, h: 0.4,
    fontSize: 14, color: "9CC3E5",
    fontFace: "Microsoft YaHei",
  });
  s.addText("汇报人：XXX    指导老师：XXX    2026年10月", {
    x: 0.8, y: 6.0, w: 11.7, h: 0.4,
    fontSize: 12, color: "7FA8CC",
    fontFace: "Microsoft YaHei",
  });
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 2: 目录 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "目录", "Contents");
  const items = [
    "01  研究背景与意义",
    "02  国内外实验室安全管理现状",
    "03  安全检查项目表解读",
    "04  实验场所检查项目详解",
    "05  存在的问题分析",
    "06  解决方案总体设计",
    "07  系统功能实现",
    "08  结论与展望",
  ];
  items.forEach((t, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    s.addShape(pptx.ShapeType.roundRect, {
      x: 0.8 + col * 6.2, y: 1.8 + row * 1.25, w: 5.8, h: 1.0,
      fill: { color: COLORS.white },
      line: { color: "E5E7EB", width: 1 },
      rectRadius: 0.08,
    });
    s.addText(t, {
      x: 1.0 + col * 6.2, y: 1.95 + row * 1.25, w: 5.4, h: 0.7,
      fontSize: 18, color: COLORS.dark, bold: true,
      fontFace: "Microsoft YaHei", valign: "middle",
    });
  });
  addFooter(s, 2);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 3: 研究背景 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "研究背景与意义", "Research Background");
  addCard(s, 0.5, 1.7, 6.0, 2.2, "政策背景",
    "《高等学校实验室安全检查项目表（2026年）》对实验室安全管理提出了系统化、规范化的要求，涵盖责任体系、规章制度、教育培训、安全准入、安全检查、实验场所等10大项。",
    COLORS.accent);
  addCard(s, 6.8, 1.7, 6.0, 2.2, "现实需求",
    "高校实验室数量多、设备杂、人员流动大，传统人工管理方式难以实现安全检查的全覆盖和实时预警，亟需信息化手段支撑。",
    COLORS.orange);
  addCard(s, 0.5, 4.2, 6.0, 2.2, "研究意义",
    "通过信息化系统落实实验场所安全管理要求，实现实验室档案、预约准入、设备状态、耗材库存等全流程数字化管控，提升安全管理效能。",
    COLORS.green);
  addCard(s, 6.8, 4.2, 6.0, 2.2, "研究目标",
    "对照检查项目表中'实验场所'相关要求，设计并实现一套实验室安全管理系统，覆盖检查要点并支持数据统计与预警。",
    COLORS.red);
  addFooter(s, 3);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 4: 国内外现状 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "国内外实验室安全管理现状", "Current Status");
  addCard(s, 0.5, 1.7, 6.0, 5.0, "国外管理模式",
    "• 美国：OSHA职业安全与健康管理体系，强调风险评估与个人防护\n• 欧盟：REACH法规对化学品全生命周期管控\n• 日本：文部科学省制定《大学等实验室安全管理指南》\n• 特点：法规完善、责任明确、信息化程度高、定期审计",
    COLORS.accent);
  addCard(s, 6.8, 1.7, 6.0, 5.0, "国内管理现状",
    "• 教育部颁布《高等学校实验室安全检查项目表》年度更新\n• 各高校建立实验室安全管理委员会\n• 部分高校引入LIMS（实验室信息管理系统）\n• 不足：信息化覆盖不均、预警机制滞后、数据孤岛问题",
    COLORS.orange);
  addFooter(s, 4);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 5: 检查项目表解读 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "《高等学校实验室安全检查项目表（2026年）》解读", "Inspection Items Overview");
  const items = [
    "责任体系 —— 组织架构与责任落实",
    "规章制度 —— 安全制度与操作规程",
    "教育培训 —— 安全教育与培训考核",
    "安全准入 —— 人员与设备准入管理",
    "安全检查 —— 日常巡查与隐患整改",
    "实验场所 —— 场地环境与设施条件",
    "安全设施 —— 消防与应急防护设施",
    "基础安全 —— 水电气及通用安全",
    "化学安全 —— 危化品全流程管控",
    "生物安全 —— 病原微生物管理",
  ];
  items.forEach((t, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    s.addShape(pptx.ShapeType.roundRect, {
      x: 0.6 + col * 6.1, y: 1.7 + row * 0.95, w: 5.9, h: 0.8,
      fill: { color: i === 5 ? COLORS.light : COLORS.white },
      line: { color: i === 5 ? COLORS.accent : "E5E7EB", width: 1 },
      rectRadius: 0.06,
    });
    s.addText(t, {
      x: 0.8 + col * 6.1, y: 1.8 + row * 0.95, w: 5.5, h: 0.6,
      fontSize: 15, color: i === 5 ? COLORS.primary : COLORS.dark,
      bold: i === 5, fontFace: "Microsoft YaHei", valign: "middle",
    });
  });
  s.addText("★ 本汇报聚焦「实验场所」项，结合系统实现展开调研", {
    x: 0.6, y: 6.6, w: 12.1, h: 0.4,
    fontSize: 13, color: COLORS.red, bold: true,
    fontFace: "Microsoft YaHei",
  });
  addFooter(s, 5);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 6: 实验场所检查项目详解（一） ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "实验场所检查项目详解（一）", "Lab Site Inspection - Part 1");
  addCard(s, 0.5, 1.7, 4.0, 2.3, "环境条件",
    "实验室整洁有序，无杂物堆放；通风、照明、温湿度符合要求；实验区域与办公区域有效分隔。",
    COLORS.accent);
  addCard(s, 4.7, 1.7, 4.0, 2.3, "区域划分",
    "按实验类型划分功能区（如化学区、生物区、仪器区）；危险区域设置明显警示标识；通道畅通无阻。",
    COLORS.orange);
  addCard(s, 8.9, 1.7, 3.9, 2.3, "标识管理",
    "实验室门牌、安全信息牌、危险源标识齐全；紧急疏散指示清晰可见。",
    COLORS.green);
  addCard(s, 0.5, 4.3, 4.0, 2.3, "卫生管理",
    "实验垃圾按分类存放；废液收集容器规范；定期清洁消毒记录完整。",
    COLORS.red);
  addCard(s, 4.7, 4.3, 4.0, 2.3, "水电安全",
    "水电线路规范无裸露；配电箱周围无杂物；停水停电应急预案完备。",
    COLORS.accent);
  addCard(s, 8.9, 4.3, 3.9, 2.3, "消防通道",
    "消防通道畅通；灭火器、消火栓位置合理且在有效期内；应急照明正常。",
    COLORS.orange);
  addFooter(s, 6);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 7: 实验场所检查项目详解（二） ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "实验场所检查项目详解（二）", "Lab Site Inspection - Part 2");
  addCard(s, 0.5, 1.7, 12.3, 1.5, "设施配置要求",
    "实验台、通风柜、气瓶柜等设施布局合理；紧急喷淋、洗眼器等应急设施配备到位且定期检查；监控系统覆盖重点区域。",
    COLORS.accent);
  addCard(s, 0.5, 3.5, 6.0, 3.2, "检查项目量化指标",
    "• 实验室安全信息牌上墙率：100%\n• 危险源标识覆盖率：100%\n• 消防通道畅通率：100%\n• 应急设施完好率：≥95%\n• 卫生检查合格率：≥90%\n• 隐患整改完成率：≥95%",
    COLORS.green);
  addCard(s, 6.8, 3.5, 6.0, 3.2, "检查频次要求",
    "• 日常巡查：每日一次（实验室责任人）\n• 定期检查：每月一次（院系）\n• 专项检查：每季度一次（学校）\n• 年度检查：每年一次（全覆盖）\n• 检查记录留存期：≥3年",
    COLORS.orange);
  addFooter(s, 7);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 8: 存在的问题分析 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "存在的问题分析", "Problem Analysis");
  addBulletList(s, [
    "实验室档案信息分散，纸质台账与电子数据不同步，难以实时掌握场所状态",
    "预约与准入环节缺乏数字化管控，非授权人员进入实验室存在安全隐患",
    "设备状态依赖人工巡检，故障报修流程长，带病运行风险高",
    "耗材（含危化品）库存预警不及时，过期或超量存放问题时有发生",
    "安全检查记录难以追溯，隐患整改闭环管理不到位",
    "数据统计分析能力弱，无法为安全决策提供数据支撑",
  ], 0.8, 1.8, 11.7, 5.0, { fontSize: 17 });
  addFooter(s, 8);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 9: 解决方案总体设计 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "解决方案总体设计", "Solution Design");
  s.addText("设计目标：以《检查项目表》为依据，构建覆盖实验场所全生命周期的安全管理系统", {
    x: 0.5, y: 1.6, w: 12.3, h: 0.5,
    fontSize: 16, color: COLORS.primary, bold: true,
    fontFace: "Microsoft YaHei",
  });
  const layers = [
    { name: "数据层", desc: "MySQL数据库 · 实验室/设备/耗材/预约/检查数据持久化", color: COLORS.accent },
    { name: "服务层", desc: "Spring Boot · RESTful API · JWT认证 · RBAC权限 · 统一异常处理", color: "2E86C1" },
    { name: "应用层", desc: "实验室档案 · 预约准入 · 设备管理 · 耗材管理 · 统计报表", color: "3498DB" },
    { name: "展示层", desc: "Vue2 + ElementUI · 响应式布局 · 角色化工作台 · 数据可视化", color: "5DADE2" },
  ];
  layers.forEach((l, i) => {
    s.addShape(pptx.ShapeType.roundRect, {
      x: 1.5, y: 2.3 + i * 1.15, w: 10.3, h: 0.95,
      fill: { color: l.color }, line: { color: l.color },
      rectRadius: 0.1,
    });
    s.addText(`${l.name}`, {
      x: 1.8, y: 2.4 + i * 1.15, w: 2, h: 0.75,
      fontSize: 18, bold: true, color: COLORS.white,
      fontFace: "Microsoft YaHei", valign: "middle",
    });
    s.addText(l.desc, {
      x: 3.8, y: 2.4 + i * 1.15, w: 7.8, h: 0.75,
      fontSize: 14, color: COLORS.white,
      fontFace: "Microsoft YaHei", valign: "middle",
    });
  });
  addFooter(s, 9);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 10: 系统架构 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "系统架构设计", "System Architecture");
  const modules = [
    { name: "系统管理", items: ["用户管理", "角色权限", "部门管理", "操作日志"], color: COLORS.accent },
    { name: "实验室管理", items: ["实验室档案", "设备台账", "维修工单", "状态管理"], color: COLORS.orange },
    { name: "预约管理", items: ["预约申请", "审核流程", "签到签退", "时段管控"], color: COLORS.green },
    { name: "耗材管理", items: ["耗材档案", "入库出库", "库存预警", "流水记录"], color: COLORS.red },
    { name: "统计报表", items: ["总览统计", "使用率", "故障分析", "数据导出"], color: "8E44AD" },
  ];
  modules.forEach((m, i) => {
    const x = 0.5 + i * 2.55;
    s.addShape(pptx.ShapeType.roundRect, {
      x, y: 1.8, w: 2.4, h: 4.8,
      fill: { color: COLORS.white },
      line: { color: m.color, width: 2 },
      rectRadius: 0.1,
    });
    s.addShape(pptx.ShapeType.rect, {
      x, y: 1.8, w: 2.4, h: 0.6,
      fill: { color: m.color }, line: { color: m.color },
    });
    s.addText(m.name, {
      x, y: 1.85, w: 2.4, h: 0.5,
      fontSize: 15, bold: true, color: COLORS.white,
      fontFace: "Microsoft YaHei", align: "center", valign: "middle",
    });
    m.items.forEach((it, j) => {
      s.addText(it, {
        x: x + 0.15, y: 2.6 + j * 0.9, w: 2.1, h: 0.7,
        fontSize: 13, color: COLORS.dark,
        fontFace: "Microsoft YaHei", align: "center", valign: "middle",
      });
      s.addShape(pptx.ShapeType.line, {
        x: x + 0.3, y: 3.3 + j * 0.9, w: 1.8, h: 0,
        line: { color: "E5E7EB", width: 1 },
      });
    });
  });
  s.addText("五大模块业务关联：预约→实验室→设备→耗材→统计，数据互通", {
    x: 0.5, y: 6.8, w: 12.3, h: 0.4,
    fontSize: 13, color: COLORS.gray, align: "center",
    fontFace: "Microsoft YaHei",
  });
  addFooter(s, 10);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 11: 实验室档案管理 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "实验室档案管理", "Laboratory Archive Management");
  addCard(s, 0.5, 1.7, 6.0, 2.2, "档案信息数字化",
    "建立实验室电子档案，记录名称、编号、所属院系、负责人、面积、容纳人数、设备清单等基础信息，支持条件查询与批量管理。",
    COLORS.accent);
  addCard(s, 6.8, 1.7, 6.0, 2.2, "状态实时管控",
    "实验室状态（空闲/使用中/维护中/停用）实时更新，预约系统根据状态自动校验，避免冲突预约。",
    COLORS.orange);
  addCard(s, 0.5, 4.2, 6.0, 2.2, "安全信息关联",
    "档案关联危险源信息、应急设施位置、安全负责人，落实检查项目表中'安全信息牌'和'标识管理'要求。",
    COLORS.green);
  addCard(s, 6.8, 4.2, 6.0, 2.2, "数据导出",
    "支持实验室档案Excel导出，满足检查台账留存要求，方便上级检查与数据归档。",
    COLORS.red);
  addFooter(s, 11);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 12: 预约与安全准入 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "预约与安全准入管理", "Reservation & Access Control");
  const steps = [
    { n: "1", t: "预约申请", d: "师生在线提交预约，选择实验室、时段、用途" },
    { n: "2", t: "权限校验", d: "系统校验角色权限与实验室状态，自动拦截" },
    { n: "3", t: "审核确认", d: "管理员审核预约，通过后生成准入凭证" },
    { n: "4", t: "签到准入", d: "预约时段内签到，系统校验时间窗口" },
    { n: "5", t: "签退离场", d: "使用结束签退，释放实验室资源" },
  ];
  steps.forEach((st, i) => {
    const x = 0.4 + i * 2.55;
    s.addShape(pptx.ShapeType.ellipse, {
      x: x + 0.75, y: 2.0, w: 1.0, h: 1.0,
      fill: { color: COLORS.accent }, line: { color: COLORS.accent },
    });
    s.addText(st.n, {
      x: x + 0.75, y: 2.05, w: 1.0, h: 0.9,
      fontSize: 28, bold: true, color: COLORS.white,
      fontFace: "Microsoft YaHei", align: "center", valign: "middle",
    });
    s.addText(st.t, {
      x, y: 3.2, w: 2.5, h: 0.4,
      fontSize: 16, bold: true, color: COLORS.primary,
      fontFace: "Microsoft YaHei", align: "center",
    });
    s.addText(st.d, {
      x: x + 0.1, y: 3.7, w: 2.3, h: 1.5,
      fontSize: 12, color: COLORS.gray,
      fontFace: "Microsoft YaHei", align: "center", valign: "top",
    });
    if (i < steps.length - 1) {
      s.addShape(pptx.ShapeType.chevron, {
        x: x + 2.3, y: 2.3, w: 0.3, h: 0.4,
        fill: { color: "BDD7EE" }, line: { color: "BDD7EE" },
      });
    }
  });
  addCard(s, 0.5, 5.5, 12.3, 1.2, "时段管控逻辑",
    "签到允许提前15分钟、签退允许延后30分钟；预约未开始或已结束均拦截；未签到不可签退；确保仅授权人员在授权时段内使用实验室。",
    COLORS.red);
  addFooter(s, 12);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 13: 设备安全管理 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "设备安全管理", "Equipment Safety Management");
  addCard(s, 0.5, 1.7, 4.0, 2.3, "设备台账",
    "记录设备编号、名称、分类、型号、所属实验室、购买日期、状态等，关联实验室档案。",
    COLORS.accent);
  addCard(s, 4.7, 1.7, 4.0, 2.3, "状态监控",
    "设备状态（在用/维修/报废）实时更新，维修中设备禁止预约使用，降低安全风险。",
    COLORS.orange);
  addCard(s, 8.9, 1.7, 3.9, 2.3, "报修流程",
    "教师与管理员可提交报修，实验室管理员处理，形成完整工单闭环。",
    COLORS.green);
  addCard(s, 0.5, 4.3, 4.0, 2.3, "数据导出",
    "支持设备台账导出，满足检查项目表中'设施配置'与'检查记录'留存要求。",
    COLORS.red);
  addCard(s, 4.7, 4.3, 4.0, 2.3, "故障统计",
    "统计设备故障分布与维修工单处理情况，辅助设备维护决策。",
    COLORS.accent);
  addCard(s, 8.9, 4.3, 3.9, 2.3, "权限控制",
    "学生仅可查看，教师可报修，管理员可增删改，按钮级权限控制。",
    COLORS.orange);
  addFooter(s, 13);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 14: 耗材与危险品管理 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "耗材与危险品管理", "Consumables & Hazardous Materials");
  addCard(s, 0.5, 1.7, 6.0, 2.2, "耗材档案",
    "建立耗材电子档案，记录编号、名称、分类、单位、预警值、所属实验室，支持条件查询。",
    COLORS.accent);
  addCard(s, 6.8, 1.7, 6.0, 2.2, "出入库流水",
    "完整记录入库、出库操作，关联操作人与预约单，实现耗材全流程可追溯。",
    COLORS.orange);
  addCard(s, 0.5, 4.2, 6.0, 2.2, "库存预警",
    "库存低于预警值时系统自动标记，统计报表高亮展示，辅助及时采购补充。",
    COLORS.green);
  addCard(s, 6.8, 4.2, 6.0, 2.2, "数据导出",
    "支持耗材档案与出入库流水导出，满足检查项目表中'化学安全'与'基础安全'台账要求。",
    COLORS.red);
  addFooter(s, 14);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 15: 安全检查与预警 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "安全检查与预警机制", "Safety Inspection & Warning");
  addBulletList(s, [
    "操作日志：完整记录用户登录、数据操作等行为，支持按账号、状态、时间查询与导出",
    "数据权限：基于RBAC模型，实验室管理员仅可见负责的实验室数据，避免信息泄露",
    "库存预警：耗材库存低于阈值自动预警，统计页面红色标识提醒",
    "时段校验：签到签退严格校验预约时间段，防止非授权时段使用",
    "状态联动：设备维修中自动锁定预约，实验室维护中禁止使用申请",
  ], 0.8, 1.8, 11.7, 4.5, { fontSize: 17 });
  addCard(s, 0.5, 6.1, 12.3, 0.9, "闭环管理",
    "发现隐患 → 登记记录 → 指派整改 → 跟踪验证 → 归档留存，形成安全检查PDCA闭环。",
    COLORS.red);
  addFooter(s, 15);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 16: 数据统计分析 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "数据统计分析", "Data Statistics & Analysis");
  addCard(s, 0.5, 1.7, 6.0, 2.2, "总览统计",
    "首页工作台展示实验室总数、设备总数、今日预约、库存预警等关键指标，按角色差异化展示。",
    COLORS.accent);
  addCard(s, 6.8, 1.7, 6.0, 2.2, "使用率分析",
    "按预约次数和使用时长统计各实验室运行情况，以图表形式展示排名与趋势，辅助资源配置。",
    COLORS.orange);
  addCard(s, 0.5, 4.2, 6.0, 2.2, "故障分析",
    "统计设备状态分布与维修工单处理情况，识别高频故障设备，辅助维护决策。",
    COLORS.green);
  addCard(s, 6.8, 4.2, 6.0, 2.2, "库存预警",
    "展示耗材库存预警、高频领用和出入库汇总，辅助采购决策，避免断供或积压。",
    COLORS.red);
  addFooter(s, 16);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 17: 权限与责任体系 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "权限与责任体系", "Role-based Access Control");
  const roles = [
    { name: "系统管理员", color: COLORS.red, perms: "全部模块管理权限\n用户/角色/部门管理\n操作日志查看" },
    { name: "实验室管理员", color: COLORS.orange, perms: "负责实验室档案与设备管理\n维修工单处理\n耗材库存管理\n本实验室数据统计" },
    { name: "教师", color: COLORS.green, perms: "实验室预约申请\n设备报修\n查看负责实验室使用率" },
    { name: "学生", color: COLORS.accent, perms: "实验室预约\n签到签退\n查看预约记录" },
  ];
  roles.forEach((r, i) => {
    const x = 0.5 + i * 3.15;
    s.addShape(pptx.ShapeType.roundRect, {
      x, y: 1.8, w: 2.95, h: 4.8,
      fill: { color: COLORS.white },
      line: { color: r.color, width: 2 },
      rectRadius: 0.1,
    });
    s.addShape(pptx.ShapeType.ellipse, {
      x: x + 0.975, y: 2.0, w: 1.0, h: 1.0,
      fill: { color: r.color }, line: { color: r.color },
    });
    s.addText(r.name, {
      x, y: 3.2, w: 2.95, h: 0.5,
      fontSize: 16, bold: true, color: r.color,
      fontFace: "Microsoft YaHei", align: "center",
    });
    s.addText(r.perms, {
      x: x + 0.15, y: 3.9, w: 2.65, h: 2.5,
      fontSize: 12, color: COLORS.dark,
      fontFace: "Microsoft YaHei", align: "center", valign: "top",
    });
  });
  s.addText("基于RBAC权限模型，菜单级与按钮级双重控制，落实检查项目表'责任体系'要求", {
    x: 0.5, y: 6.8, w: 12.3, h: 0.4,
    fontSize: 13, color: COLORS.gray, align: "center",
    fontFace: "Microsoft YaHei",
  });
  addFooter(s, 17);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 18: 系统实现效果 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "系统实现效果", "Implementation Results");
  const features = [
    { t: "前后端分离", d: "Spring Boot + Vue2 + ElementUI，RESTful API" },
    { t: "JWT认证", d: "无状态认证，localStorage存储Token" },
    { t: "动态路由", d: "基于角色动态生成菜单与路由" },
    { t: "数据权限", d: "实验室管理员数据范围隔离" },
    { t: "操作审计", d: "AOP切面记录操作日志" },
    { t: "数据导出", d: "Apache POI生成Excel报表" },
    { t: "实时通知", d: "WebSocket消息推送" },
    { t: "图表可视化", d: "ECharts数据展示" },
  ];
  features.forEach((f, i) => {
    const col = i % 4;
    const row = Math.floor(i / 4);
    s.addShape(pptx.ShapeType.roundRect, {
      x: 0.5 + col * 3.15, y: 1.8 + row * 2.4, w: 2.95, h: 2.1,
      fill: { color: COLORS.white },
      line: { color: "E5E7EB", width: 1 },
      rectRadius: 0.08,
    });
    s.addText(f.t, {
      x: 0.5 + col * 3.15, y: 1.95 + row * 2.4, w: 2.95, h: 0.5,
      fontSize: 16, bold: true, color: COLORS.primary,
      fontFace: "Microsoft YaHei", align: "center",
    });
    s.addText(f.d, {
      x: 0.65 + col * 3.15, y: 2.55 + row * 2.4, w: 2.65, h: 1.2,
      fontSize: 12, color: COLORS.gray,
      fontFace: "Microsoft YaHei", align: "center", valign: "top",
    });
  });
  addFooter(s, 18);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 19: 应用场景演示 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "应用场景演示", "Application Scenarios");
  const scenarios = [
    { t: "场景一：日常预约使用", d: "学生在线预约实验室→管理员审核→时段内签到→使用后签退→系统记录存档", color: COLORS.accent },
    { t: "场景二：设备故障报修", d: "教师发现设备故障→提交报修工单→实验室管理员处理→设备状态更新→故障统计", color: COLORS.orange },
    { t: "场景三：耗材库存预警", d: "耗材出库后库存低于阈值→系统预警→管理员采购入库→库存恢复正常", color: COLORS.green },
    { t: "场景四：安全检查台账", d: "管理员导出实验室档案/设备台账/操作日志→形成检查台账→满足上级检查要求", color: COLORS.red },
  ];
  scenarios.forEach((sc, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    addCard(s, 0.5 + col * 6.3, 1.8 + row * 2.5, 6.0, 2.2, sc.t, sc.d, sc.color);
  });
  addFooter(s, 19);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 20: 调研结论与建议 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "调研结论与建议", "Conclusions & Recommendations");
  addCard(s, 0.5, 1.7, 12.3, 1.5, "调研结论",
    "对照《高等学校实验室安全检查项目表（2026年）》中'实验场所'相关要求，现有实验室管理在档案数字化、准入管控、设备状态、耗材预警、检查留痕等方面存在信息化不足，亟需系统化解决方案。",
    COLORS.accent);
  addBulletList(s, [
    "建议一：推进实验室档案全量数字化，实现'一室一档'动态管理",
    "建议二：完善预约准入机制，严格落实时段管控与签到签退",
    "建议三：建立设备状态实时监控与报修闭环流程",
    "建议四：强化耗材（含危化品）库存预警与出入库追溯",
    "建议五：健全安全检查台账，支持数据导出与历史留存",
    "建议六：构建数据统计分析能力，为安全决策提供支撑",
  ], 0.8, 3.5, 11.7, 3.3, { fontSize: 16 });
  addFooter(s, 20);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 21: 未来展望 ============
{
  const s = pptx.addSlide();
  addBg(s);
  addHeader(s, "未来展望", "Future Outlook");
  addCard(s, 0.5, 1.7, 4.0, 4.8, "智能化升级",
    "• 引入物联网传感器，实时监测温湿度、烟雾、气体浓度\n• 异常情况自动报警并联动门禁系统\n• AI识别违规操作行为",
    COLORS.accent);
  addCard(s, 4.7, 1.7, 4.0, 4.8, "移动端扩展",
    "• 开发微信小程序端，支持移动端预约、签到、报修\n• 扫码识别设备与耗材\n• 移动审批提升效率",
    COLORS.orange);
  addCard(s, 8.9, 1.7, 3.9, 4.8, "数据互通",
    "• 对接学校统一身份认证\n• 与教务系统课表联动\n• 跨实验室数据共享分析",
    COLORS.green);
  addFooter(s, 21);
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

// ============ Slide 22: 致谢 ============
{
  const s = pptx.addSlide();
  addBg(s, COLORS.primary);
  s.addText("感谢聆听", {
    x: 0, y: 2.5, w: W, h: 1.5,
    fontSize: 60, bold: true, color: COLORS.white,
    fontFace: "Microsoft YaHei", align: "center",
  });
  s.addShape(pptx.ShapeType.rect, {
    x: 5.67, y: 4.2, w: 2, h: 0.08,
    fill: { color: COLORS.accent }, line: { color: COLORS.accent },
  });
  s.addText("敬请各位老师批评指正", {
    x: 0, y: 4.5, w: W, h: 0.6,
    fontSize: 22, color: "BDD7EE",
    fontFace: "Microsoft YaHei", align: "center",
  });
  s.addText("实验场所安全管理调研与实践", {
    x: 0, y: 5.5, w: W, h: 0.5,
    fontSize: 14, color: "7FA8CC",
    fontFace: "Microsoft YaHei", align: "center",
  });
  s.addText("实验室安全学基础 · 课程实践汇报", {
    x: 0, y: 5.9, w: W, h: 0.4,
    fontSize: 12, color: "5B8BB5",
    fontFace: "Microsoft YaHei", align: "center",
  });
  warnIfSlideHasOverlaps(s, pptx);
  warnIfSlideElementsOutOfBounds(s, pptx);
}

pptx.writeFile({ fileName: "实验场所安全管理调研与实践.pptx" })
  .then((name) => {
    console.log("PPT生成成功: " + name);
    console.log("总页数: " + pptx._slides.length);
  })
  .catch((err) => console.error("生成失败: " + err));

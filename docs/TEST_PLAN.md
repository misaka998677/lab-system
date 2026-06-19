# 实验室管理系统 — 前端功能页面全量测试计划

> 适用：lab-frontend（Vue 2 + Element UI + Vue Router）
> 测试方式：Playwright（`channel='msedge'`）+ Python
> 测试服务器：`http://localhost:8086`（前端 dev server），后端接口 `http://localhost:8081`

---

## 1. 测试范围与目标

### 1.1 覆盖模块（共 5 个模块，19 个页面 + 2 个公共页）

| 模块 | 页面 | 路由路径 | 组件文件 |
|------|------|----------|----------|
| **登录/注册** | 登录页 | `/login` | [login/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/login/index.vue) |
| | 注册页 | `/register` | [register/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/register/index.vue) |
| **仪表盘** | 首页仪表盘 | `/dashboard` | [dashboard/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/dashboard/index.vue) |
| **系统管理** | 用户管理 | `/system/user` | [system/user/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/system/user/index.vue) |
| | 角色管理 | `/system/role` | [system/role/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/system/role/index.vue) |
| | 菜单管理 | `/system/menu` | [system/menu/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/system/menu/index.vue) |
| | 部门管理 | `/system/dept` | [system/dept/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/system/dept/index.vue) |
| | 操作日志 | `/system/log` | [system/log/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/system/log/index.vue) |
| **实验室管理** | 实验室档案 | `/lab/room` | [lab/room/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/lab/room/index.vue) |
| | 设备台账 | `/lab/device` | [lab/device/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/lab/device/index.vue) |
| | 维修单 | `/lab/repair` | [lab/repair/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/lab/repair/index.vue) |
| **预约中心** | 我的预约 | `/reserve/mine` | [reserve/mine/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/reserve/mine/index.vue) |
| | 预约审核 | `/reserve/audit` | [reserve/audit/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/reserve/audit/index.vue) |
| | 签到记录 | `/reserve/check` | [reserve/check/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/reserve/check/index.vue) |
| **耗材管理** | 耗材档案 | `/stock/item` | [stock/item/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/stock/item/index.vue) |
| | 出入库 | `/stock/record` | [stock/record/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/stock/record/index.vue) |
| **数据统计** | 使用率分析 | `/stat/usage` | [stat/usage/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/stat/usage/index.vue) |
| | 设备故障 | `/stat/fault` | [stat/fault/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/views/stat/fault/index.vue) |
| | 库存预警 | `/stat/stock-warning` | [stat/usage/index.vue]（按后端返回的 path） |

### 1.2 测试目标
- **页面可加载**：每个页面路由都能打开，无空白页 / 渲染报错
- **导航可用**：侧边栏菜单点击可跳转到对应页面，无 `NavigationDuplicated` 报错
- **核心操作无 JS 报错**：打开表单弹窗、搜索、重置、分页切换等操作均可完成
- **控制台零错误**：页面无 `console.error` / `pageerror` / 路由异常
- **多角色可见性正确**：不同账号登录后，只能看到自己权限的菜单

---

## 2. 前置条件与环境

### 2.1 启动服务

```bash
# 后端（Java / Spring Boot）
cd lab-system/lab-backend
# 通过 IDE 或打包 jar 启动，默认端口 8081

# 前端（Vue CLI）
cd lab-system/lab-frontend
npm run serve
# 监听 http://localhost:8086
```

启动后用以下命令验证：

```bash
# 检查前端端口
netstat -ano | findstr ":8086"

# 检查后端端口
netstat -ano | findstr ":8081"
```

### 2.2 测试账号（4 种角色）

| 账号 | 密码 | 角色 | 期望可见菜单 |
|------|------|------|-------------|
| `admin` | `123456` | ROLE_ADMIN | 全部菜单 |
| `labadmin` | `123456` | ROLE_LABADMIN | 实验室管理、预约中心、耗材管理、数据统计 |
| `teacher` | `123456` | ROLE_TEACHER | 预约中心（我的预约、签到记录） |
| `student` | `123456` | ROLE_STUDENT | 预约中心（我的预约） |

### 2.3 测试断言规则

**页面级通过条件**（每个页面必须满足以下全部 4 条）：

1. **HTTP 200 可访问**：`page.goto(url)` 成功，非空白页
2. **关键元素可见**：页面内出现主容器/表格/搜索栏等核心组件（通过 `el-main`、`.page-header` 或模块专属 class 定位）
3. **控制台零错误**：`page.on('console', ...)` 中 `type === 'error'` 的数量为 0；`page.on('pageerror', ...)` 捕获到 0 条
4. **路由正确**：`page.url` 不包含 `/login`，且包含预期路径

**操作级通过条件**（按钮/表单/分页操作）：

- 按钮点击后无 JS 报错
- 弹窗打开/关闭无异常（`el-dialog` 渲染、`visible` 切换）
- 搜索后表格 `loading` 状态能正常切换回完成
- 分页切换不会导致页面崩溃

### 2.4 Pass/Fail 定义

| 符号 | 含义 | 判定标准 |
|------|------|---------|
| ✓ PASS | 全部断言通过 | 满足 4 条页面级条件 + 操作级无异常 |
| ⚠ WARN | 可工作但有非致命告警 | 页面可操作，但控制台有非致命 warning 或 404（例如 favicon） |
| ✗ FAIL | 功能失效 | 页面空白、路由卡死、JS 报错导致操作不可用 |

---

## 3. 分层测试计划

### 3.1 Level 1 — 登录/鉴权/注册（前置网关）

**目标**：验证登录流程、Token 持久化、注册入口、退出登录

| # | 用例 | 步骤 | 断言 |
|---|------|------|------|
| L1-1 | 登录页可访问 | 打开 `/login` | 页面标题"账号登录"可见；用户名/密码输入框存在 |
| L1-2 | 空表单提示 | 不填密码直接点"登录" | Element UI 表单校验提示出现 |
| L1-3 | 错误密码拒绝 | 用户名 `admin`，密码 `wrong` | 提示"认证失败/密码错误"，不跳转 |
| L1-4 | admin 登录成功 | `admin` / `123456` | 跳转到 `/dashboard`；侧边栏出现所有菜单 |
| L1-5 | Token 持久化 | 登录后刷新页面（F5） | 仍保持在业务页，不被踢回 `/login` |
| L1-6 | 退出登录 | 点击右上角 → "退出登录" | 跳回 `/login`；再次访问 `/dashboard` 被拦 |
| L1-7 | 注册页入口 | 登录页点"立即注册" | 跳转到 `/register`；注册表单存在 |
| L1-8 | 注册提交 | 填写用户名/密码/角色，提交 | 提交接口返回成功或"已存在"，不报 JS 错 |
| L1-9 | 其他角色登录 | 依次登录 `labadmin`/`teacher`/`student` | 进入仪表盘；菜单数量与角色权限匹配 |

### 3.2 Level 2 — 全页面导航冒烟（点击模式）

**目标**：从侧边栏依次点击每个菜单，验证路由正确跳转、页面无报错

| # | 模块 | 页面 | 路径 | 断言 |
|---|------|------|------|------|
| L2-1 | 仪表盘 | 首页 | `/dashboard` | 欢迎区域 + 指标卡片 + ECharts 图表全部渲染；无 JS 报错 |
| L2-2 | 系统管理 | 用户管理 | `/system/user` | 搜索框存在；表格有列；"新增/编辑/删除"按钮可见 |
| L2-3 | | 角色管理 | `/system/role` | 同上；注意**重复点击不报错**（NavigationDuplicated 防御） |
| L2-4 | | 菜单管理 | `/system/menu` | 树形菜单展示成功；页面无白屏 |
| L2-5 | | 部门管理 | `/system/dept` | 树形结构 + 表格；按钮点击无报错 |
| L2-6 | | 操作日志 | `/system/log` | 日志表格 + 分页；可翻页；可按关键词搜索 |
| L2-7 | 实验室管理 | 实验室档案 | `/lab/room` | 表格渲染；新增弹窗可打开/关闭 |
| L2-8 | | 设备台账 | `/lab/device` | 同上；搜索关键字不崩溃 |
| L2-9 | | 维修单 | `/lab/repair` | 维修状态标签渲染；状态筛选按钮可用 |
| L2-10 | 预约中心 | 我的预约 | `/reserve/mine` | 列表 + 分页；有"新增预约"按钮 |
| L2-11 | | 预约审核 | `/reserve/audit` | 审核按钮存在；表格 action 列渲染 |
| L2-12 | | 签到记录 | `/reserve/check` | 签到状态显示；分页正常 |
| L2-13 | 耗材管理 | 耗材档案 | `/stock/item` | 表格含库存量、库存预警列；新增弹窗可打开 |
| L2-14 | | 出入库 | `/stock/record` | 入库/出库单选框；表格可正常显示 |
| L2-15 | 数据统计 | 使用率分析 | `/stat/usage` | ECharts 柱状图/饼图渲染；图表尺寸不为 0 |
| L2-16 | | 设备故障 | `/stat/fault` | 图表渲染；标题可见 |
| L2-17 | | 库存预警 | `/stat/stock-warning` | 预警列表渲染；无空白页 |

### 3.3 Level 3 — 页面内操作（表单 / 搜索 / 分页 / 弹窗）

**目标**：验证每个业务页的按钮级交互不会触发 JS 报错或路由异常

| # | 页面 | 测试项 | 步骤 | 断言 |
|---|------|--------|------|------|
| L3-1 | 所有 CRUD 页面 | 搜索框 | 输入任意文本 → 点"搜索"；点"重置" | 表格 `loading` 会消失；控制台无 error |
| L3-2 | | 分页切换 | 点"下一页"；再点"第 1 页" | URL 不跳变/保持在原路径；表格数据更新 |
| L3-3 | | 新增按钮 | 点"新增" | `el-dialog` 可见；关闭后 `el-dialog` 消失 |
| L3-4 | | 编辑按钮 | 选中一行 → 点"编辑" | 弹窗预填数据；关闭不报错 |
| L3-5 | | 删除按钮 | 选中一行 → 点"删除"（不二次确认提交） | 确认弹窗出现；取消后表格不变 |
| L3-6 | dashboard | 图表渲染 | 打开页 → 等待 2s | ECharts 容器尺寸 > 0；无 `ResizeObserver` 报错 |
| L3-7 | 统计页 | 图表自适应 | 窗口 resize（可选） | 图表重新渲染，无异常 |

### 3.4 Level 4 — 多角色权限隔离

**目标**：验证 SidebarItem 与路由守卫按角色正确过滤

| # | 账号 | 期望 | 不期望 |
|---|------|------|--------|
| L4-1 | `admin` | 5 个子菜单（系统/实验室/预约/耗材/统计）全部展开可点 | — |
| L4-2 | `labadmin` | 实验室管理、预约中心、耗材管理、数据统计可见 | 看不到"系统管理"菜单 |
| L4-3 | `teacher` | 预约中心（我的预约、签到记录）可见 | 看不到系统管理/耗材管理/实验室管理/统计 |
| L4-4 | `student` | 预约中心（我的预约）可见 | 其他菜单不出现 |
| L4-5 | 越权访问（手动改 URL） | `student` 直接访问 `/system/user` | 被路由守卫重定向到 `/dashboard` 或提示无权限，不崩溃 |

### 3.5 Level 5 — 回归测试（常见回归陷阱）

| # | 回归场景 | 检查点 |
|---|----------|--------|
| R-1 | 重复点击同一菜单项 | 不出现 `NavigationDuplicated`；不出现 Vue Router 控制台 error |
| R-2 | 快速切换菜单 | 连续点 5-8 个菜单，不出现路由卡死 |
| R-3 | 刷新业务页 | 停留在 `/lab/device` → 刷新 → 仍在 `/lab/device` | Token 有效且能恢复 |
| R-4 | 登录页刷新 | 停留在 `/login` → 刷新 → 仍在登录页可输入 |
| R-5 | 接口 401 未鉴权 | Token 过期后点击菜单项 → 被踢回 `/login`；不报 JS 错 |

---

## 4. 测试脚本设计规范

使用 Playwright 编写自动化脚本，遵循以下模板：

```python
from playwright.sync_api import sync_playwright

BASE_URL = 'http://localhost:8086'

def login(page, username, password='123456'):
    """统一登录入口"""
    page.goto(f'{BASE_URL}/#/login')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)
    page.locator('input[placeholder="请输入账号"]').fill(username)
    page.locator('input[placeholder="请输入密码"]').fill(password)
    page.locator('.login-btn').click()
    # 等待登录后的变化：等待 dashboard 元素或 URL 不含 /login
    page.wait_for_timeout(3000)

def assert_no_console_errors(console_errors, page_errors):
    """核心断言：无 JS 报错"""
    fatal = [e for e in console_errors + page_errors
             if 'NavigationDuplicated' in e or 'TypeError' in e
             or 'Cannot read' in e or 'chunk' in e.lower()]
    assert len(fatal) == 0, f'发现致命报错 {fatal}'
```

### 4.1 测试脚本命名与目录

```
lab-system/
├─ _tests_smoke.py        # L1+L2 快速冒烟（主要用例）
├─ _tests_pageops.py      # L3 页面内操作（搜索/分页/弹窗）
├─ _tests_roles.py        # L4 多角色权限隔离
└─ _tests_regression.py   # L5 回归测试（重复点击/刷新/Token失效）
```

### 4.2 成功判定模板

```python
def test_smoke_page(page, path, title_keyword):
    """标准页面冒烟测试"""
    errors = []
    page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)
    page.on('pageerror', lambda err: errors.append(str(err)))

    page.goto(f'{BASE_URL}/#{path}')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)

    # 1. URL 不被踢回 login
    assert '/login' not in page.url, f'{path} 被踢回登录页'

    # 2. 路由命中
    assert path in page.url or 'dashboard' in page.url, f'{path} 路由异常 → {page.url}'

    # 3. 关键元素存在（el-main 或模块专属 class）
    body = page.content().lower()
    assert title_keyword in body, f'{path} 页面内容为空，关键词 "{title_keyword}" 未出现'

    # 4. 控制台无致命错误
    fatal = [e for e in errors
             if any(k in e for k in ['NavigationDuplicated', 'TypeError', 'Cannot read', 'ChunkLoadError'])]
    assert not fatal, f'{path} 控制台致命错误: {fatal}'

    return True
```

---

## 5. 安全测试计划（Security）

> 面向 OWASP API Security Top 10 的最小可验证测试集。脚本可复用上面的 HTTP 请求工具，主要用 curl / Python requests 直接打后端（`http://localhost:8080`）。

### 5.1 Sec-1 未认证访问（Broken Authentication）

| # | 用例 | 步骤 | 断言 |
|---|------|------|------|
| SEC1-1 | 无 Token 访问受保护接口 | 不带 `Authorization` 请求 `/system/user/page` | HTTP 401 或 403；响应体为统一 JSON，不暴露堆栈 |
| SEC1-2 | 伪造/过期 Token 访问 | 用随机字符串做 `Bearer xxx` 请求同上 | 401 |
| SEC1-3 | 登录暴力破解防护 | 对 `admin` 账号连续 6 次用错误密码登录 | 第 6 次响应应出现"尝试过于频繁/账号已锁定"字样 |
| SEC1-4 | 弱密码检测 | 用 `12345`、`123456`、`admin` 作为新用户密码注册 | 前端/后端至少一处拒绝（密码长度不足） |

### 5.2 Sec-2 越权访问（BOLA / Broken Access Control）

| # | 用例 | 步骤 | 断言 |
|---|------|------|------|
| SEC2-1 | 学生操作他人预约 | 以 `student` 登录；取到自己的一个预约 ID 后，改用 teacher 的 Token 调用 PUT `/reserve/{id}/cancel` | 403 或 "只能操作自己的预约" 提示；数据库状态未变 |
| SEC2-2 | 学生删除他人维修单 | 以 `student` 登录；获取一个维修单 ID，调用 DELETE `/lab/repair/{id}` | 403 或 "仅管理员可删除维修单"；数据库未变 |
| SEC2-3 | teacher 访问用户管理 | 用 teacher 的 Token 请求 `/system/user/page` | 403；前端菜单也不出现在 teacher 账户 |
| SEC2-4 | labadmin 访问系统用户 | 用 labadmin 的 Token 请求 `/system/user/page` | 403 |
| SEC2-5 | 越权审核 | teacher/student Token 请求 PUT `/reserve/{id}/audit?pass=true` | 403；预约状态未变 |

### 5.3 Sec-3 SQL 注入

| # | 用例 | 步骤 | 断言 |
|---|------|------|------|
| SEC3-1 | LIKE 注入关键字 | `/system/user/page?keyword=' OR 1=1 -- ` | 返回 200；查询结果为空或仅返回匹配用户名；不会全表输出 |
| SEC3-2 | LIKE 通配符测试 | `keyword=%` 或 `keyword=_` | 返回正常 200；响应体不出现所有用户 |
| SEC3-3 | 参数注入 | 对 `/lab/device/page?keyword=1' or '1'='1` | 响应状态正常；无 SQL 异常堆栈 |

### 5.4 Sec-4 日志与信息泄露

| # | 用例 | 步骤 | 断言 |
|---|------|------|------|
| SEC4-1 | 登录接口日志脱敏 | admin 登录 1 次 → 查询 sys_log 表 | 日志中 `password` 字段应为 `***` 或不存在 |
| SEC4-2 | 异常响应无堆栈 | 调用不存在的接口、或传参格式错误 | 响应体仅返回 `{code,message}` 结构，不含 java stacktrace |
| SEC4-3 | CORS origin 限制 | 用 `Origin: http://evil.com` 请求 `/auth/login` | 响应头 `Access-Control-Allow-Origin` 为 null 或不出现在响应中（至少不是 `*`） |
| SEC4-4 | HTTP 安全响应头 | 任意正常响应 | 响应头必须包含 `X-Frame-Options: DENY` 和 `X-Content-Type-Options: nosniff` |

---

## 6. 执行步骤与优先级

### Phase 1（P0，1-2 小时）— L1 登录 + L2 全页面冒烟
1. 验证前端（8086）与后端（8081）启动正常
2. 运行 `_tests_smoke.py`：逐个角色登录，菜单点击导航测试
3. 记录每个页面的 PASS / FAIL / WARN

### Phase 2（P1，2-3 小时）— L3 页面级操作测试
1. 对每个表格页执行搜索 / 重置 / 分页 / 新增弹窗操作
2. 重点验证：`/lab/device`、`/stock/item`、`/system/user`、`/reserve/mine` 四个高频页
3. 对 dashboard 验证图表渲染非 0

### Phase 3（P1，1 小时）— L4 多角色权限
1. 以每个账号登录 → 检查可见菜单数量（与预期对比）
2. 以 student 手动构造越权 URL（`/system/user`）→ 验证被拦截

### Phase 4（P2，1 小时）— L5 回归测试
1. 重复点击同一菜单 3 次 → 控制台检查
2. 快速在 6 个菜单间切换 → 不卡死
3. 刷新业务页 → 保持路径

---

## 6. 报告模板

每次测试输出以下格式的表格（记录到报告文件）：

```
测试日期: 2026-06-18
浏览器:  Edge (msedge)
环境:    localhost:8086 + localhost:8081

[Phase 1 — 冒烟测试]
  ✓ /dashboard
  ✓ /system/user
  ✓ /system/role
  ✓ /system/menu
  ✓ /system/dept
  ✓ /system/log
  ✓ /lab/room
  ✓ /lab/device
  ✓ /lab/repair
  ✓ /reserve/mine
  ✓ /reserve/audit
  ✓ /reserve/check
  ✓ /stock/item
  ✓ /stock/record
  ✓ /stat/usage
  ✓ /stat/fault
  ✓ /stat/stock-warning

[Phase 3 — 页面操作]
  ✓ 用户管理 - 搜索/重置/分页/新增弹窗
  ✓ 设备台账 - 搜索/分页/编辑
  ✓ 耗材档案 - 搜索/分页/新增
  ...

[Phase 4 — 权限]
  ✓ admin 可见 5 个模块
  ✓ labadmin 不可见系统管理
  ✓ teacher 仅可见预约中心
  ✓ student 仅可见我的预约

[Phase 5 — 回归]
  ✓ 重复点击菜单无 NavigationDuplicated
  ✓ 刷新业务页保持路径

结论: ALL PASS / N FAIL (详情见日志文件)
```

---

## 7. 常见问题预案

| 问题 | 定位方式 | 处理思路 |
|------|----------|----------|
| 点击菜单无反应 | `el-menu` 是否有 `router` 属性；SidebarItem 的 `@click` 是否绑定 | 检查 [layout/index.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/layout/index.vue) 和 [SidebarItem.vue](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/layout/components/SidebarItem.vue) |
| NavigationDuplicated | Playwright 监听 console.error + pageerror | 在 `handleMenuClick` 中提前 `return` 同路径，并对 `router.push()` 加 `.catch` |
| 页面空白 | 控制台看是否 `ChunkLoadError`（懒加载失败） | 确认 [permission.js](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/store/modules/permission.js) 中 `componentMap` 路径正确 |
| 登录后菜单为空 | 检查 `permission/dynamicRoutes` 是否有数据；`getMenus()` 接口返回 | 后端 `/auth/menu` 接口是否正常 |
| 401 Token 失效 | 控制台请求显示 401 | 检查 [request.js](file:///c:/Users/12980/Desktop/lab-system/lab-frontend/src/utils/request.js) 的响应拦截器是否自动跳登录 |
| 图表尺寸为 0 | 元素尺寸为 0 时 ECharts 不渲染 | 检查容器是否有 `width`/`height` 显式设定 |
| Playwright 元素可见性超时 | 子菜单在展开后才出现 | 必须先点击父级 `el-submenu__title` 再点击子项 |

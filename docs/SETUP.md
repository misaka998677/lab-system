# 实验室资源预约管理系统 —— 启动与验收指南（SETUP.md）

> 面向课程考核：用于项目启动、功能验收与现场答辩。
> 阅读本文档后，你应能在 15 分钟内将系统跑通，并完成核心功能的演示。

---

## 1. 运行环境要求

| 项目 | 要求 | 版本推荐 |
|------|------|----------|
| JDK | 17 及以上 | OpenJDK 17 / Oracle JDK 17 |
| Maven | 3.8 及以上 | Maven 3.8.x |
| Node.js | 16 及以上 | Node.js 16 / 18 LTS |
| MySQL | 8.0 及以上 | MySQL 8.0 |
| 浏览器 | Chrome / Edge / Firefox | 最新稳定版 |
| 操作系统 | Windows / Linux / macOS | — |

> 验证命令（安装完成后可执行检查）：
>
> ```bash
> java -version     # 应输出 openjdk 17.x.x 或 17 以上
> mvn -version      # 应输出 Apache Maven 3.8.x 或更高
> node -v           # 应输出 v16.x.x / v18.x.x
> npm -v            # 应输出 8.x 或更高
> mysql --version   # 应输出 MySQL 8.0.x
> ```

---

## 2. 数据库初始化步骤

项目 SQL 脚本位于：`sql/lab_system.sql`（建表脚本）与 `sql/demo_data.sql`（演示数据）。

### 2.1 详细步骤

1. **登录 MySQL**
   ```bash
   mysql -u root -p
   ```
   然后输入密码进入 MySQL 命令行。

2. **创建数据库**
   ```sql
   CREATE DATABASE lab_system DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
   ```

3. **切换到目标数据库**
   ```sql
   USE lab_system;
   ```

4. **执行建表脚本**（注意路径替换为你本地绝对路径或相对路径）
   ```sql
   source C:/Users/12980/Desktop/lab-system/sql/lab_system.sql;
   ```
   > Windows 下推荐使用正斜杠 `/` 或双反斜杠 `\\`。

5. **执行演示数据脚本**
   ```sql
   source C:/Users/12980/Desktop/lab-system/sql/demo_data.sql;
   ```

6. **验证建表结果**
   ```sql
   SHOW TABLES;
   ```
   预期返回 **13 张表**：`sys_dept`、`sys_log`、`sys_menu`、`sys_role`、`sys_user`、`sys_user_role`、`lab_room`、`lab_device`、`lab_device_repair`、`reservation`、`stock_item`、`stock_record`、`stat_cache`。

7. **查看默认演示账号**
   ```sql
   SELECT id, username, real_name FROM sys_user WHERE deleted = 0;
   ```
   应能看到 admin / labadmin / teacher1 / student1 等账号。

### 2.2 备选：MySQL Workbench / Navicat 方式

- 新建连接 → 连接到本地 `3306` 端口
- 新建 Schema：`lab_system`，字符集 `utf8mb4`
- 右键该 Schema → **Run SQL Script** → 依次选择 `lab_system.sql` 与 `demo_data.sql`
- 刷新 Schema，确认表数量为 13

---

## 3. 后端启动步骤

后端项目位于：`lab-backend/`

### 3.1 详细步骤

1. **进入后端项目目录**
   ```bash
   cd lab-backend
   ```

2. **修改数据库连接信息**

   打开文件：`lab-backend/src/main/resources/application.yml`

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/lab_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
       username: root                      # ← 修改为你的 MySQL 用户名
       password: 123456                    # ← 修改为你的 MySQL 密码
   ```

   > 若你的 MySQL 端口非 `3306`，请同步修改 `url` 中的端口号。

3. **启动 Spring Boot 应用（跳过测试）**
   ```bash
   mvn spring-boot:run -DskipTests
   ```
   首次启动 Maven 会下载依赖，网络良好情况下大约 2–5 分钟。

4. **启动成功标志**

   控制台出现类似如下日志：
   ```
   Tomcat started on port 9090 (http) with context path '/api'
   Started LabBackendApplication in X.XXX seconds
   ```

   后端运行于：**`http://localhost:9090/api`**

5. **验证接口（可选）**

   使用 Postman / curl 发送 POST 请求到登录接口：
   ```
   POST http://localhost:9090/api/auth/login
   Content-Type: application/json

   {
     "username": "admin",
     "password": "123456"
   }
   ```
   预期返回：`code=200`，并包含 `token` 字段。

---

## 4. 前端启动步骤

前端项目位于：`lab-frontend/`

### 4.1 详细步骤

1. **进入前端项目目录**
   ```bash
   cd lab-frontend
   ```

2. **安装依赖**
   ```bash
   npm install
   ```
   > 如遇网络问题，可临时切换到国内镜像：`npm config set registry https://registry.npmmirror.com`

3. **启动开发服务器**
   ```bash
   npm run serve
   ```

4. **启动成功标志**

   控制台出现：
   ```
   App running at:
   - Local:   http://localhost:8087/
   ```

   前端运行于：**`http://localhost:8087`**

5. **浏览器访问**

   打开 `http://localhost:8087`，进入登录页。

---

## 5. 默认演示账号

演示账号已通过 `sql/demo_data.sql` 注入，所有账号密码统一为 **`123456`**。

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| admin | 123456 | ROLE_ADMIN | 超级管理员：所有菜单可见，看到所有数据 |
| labadmin | 123456 | ROLE_LABADMIN | 实验室管理员：可审核预约、管理设备与耗材库存 |
| teacher1 | 123456 | ROLE_TEACHER | 教师：可提交预约、查看自己负责的实验室、报修设备 |
| student1 | 123456 | ROLE_STUDENT | 学生：可提交预约、提交报修、签到签退，仅能看到自己相关的数据 |

---

## 6. 核心功能验收清单（课程考核用）

> 每一条验收项均为答辩时可现场演示的操作。建议按照本清单逐项验证，**A01–A25 全部通过视为项目功能完整**。

| 编号 | 功能名称 | 验收步骤 | 预期结果 |
|------|----------|----------|----------|
| **A01** | **用户登录** | 1. 访问 `http://localhost:8087` → 输入 `admin / 123456`<br>2. 点击"登录" | 登录成功，跳转到工作台首页，顶部显示当前登录用户信息 |
| **A02** | **用户注册** | 1. 登录页 → 点击"去注册"<br>2. 填写新账号信息，默认角色为学生<br>3. 返回登录页用新账号登录 | 注册成功后返回登录页；新账号可正常登录，进入工作台 |
| **A03** | **实验室档案 CRUD** | 以 `admin` 登录：<br>1. 菜单：实验室管理 → 实验室档案<br>2. 点击"新增"填写并保存<br>3. 点击某行"编辑"修改后保存<br>4. 点击搜索输入关键字后查询<br>5. 勾选多行点击"删除" | 新增后列表立即出现新记录；编辑后字段更新；搜索仅返回匹配项；删除后数据从列表消失（逻辑删除，数据库 `deleted=1`） |
| **A04** | **实验室条件查询与筛选** | 1. 进入实验室档案页<br>2. 分别输入"实验室名称"、选择"类型"、输入"容纳人数"区间<br>3. 点击"搜索" | 列表结果根据条件动态变化；点击"重置"可清空条件 |
| **A05** | **实验室批量导入** | 1. 实验室档案页 → 点击"导入"<br>2. 下载导入模板 → 按模板填写 3–5 条记录<br>3. 上传 Excel → 点击"确认导入"<br>4. 查看导入结果 | 导入成功后返回成功条数与失败条数；刷新列表可见新数据；错误行会显示具体错误提示 |
| **A06** | **设备台账 CRUD** | 1. 菜单：实验室管理 → 设备台账<br>2. 执行 新增 / 编辑 / 删除 / 搜索 | 设备新增后可关联到所属实验室；删除后不再出现在列表 |
| **A07** | **设备条件查询** | 1. 设备台账页 → 输入"设备名称/编号"<br>2. 选择"状态/所属实验室"<br>3. 点击搜索 | 条件组合查询生效，返回正确记录 |
| **A08** | **设备报修提交** | 以 `student1` 登录：<br>1. 菜单：设备维修 → 报修管理<br>2. 点击"新增报修"，选择设备、填写描述<br>3. 提交 | 报修单状态为"待处理"，同时工作台"我的报修"卡片数字 +1 |
| **A09** | **维修工单状态流转** | 1. 以 `labadmin` 登录，进入"报修管理"<br>2. 对新工单点击"处理中"<br>3. 再点击"已完成"，填写维修说明<br>4. 以 `student1` 登录查看 | 状态从"待处理" → "处理中" → "已完成"，时间线完整记录；学生端可看到自己工单状态变化 |
| **A10** | **预约申请提交** | 以 `student1` 登录：<br>1. 菜单：预约中心 → 我要预约<br>2. 选择实验室、日期、时段，填写用途<br>3. 点击"提交预约" | 预约状态为"待审核"，可在"我的预约"中看到新记录 |
| **A11** | **预约审核（实验室管理员）** | 以 `labadmin` 登录：<br>1. 菜单：预约中心 → 预约审核<br>2. 对一条预约点击"通过"，另一条点击"驳回"<br>3. 以学生端登录查看结果 | "通过"后预约状态变为"已通过"；"驳回"需填写理由；学生端可看到审核结果 |
| **A12** | **预约签到与签退** | 1. 用 `student1` 登录 → "我的预约" → 对一条"已通过"预约点击"签到"<br>2. 使用后点击"签退" | 状态从"已通过" → "使用中" → "已完成"，并自动记录签到/签退时间 |
| **A13** | **预约超时自动处理（5 分钟定时任务）** | 1. 以学生提交一个"已通过"预约，但不签到<br>2. 等待超过预约开始时间 5 分钟以上（或手动调整数据库 `reservation.start_time`）<br>3. 观察预约状态变化 | 系统自动将超时未签到的预约标记为"已取消"，并在日志中可见 |
| **A14** | **我的预约查看** | 以 `student1` 登录，菜单：预约中心 → 我的预约 | 列表仅显示当前用户提交的预约，区分"待审核/已通过/已驳回/使用中/已完成/已取消"等状态标签 |
| **A15** | **耗材档案 CRUD** | 以 `labadmin` 登录：<br>1. 菜单：耗材库存 → 耗材档案<br>2. 新增 / 编辑 / 删除 / 搜索 | 耗材档案含名称、型号、库存阈值等字段；删除为逻辑删除 |
| **A16** | **耗材入库登记** | 1. 菜单：耗材库存 → 库存台账<br>2. 点击"入库"，选择耗材、填写数量与备注<br>3. 保存 | 耗材总库存增加；同时生成一条入库流水记录（库存记录页可见） |
| **A17** | **耗材出库登记** | 1. 库存台账 → 点击"出库"，选择耗材、填写数量与用途<br>2. 保存 | 总库存减少；生成出库流水记录；若库存不足会提示无法出库 |
| **A18** | **库存预警查看** | 1. 菜单：统计分析 → 库存预警<br>2. 对某一预警耗材执行入库操作 | 列表显示"库存 < 预警阈值"的耗材；入库后若库存超过阈值，则该耗材从预警列表消失 |
| **A19** | **工作台数据看板** | 用任意账号登录首页 | 显示 4–6 张关键指标卡片（总预约数、今日报修、库存预警、使用率等）和图表，数据实时准确 |
| **A20** | **统计分析（使用率/故障分布/库存预警）** | 1. 菜单：统计分析 → 使用率/故障分布/库存预警<br>2. 切换时间范围查看 | 三种图表（饼图/柱状图/折线图）数据随时间范围变化而刷新 |
| **A21** | **WebSocket 实时刷新验证** | 1. 以 `student1` 登录，在工作台停留<br>2. 打开浏览器开发者工具 → Network → WS，确认存在 WebSocket 连接<br>3. 用同一账号在另一个页签提交报修/预约<br>4. 回到工作台首页观察 | "我的报修/我的预约"等卡片数字 **无需手动刷新浏览器**即可实时 +1（WebSocket 广播推送） |
| **A22** | **用户管理（管理员）** | 以 `admin` 登录：菜单：系统管理 → 用户管理 → 新增 / 编辑 / 禁用 / 重置密码 | 新用户可正常登录；禁用后无法登录；重置密码恢复为 `123456` |
| **A23** | **角色与菜单权限** | 1. 菜单：系统管理 → 角色管理<br>2. 编辑某角色的菜单权限<br>3. 用属于该角色的用户登录 | 角色与菜单多对多关联；用户登录后左侧菜单按角色动态渲染，未授权菜单不可见 |
| **A24** | **操作日志查看** | 菜单：系统管理 → 操作日志 | 可按操作人、模块、时间查询；登录、新增、删除等关键操作均有记录 |
| **A25** | **不同角色登录后数据权限差异验证** | 1. 用 `admin` 登录 → 看到所有实验室、所有预约、所有报修<br>2. 用 `labadmin` 登录 → 只能看到所属实验室的数据<br>3. 用 `teacher1` 登录 → 只能看到自己相关的预约和报修<br>4. 用 `student1` 登录 → 只能看到自己提交的数据 | `DataScopeUtil` 基于角色动态拼接 SQL 过滤条件，**不同角色看到的列表数据范围不同**；控制台可观察到拼接后的 WHERE 条件差异 |

---

## 7. 常见问题 FAQ

### Q1：前端启动后无法连接后端？

**现象**：登录页提交后提示"请求失败"或控制台 500 / 404。

**排查步骤**：
1. 确认后端已启动：浏览器访问 `http://localhost:9090/api/auth/login`（POST）应有响应。
2. 检查前端代理配置：`lab-frontend/vue.config.js`
   ```js
   proxy: { '/api': { target: 'http://localhost:9090', changeOrigin: true, pathRewrite: { '^/api': '/api' } } }
   ```
   target 需与后端端口一致（默认 `9090`）。
3. 查看浏览器 F12 → Network，确认请求确实被代理到 `9090`。

### Q2：数据库密码配置在哪里？

后端 `lab-backend/src/main/resources/application.yml` 的 `spring.datasource.password`。修改后需重启后端（`Ctrl+C` 后重新 `mvn spring-boot:run -DskipTests`）。

### Q3：预约提交后工作台数据没变化？

可能原因：
1. **前端未连接 WebSocket**：F12 → Console 查看是否有 `ws://localhost:9090/api/ws` 连接失败日志。
2. **统计缓存未刷新**：后端 `StatCacheInvalidator` 会在关键数据写入后清除缓存，确保下次读取重新统计。可以手动刷新一次页面看是否变化，若变化但 WebSocket 未推送，说明 WS 连接有问题（见 Q7）。
3. **定时任务未生效**：确认 `@EnableScheduling` 正常加载，见 Q4。

### Q4：定时任务（超时处理）在哪里？

- 类：`lab-backend/src/main/java/com/lab/module/reserve/service/ReservationTimeoutService.java`
- 核心注解：`@Scheduled(cron = "0 */5 * * * ?")`（每 5 分钟执行一次）
- 主启动类需存在 `@EnableScheduling`（`LabBackendApplication.java`）
- 日志中应能看到 `[ReservationTimeoutService]` 执行痕迹

### Q5：角色权限如何验证？

1. 登录后观察左侧侧边栏菜单数量变化：
   - admin：所有菜单
   - labadmin：实验室/设备/预约审核/耗材等
   - teacher1/student1：仅我的预约、我的报修等
2. 关键实现：
   - 前端路由守卫 `lab-frontend/src/router/router-guards.js` 与 `store/modules/permission.js`
   - 后端菜单查询 `SysMenuController.java` 根据角色返回菜单树
3. 直接构造未授权 API 请求，后端应返回 `403 Forbidden`（Spring Security + JWT 过滤器）

### Q6：密码忘记 / 重置怎么操作？

**场景 1：忘记 admin 密码** — 直接执行数据库重置：
```sql
USE lab_system;
UPDATE sys_user SET password = '$2a$10$7EqJtq98hPqEX7fNZaFWoO.5NkXa4PZ0YQjV5bC3e3R5c7m9q1a2' WHERE username = 'admin';
```
> 以上 BCrypt 哈希对应明文 `123456`，若需重新生成，可在 `AuthService` 中通过 `BCryptPasswordEncoder.encode("123456")` 打印一次。

**场景 2：其他账号** — admin 登录 → 系统管理 → 用户管理 → 对应用户点击"重置密码"按钮，密码恢复为 `123456`。

### Q7：WebSocket 连接失败怎么办？

检查点：
1. 后端端口：`application.yml` 中 `server.port=9090`，`context-path=/api`，WS 端点为 `ws://localhost:9090/api/ws`。
2. 前端 `lab-frontend/src/utils/websocket.js` 中连接 URL 是否指向正确地址（开发模式下会自动拼接当前域的 `/api/ws`，走代理）。
3. Spring Security 放行：`SecurityConfig.java` 中应对 `/ws/**` 放行。
4. 浏览器 F12 → Network → WS，看是否有 `Status 101 Switching Protocols` 成功握手；若一直 pending 或报错，说明被 Spring Security 拦截或后端未启动。

### Q8：导入 Excel 失败是什么原因？

常见原因：
1. **表头不一致**：必须使用系统提供的模板，不要擅自改列名。
2. **字段格式错误**：例如日期字段应符合 `yyyy-MM-dd`，数字字段不要带中文。
3. **必填字段缺失**：如实验室名称、编号等为必填。
4. **重复编号**：已存在编号的记录插入会失败（唯一约束）。
5. **POI 依赖**：后端 `pom.xml` 中应有 `poi` / `poi-ooxml` 依赖；若缺失会报 `ClassNotFoundException`。

**定位方式**：导入返回结果包含成功条数、失败条数、失败明细。

---

## 8. 课程考核答辩建议

> 本节用于指导答辩现场的讲解节奏（以 **5–7 分钟**为宜）。节奏要点：**先整体后细节、先演示再讲解、先功能再技术**。

### 8.1 推荐讲解顺序（5–7 分钟节奏）

1. **开场：项目定位 + 技术栈**（约 1 分钟）
   - 面向高校实验室：预约、设备、耗材、统计一体化管理
   - 技术栈：Spring Boot 2.7 + MyBatis + MySQL 8 + Vue 2 + Element UI + WebSocket + Spring Security + JWT

2. **架构图讲解：前后端分离 + DB**（约 1 分钟）
   - 浏览器 → 前端（Vue 2, 端口 8087）→ 后端（Spring Boot, 端口 9090）→ MySQL 8
   - 前端通过代理 `/api` 访问后端；WebSocket 用于实时统计看板刷新

3. **模块互通图 + ER 图讲解**（约 1 分钟）
   - 核心表：`sys_user`（用户）、`sys_role`（角色）、`lab_room`（实验室）、`lab_device`（设备）、`lab_device_repair`（报修）、`reservation`（预约）、`stock_item`（耗材）、`stock_record`（出入库）
   - 关联关系：用户 ↔ 角色（多对多）；实验室 ↔ 设备（一对多）；用户 ↔ 预约（多对多）；设备 ↔ 报修（一对多）

4. **核心功能演示**（约 2 分钟）
   - 以 `student1` 登录：提交一个预约 + 提交一个报修
   - 以 `labadmin` 登录：审核预约、处理报修
   - 以 `admin` 登录：进入工作台，观察统计卡片实时刷新

5. **角色权限演示**（约 1 分钟）
   - 分别用 admin / labadmin / teacher1 / student1 登录，展示：
     - 菜单数量不同
     - 列表中可见的数据范围不同（DataScope 数据隔离）

6. **技术亮点讲解**（约 1 分钟）
   - WebSocket 实时推送（StatPushService）
   - 基于角色的数据权限隔离（DataScopeUtil）
   - 5 分钟定时任务处理超时预约（@Scheduled）
   - JWT 无状态认证 + Spring Security 权限

7. **总结 + 源码导航**（约 30 秒）
   - 一句话总结："前后端分离、多角色数据隔离、实时看板的一体化实验室管理系统"
   - 快速打开关键文件（见第 9 节），展示核心代码行数定位

### 8.2 推荐现场操作清单

> 建议提前在本地准备好，答辩时按顺序快速演示，**每步 <30 秒**。

1. **演示 1：工作台数据看板**
   - `admin` 登录 → 进入首页工作台 → 查看统计卡片、图表

2. **演示 2：WebSocket 实时刷新（最能打动人的演示）**
   - `student1` 登录工作台 → 保持该页面打开
   - 新建浏览器页签（或开一个无痕窗口）→ `student1` 再登录 → 提交一个报修
   - 回到第一个页签的工作台 → **无需手动刷新**，观察"我的报修"卡片数字变化

3. **演示 3：预约审核流程**
   - `student1` 登录 → 预约中心 → 我要预约 → 选择实验室 + 时段 → 提交
   - `labadmin` 登录 → 预约审核 → 对该预约点击"通过"
   - 回到 `student1` → 我的预约 → 状态变为"已通过"
   - `student1` 点击"签到" → 再"签退"

4. **演示 4：库存预警联动**
   - `labadmin` 登录 → 统计分析 → 库存预警 → 截图观察预警项数量
   - `labadmin` → 库存台账 → 对预警耗材执行"入库"→ 数量 10
   - 回到库存预警页 → 预警数量减少或消失

5. **演示 5：角色权限差异**
   - `admin` 登录 → 左侧可见所有菜单（含系统管理）
   - `student1` 登录 → 左侧仅"我的预约""我的报修"等 2–3 个菜单项
   - `student1` 预约列表只有自己提交的数据 → 对比 admin 可见所有数据

### 8.3 技术深度讲解建议

> 答辩时可边打开代码边讲解，**每点 <1 分钟**。建议选择 3 个最能体现技术深度的点。

- **DataScopeUtil（数据权限隔离）**
  - 文件：`lab-backend/src/main/java/com/lab/security/DataScopeUtil.java`
  - 思路：根据当前登录用户的角色，在 MyBatis 查询时通过 `@DataScope` 注解 + SQL 拼接自动注入 `WHERE (user_id = ? OR dept_id IN (...))` 等过滤条件
  - 效果：student 只能看自己的数据，labadmin 看所属实验室的数据，admin 看全部

- **StatPushService + WebSocket（实时刷新）**
  - 文件：`lab-backend/src/main/java/com/lab/websocket/StatPushService.java`、`StatWebSocketHandler.java`
  - 思路：写操作（报修提交、审核通过、入库/出库等）执行后，调用 `StatPushService.push(userId / roleType)` → `StatWebSocketHandler` 广播 `{type:'stat-refresh'}` 消息 → 前端 `websocket.js` 收到消息后调用 `reloadDashboard()`
  - 优点：**避免前端轮询**，节省服务器资源；数据变化即时可见

- **ReservationTimeoutService（定时任务）**
  - 文件：`lab-backend/src/main/java/com/lab/module/reserve/service/ReservationTimeoutService.java`
  - 核心：`@Scheduled(cron = "0 */5 * * * ?")` 每 5 分钟扫描一次 `reservation` 表
  - 规则：状态为"已通过"且 `start_time < now - 5min` 且未签到的 → 标记为"已取消"，写入备注"超时未签到"

- **Excel 导入导出（Apache POI）**
  - 文件：`lab-backend/src/main/java/com/lab/common/ExcelImportUtil.java`、`ExcelExportUtil.java`
  - 思路：反射读取实体 `@ExcelColumn` 注解 → 自动映射列 → 写入/读取 Workbook
  - 校验：必填、唯一性、数据类型，返回"成功/失败明细"供前端展示

- **JWT + Spring Security（无状态认证）**
  - 文件：`lab-backend/src/main/java/com/lab/security/JwtService.java`、`JwtAuthFilter.java`、`SecurityConfig.java`
  - 登录流程：`AuthService.login()` 校验用户名密码 → 签发 JWT → 前端保存到 `localStorage`
  - 请求流程：前端 `request.js` 在 header 中带 `Authorization: Bearer xxx` → `JwtAuthFilter` 校验 → 有效则放入 `SecurityContextHolder`
  - 权限：`SecurityConfig` 中基于 URL 角色拦截；`@PreAuthorize("hasRole('ADMIN')")` 用于更细粒度的方法级控制

- **逻辑删除 vs 物理删除**
  - 所有业务表含 `deleted`（`0` 未删 / `1` 已删）字段
  - Mapper XML 中每个查询都会带 `AND deleted = 0`
  - 优点：保留历史轨迹，便于统计、审计与恢复；避免误删不可回溯

---

## 9. 项目文件导航（答辩时快速定位源码）

> 答辩时可将此节放在 PPT 最后一页，或直接在 VS Code / IDEA 中按以下路径打开。

| 文件 | 绝对路径 | 说明 |
|------|----------|------|
| **pom.xml** | `lab-backend/pom.xml` | Maven 依赖：Spring Boot、MyBatis、PageHelper、POI、Spring Security、JWT、WebSocket 等 |
| **application.yml** | `lab-backend/src/main/resources/application.yml` | 数据库连接、JWT 配置、WebSocket 地址、端口 `9090`、`context-path=/api` |
| **DataScopeUtil.java** | `lab-backend/src/main/java/com/lab/security/DataScopeUtil.java` | 基于角色的数据隔离，自动拼接 SQL 数据权限过滤 |
| **StatService.java** | `lab-backend/src/main/java/com/lab/module/stat/service/StatService.java` | 工作台与统计分析图表数据聚合服务 |
| **StatPushService.java** | `lab-backend/src/main/java/com/lab/websocket/StatPushService.java` | 触发 WebSocket 广播的入口；写操作后主动推送刷新信号 |
| **ReservationTimeoutService.java** | `lab-backend/src/main/java/com/lab/module/reserve/service/ReservationTimeoutService.java` | `@Scheduled(cron="0 */5 * * * ?")` 每 5 分钟处理超时未签到的预约 |
| **LabDeviceRepairService.java** | `lab-backend/src/main/java/com/lab/module/lab/service/LabDeviceRepairService.java` | 报修单 CRUD + 状态流转（待处理 → 处理中 → 已完成） |
| **dashboard/index.vue** | `lab-frontend/src/views/dashboard/index.vue` | 工作台首页：统计卡片 + 图表 + 实时刷新（WebSocket） |
| **websocket.js** | `lab-frontend/src/utils/websocket.js` | 前端 WebSocket 客户端：建连、心跳、断线重连、接收 `stat-refresh` 消息并刷新数据 |
| **lab_system.sql** | `sql/lab_system.sql` | 建表脚本：13 张表的结构定义（含索引、外键、默认值） |
| **demo_data.sql** | `sql/demo_data.sql` | 演示数据脚本：默认账号（admin/labadmin/teacher1/student1）、示例实验室、设备、耗材、预约等 |

---

> **祝你答辩顺利！** 🎓
> 如有其他问题，可结合 `docs/ARCHITECTURE.md`、`docs/MODULES.md`、`docs/API.md` 查阅系统设计与接口详情。

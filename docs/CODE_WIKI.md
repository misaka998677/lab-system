# 校园实验室预约与设备耗材管理系统 · Code Wiki

> 项目代号：**lab-system**
> Java EE 开发模块拓展实训课程项目。

---

## 1. 项目总览

### 1.1 项目定位

面向高校实验室的一体化管理平台，覆盖 **用户权限 → 实验室设备台账 → 预约审核签到 → 耗材出入库 → 数据统计** 全流程。采用前后端分离 + RBAC 权限模型的企业级架构。

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| 后端语言 | JDK 17 |
| 后端框架 | Spring Boot 3.2.x + Spring Security 6 |
| ORM / 分页 | MyBatis 3 + PageHelper |
| 工具库 | Hutool 5.8 + Lombok |
| 认证 | JWT（JJWT 0.11.5，HMAC SHA256） |
| 数据库 | MySQL 8.x（兼容 5.7，字符集 utf8mb4） |
| 前端框架 | Vue 2.6 + Vue Router 3 + Vuex 3 |
| UI | Element UI 2.15 |
| 图表 | ECharts 5.4 |
| HTTP | Axios |
| Cookie | js-cookie |
| 构建 | Maven 3.8 / Node 16–18 |
| 进度条 | NProgress |

### 1.3 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin`    | `123456` | 系统管理员 |
| `labadmin` | `123456` | 实验室管理员 |
| `teacher`  | `123456` | 教师 |
| `student`  | `123456` | 学生 |

> 密码统一使用 BCrypt 加密，密文：`$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G`

---

## 2. 目录结构

```
lab-system/
├── lab-backend/          # Spring Boot 后端
│   ├── src/main/java/com/lab/
│   │   ├── common/       # 统一返回、分页、异常、全局异常处理
│   │   ├── config/       # 跨域、操作日志 AOP
│   │   ├── security/     # JWT 服务、过滤器、Security 配置、用户详情
│   │   └── module/
│   │       ├── system/   # 用户/角色/菜单/部门/操作日志
│   │       ├── lab/      # 实验室/设备/维修
│   │       ├── reserve/  # 预约/审核/签到
│   │       ├── stock/    # 耗材/出入库
│   │       └── stat/     # 数据统计
│   └── src/main/resources/
│       ├── mapper/       # MyBatis XML 映射
│       └── application.yml
│
├── lab-frontend/         # Vue 2 前端
│   └── src/
│       ├── api/          # 按业务模块拆分的 API 封装
│       ├── utils/        # request、auth（JWT Cookie 读写）
│       ├── store/        # Vuex user / permission 模块
│       ├── router/       # 基础路由 + 动态路由注册
│       ├── layout/       # 后台主布局（侧栏 + 顶栏 + 路由视图）
│       ├── views/        # 业务页面
│       ├── assets/       # 全局 SCSS
│       ├── App.vue
│       ├── main.js
│       └── permission.js # 全局路由守卫
│
├── sql/
│   ├── lab_system.sql    # 基础结构与初始数据
│   └── demo_data.sql     # 答辩演示数据（可重复执行）
│
├── docs/                 # 课程设计文档 / 启动验收指南
└── README.md
```

---

## 3. 运行方式

### 3.1 数据库初始化

```sql
-- 1. 创建库
CREATE DATABASE lab_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_system;

-- 2. 导入结构与初始数据
SOURCE C:/Users/12980/Desktop/lab-system/sql/lab_system.sql;

-- 3.（可选）导入演示数据，让首页统计、耗材、维修等页面更饱满
SOURCE C:/Users/12980/Desktop/lab-system/sql/demo_data.sql;
```

### 3.2 后端启动

修改 `lab-backend/src/main/resources/application.yml` 中数据库用户名/密码后：

```bash
cd lab-backend
mvn clean spring-boot:run
# 默认监听 http://localhost:8080/api
```

### 3.3 前端启动

```bash
cd lab-frontend
npm install             # 首次安装依赖
npm run serve           # 启动开发服务器 http://localhost:8081
```

生产打包：`npm run build` → 输出 `dist/`；后端 `mvn clean package` → 出 `lab-backend.jar`。

### 3.4 健康检查

```bash
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"123456"}'
```

预期返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "<JWT Token>",
    "username": "admin",
    "realName": "超级管理员",
    "userId": 1
  }
}
```

---

## 4. 系统架构

### 4.1 总体架构图

```
┌──────────────────────────────────────────────────────────┐
│                      浏览器 (Vue 2)                       │
│   ┌──────────┐ ┌──────────┐ ┌────────────┐              │
│   │  Layout  │ │  Router  │ │  Vuex Store│              │
│   │ (后台布局)│ │(动态路由)│ │ (user/perm)│              │
│   └────┬─────┘ └────┬─────┘ └─────┬──────┘              │
│        └────────────┼─────────────┘                       │
│                     ▼                                     │
│          Axios (baseURL="/api")                          │
│                  │  (携带 JWT in Authorization)          │
└──────────────────┼────────────────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────────────┐
│                    Spring Boot 后端                       │
│                                                            │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Servlet Filter 链                                     │ │
│  │  ┌──────────────────┐                                 │ │
│  │  │ CorsFilter (内置)│                                 │ │
│  │  └──────────────────┘                                 │ │
│  │  ┌──────────────────┐   ┌──────────────────────────┐  │ │
│  │  │ JwtAuthFilter    │──▶│ UsernamePasswordAuth...  │  │ │
│  │  │  解析 Bearer JWT│   │  AuthenticationManager    │  │ │
│  │  └──────────────────┘   └──────────────────────────┘  │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │ SecurityFilterChain（授权 / 401 / 403 返回 JSON）│ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────┘ │
│                      ▼                                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ @RestController 层                                    │ │
│  │ /auth/*, /system/*, /lab/*, /reserve/*,               │ │
│  │ /stock/*, /stat/*                                     │ │
│  │  └─ @PreAuthorize("hasAuthority('xxx')")              │ │
│  └───────────────────────────────────────────────────────┘ │
│                      ▼                                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ @Service 业务层（含 @Transactional）                  │ │
│  │  如：AuthService · ReservationService ·               │ │
│  │  LabDeviceService · StockService · StatService        │ │
│  └───────────────────────────────────────────────────────┘ │
│                      ▼                                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ MyBatis Mapper 接口 + XML + PageHelper                │ │
│  └───────────────────────────────────────────────────────┘ │
│                      ▼                                      │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ AOP：OperationLogAspect（拦截 POST/PUT/DELETE 写操作）│ │
│  │ GlobalExceptionHandler（统一异常 → Result.fail）      │ │
│  └───────────────────────────────────────────────────────┘ │
│                      ▼                                      │
│              MySQL 8.x (lab_system)                         │
│                13 张业务表                                  │
└──────────────────────────────────────────────────────────┘
```

### 4.2 前后端调用时序（以登录为例）

```
浏览器                         后端 (Spring Boot)
  │                                │
  │  POST /api/auth/login          │
  │  { username, password }        │
  │───────────────────────────────▶│
  │                                │
  │  ──▶ AuthController            │
  │       └──▶ AuthService.login() │
  │            └──▶ AuthenticationManager.authenticate()
  │                 └──▶ UserDetailsServiceImpl.loadUserByUsername()
  │                      └──▶ SysUserMapper.findByUsername()
  │                ←───────────────────────────────┘
  │            认证通过 → JwtService.issue(userId, username)
  │            构造 Map { token, username, realName, userId }
  │  ◀───────────────────────────────
  │  Result<Map> + code=200
  │
  │  → utils/auth:setToken(token)
  │  → store/user 提交 mutations
  │  → router.push('/dashboard')
  │
  │  GET /api/auth/menus           │
  │  Header: Authorization: Bearer <token>
  │───────────────────────────────▶│
  │  JwtAuthFilter → parse JWT     │
  │  SecurityUtil.current() 得到用户
  │  AuthService.menus()：根据 role 过滤，构造树
  │  ◀───────────────────────────────
  │  store/permission.buildRoutes() 动态注册
```

### 4.3 模块互通关系

```
 sys_user  ←─ N:N ─→  sys_role  ←─ N:N ─→  sys_menu
     │                 │                    │
     ▼                 ▼                    ▼  (perm 标识控制按钮显隐 + @PreAuthorize)
┌────────────┐    ┌─────────────┐    ┌──────────────┐
│ 实验室档案  │    │ 设备台账    │    │ 维修单        │
│  lab_room  │───▶│ lab_device  │───▶│ lab_device_repair │
└─────┬──────┘    └──────┬──────┘    └───────────────────┘
      │ (1:N)             │ (status 联动：1在用→2维修→3报废)
      ▼                   ▼
┌────────────────────────────────────────────────────┐
│ 预约单 lab_reservation                               │
│  (lab_id + start_time + end_time) 联合冲突校验       │
│  status: 0待审核 1通过 2驳回 3签到 4签退 5取消        │
└───────┬─────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────────────────┐
│ 耗材出入库 stock_record                             │
│  reservation_id 字段预留，实现"预约→领用"联动       │
└────────────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────────────────┐
│ 统计报表 stat/overview · stat/lab-usage · etc.     │
│ （跨表聚合查询）                                     │
└────────────────────────────────────────────────────┘
```

---

## 5. 配置清单

### 5.1 后端 `application.yml`

```yaml
server:
  port: 8080
  servlet.context-path: /api

spring:
  application.name: lab-backend
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/lab_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.lab.module.**.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

pagehelper:
  helper-dialect: mysql
  reasonable: true
  support-methods-arguments: true
  params: count=countSql

lab:
  jwt:
    secret: bGFiLXN5c3RlbS1qd3Qtc2VjcmV0LWtleS1mb3ItaG1hYy1zaGEyNTYtMjAyNg==
    expire-minutes: 720              # 12 小时
    header: Authorization
    prefix: "Bearer "

logging:
  level:
    com.lab: debug
    org.springframework.security: info
```

### 5.2 前端关键配置

- `vue.config.js`：`port 8081`，对 `/api` 反向代理到 `http://localhost:8080`，生产环境禁用 sourceMap。
- `src/main.js`：注入 Element UI、全局样式、路由守卫脚本。
- `src/permission.js`：基于 token + `user` store 状态判断是否拉取 `auth/info` 与 `auth/menus`，未登录重定向到 `/login?redirect=...`。
- `src/api/*`：全部 API 用 `@/utils/request`（Axios 实例 + JWT 请求拦截 + 401 弹框 + 统一错误 toast）。
- Cookie：`lab_token`，过期时间 1 天（客户端视角，真正校验仍以 JWT `exp` 为准）。

---

## 6. 核心模块与 API

> 所有 API 响应体采用统一包装：`{ code, message, data }`。
> 需要登录的请求在 Header 携带 `Authorization: Bearer <token>`。
> 受权限控制的接口使用 `@PreAuthorize("hasAuthority('xxx:xxx:xxx')")`。

### 6.1 模块1：用户 / 角色 / 菜单 / 部门 / 日志

| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| POST | `/auth/login` | 账号密码登录 → 返回 JWT | 匿名 |
| GET  | `/auth/info`  | 获取当前登录用户与角色、权限字符串 | 登录 |
| GET  | `/auth/menus` | 获取当前用户可用菜单树（用于前端动态路由） | 登录 |
| POST | `/auth/logout`| JWT 无状态，仅触发前端清 cookie | 登录 |
| GET  | `/system/user/page` | 用户分页列表 | `system:user:list` |
| GET  | `/system/user/{id}` | 用户详情（含已关联角色） | `system:user:list` |
| POST | `/system/user` | 新增用户（密码由后端 BCrypt 加密） | `system:user:list` |
| PUT  | `/system/user` | 修改用户基本信息 | `system:user:list` |
| PUT  | `/system/user/{id}/status/{0|1}` | 启用/禁用 | `system:user:list` |
| PUT  | `/system/user/{id}/password?password=xxx` | 重置密码 | `system:user:list` |
| DELETE | `/system/user/{id}` | 删除用户（逻辑） | `system:user:list` |
| GET  | `/system/role/all` | 全部角色（下拉） | - |
| GET  | `/system/role/page` | 角色分页 | `system:role:list` |
| POST | `/system/role` / PUT / DELETE | CRUD | `system:role:list` |
| GET  | `/system/menu/tree` | 菜单树（权限分配用） | `system:menu:list` |
| POST | `/system/menu` / PUT / DELETE | CRUD | `system:menu:list` |
| GET  | `/system/dept/all` | 部门树 | - |
| POST | `/system/dept` / PUT / DELETE | CRUD | `system:dept:list` |
| GET  | `/system/log/page` | 操作日志分页 | `system:log:list` |
| DELETE | `/system/log/clear` | 清空日志 | `system:log:list` |

> **日志打点策略**：`OperationLogAspect` 以 AOP 环绕所有 `@RestController` 中的 `@PostMapping / @PutMapping / @DeleteMapping`，异步写入 `sys_log` 表。

### 6.2 模块2：实验室 / 设备 / 维修

| Method | Path | 说明 |
|--------|------|------|
| GET | `/lab/room/page` | 实验室分页 |
| GET | `/lab/room/all` | 全部实验室 |
| POST | `/lab/room` | 新增实验室 |
| PUT  | `/lab/room` | 修改 |
| DELETE | `/lab/room/{id}` | 删除 |
| GET  | `/lab/device/page` | 设备分页（keyword/labId/category/status 多条件） |
| GET  | `/lab/device/by-lab/{labId}` | 指定实验室设备列表 |
| POST | `/lab/device` | 新增设备 |
| PUT  | `/lab/device` | 修改 |
| DELETE | `/lab/device/{id}` | 删除 |
| PUT  | `/lab/device/{id}/status/{1|2|3}` | 1在用 2维修 3报废 |
| GET  | `/lab/repair/page` | 维修单分页 |
| POST | `/lab/repair/report` | 提交报修单 → 设备状态自动切换为"维修" |
| PUT  | `/lab/repair/{id}/handle?status=2&handlerId=2&note=xxx` | 处理（受理/完成/驳回）→ 状态=2 时把设备恢复为"在用" |
| DELETE | `/lab/repair/{id}` | 删除维修单 |

### 6.3 模块3：预约 / 审核 / 签到

| Method | Path | 说明 |
|--------|------|------|
| GET | `/reserve/page?keyword=&labId=&status=&start=&end=` | 全部预约分页（审核人员） |
| GET | `/reserve/mine?status=` | 我的预约分页 |
| GET | `/reserve/check-records` | 签到记录（按权限过滤） |
| GET | `/reserve/{id}` | 详情 |
| POST | `/reserve` | 提交申请（冲突校验 + 状态=0 待审核） |
| PUT  | `/reserve` | 修改（仅 0 状态可改，含冲突重校验） |
| PUT  | `/reserve/{id}/cancel` | 取消（签到后不可取消） |
| PUT  | `/reserve/{id}/audit?pass=true&note=...` | 审核（通过/驳回） |
| PUT  | `/reserve/{id}/check-in` | 签到（状态→3） |
| PUT  | `/reserve/{id}/check-out` | 签退（状态→4） |

**冲突校验 SQL（ReservationMapper）**：

```sql
SELECT COUNT(*) FROM lab_reservation
WHERE lab_id = #{labId}
  AND deleted = 0
  AND status <> 2                -- 已驳回不算
  AND id <> IFNULL(#{id}, 0)
  AND start_time < #{endTime}
  AND end_time   > #{startTime};
```

### 6.4 模块4：耗材出入库

| Method | Path | 说明 |
|--------|------|------|
| GET | `/stock/item/page` | 耗材档案分页 |
| GET | `/stock/item/warnings` | 低于 `warn_qty` 的耗材预警清单 |
| POST | `/stock/item` | 新增耗材档案 |
| PUT  | `/stock/item` | 修改 |
| DELETE | `/stock/item/{id}` | 删除 |
| GET  | `/stock/record/page` | 出入库流水分页 |
| POST | `/stock/record/in` | 入库（`stock_item.qty` 原子 +） |
| POST | `/stock/record/out`| 出库（`stock_item.qty` 原子 −，不足时报错） |

> 字段 `stock_record.reservation_id` 预留，供后续"预约→领用"联动。

### 6.5 模块5：数据统计

| Method | Path | 说明 |
|--------|------|------|
| GET  | `/stat/overview` | 首页卡片：用户数、实验室数、设备数、预约数、耗材预警数 |
| GET  | `/stat/lab-usage` | 各实验室按周/月使用时长（给 ECharts 柱状图） |
| GET  | `/stat/device-fault` | 设备故障报修统计 |
| GET  | `/stat/stock-warning` | 耗材预警明细 |

> 所有统计接口在前端通过 `/api/stat/*` 访问，返回聚合数据用于 ECharts 可视化。

---

## 7. 数据库表结构

共 **13 张表**，按模块划分。

### 7.1 权限域（5 张）

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `sys_user` | 用户 | id / username(UK) / password / real_name / dept_id / status / deleted |
| `sys_role` | 角色 | id / role_code(UK) / role_name / status |
| `sys_menu` | 菜单 | id / parent_id / name / path / component / icon / perm / type(1-3) |
| `sys_user_role` | 用户-角色 | (user_id, role_id) 联合主键 |
| `sys_role_menu` | 角色-菜单 | (role_id, menu_id) 联合主键 |
| `sys_dept` | 部门 | id / parent_id / name |
| `sys_log` | 操作日志 | id / user_id / module / action / method / params / ip / cost_ms / status / error_msg |

> 实际物理表数为 7 张（权限域中多加了 `sys_dept` 与 `sys_log`）。

### 7.2 实验室域（3 张）

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `lab_room` | 实验室 | id / code(UK) / name / building / capacity / dept_id / manager_id / status |
| `lab_device` | 设备 | id / asset_no(UK) / name / category / lab_id / price / status(1-3) |
| `lab_device_repair` | 维修单 | id / device_id / reporter_id / fault_desc / handler_id / status / report_time / finish_time |

### 7.3 预约域（1 张）

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `lab_reservation` | 预约单 | id / reserve_no(UK) / user_id / lab_id / start_time / end_time / status(0-5) / audit_user_id / check_in_time / check_out_time |

> 唯一索引 `uk_reserve_no`；组合索引 `idx_res_lab_time (lab_id, start_time, end_time)` 用于冲突校验加速。

### 7.4 耗材域（2 张）

| 表名 | 用途 | 关键列 |
|------|------|--------|
| `stock_item` | 耗材档案 | id / code(UK) / name / qty / warn_qty / lab_id |
| `stock_record` | 出入库流水 | id / item_id / type(1-3) / qty / reservation_id / user_id |

---

## 8. 关键类与函数

### 8.1 公共层 `com.lab.common`

- [Result](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/common/Result.java)：统一返回结构，静态工厂 `ok(data)`、`fail(code, msg)`。
- [PageResult](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/common/PageResult.java)：分页包装，字段 `{ total, rows, pageNum, pageSize }`。
- [BizException](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/common/BizException.java)：业务异常，含自定义 `code`。
- [GlobalExceptionHandler](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/common/GlobalExceptionHandler.java)：`@RestControllerAdvice` 统一捕获 `BizException / AccessDeniedException / AuthenticationException / 参数校验异常 / 其他异常`，全部返回 JSON。

### 8.2 AOP / 配置

- [OperationLogAspect](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/config/OperationLogAspect.java)：环绕所有写操作（POST/PUT/DELETE），记录用户、方法名、模块、参数、IP、耗时，异步写入 `sys_log`。失败场景也会被 `catch` 住，记录 error_msg 后继续抛出。
- `CorsConfig`：基于 Spring MVC 的全局跨域放开（和 Security 放行 `/auth/login` 配合）。

### 8.3 安全层 `com.lab.security`

- [SecurityConfig](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/security/SecurityConfig.java)：
  - 注册 `PasswordEncoder = BCryptPasswordEncoder`
  - `SessionCreationPolicy.STATELESS`（无 Session）
  - 放行 `/auth/login`、`/error`、`/favicon.ico`
  - 定制 `401` 与 `403` 以 JSON 返回，避免浏览器 Basic-Auth 弹窗
  - `@EnableMethodSecurity(prePostEnabled=true)` 开启 `@PreAuthorize` 方法级权限
- [JwtService](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/security/JwtService.java)：
  - `issue(userId, username)`：构造 payload（`sub`、`username`），使用 `lab.jwt.secret`（base64 解码后 ≥ 32 字节）做 HMAC-SHA256 签名，有效期由 `expire-minutes` 控制。
  - `parse(token)`：校验并返回 Claims；过期/签名错误抛 `JwtException`，由 Filter 捕获。
- [JwtAuthFilter](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/security/JwtAuthFilter.java)：读取 `Authorization` 头 → 去掉 `Bearer ` → 解析 JWT → 从 `UserDetailsServiceImpl` 加载最新权限 → 注入 `UsernamePasswordAuthenticationToken`。
- [SecurityUtil](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/security/SecurityUtil.java)：`current()`、`currentUserId()` 从 `SecurityContextHolder` 获取 `LoginUser`。
- `LoginUser`：实现 `UserDetails`，持有 `SysUser` + 角色列表 + 权限字符串列表。
- `UserDetailsServiceImpl`：`loadUserByUsername(username)` → 从 `sys_user` 查询用户 + 关联角色 + 关联菜单 → 构造 `LoginUser`。

### 8.4 系统模块 `com.lab.module.system`

- [AuthService](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/module/system/service/AuthService.java)：
  - `login(username, password)`：调用 `AuthenticationManager.authenticate()`，失败抛 `BadCredentialsException → BizException(401, ...)`；成功后 issue JWT。
  - `info()`：返回 `{ user, roles[], perms[] }`，前端据此渲染/隐藏按钮。
  - `menus()`：从当前用户 `role_id` 递归查询 `sys_menu`，在服务层 `buildTree()` 构造成树结构返回。

### 8.5 预约模块 `com.lab.module.reserve`

- [ReservationService](file:///c:/Users/12980/Desktop/lab-system/lab-backend/src/main/java/com/lab/module/reserve/service/ReservationService.java)：
  - `apply(r)`：校验时间区间 + 冲突计数 > 0 → 抛 `BizException`；否则设置 `status=0`，生成 `reserve_no = R + yyyyMMddHHmmss + (snowflakeId % 1000)`。
  - `update(r)`：仅当状态为 0 时可修改，修改前重新走冲突计数。
  - `audit(id, pass, note)`：状态由 0→1 或 2；写入 `audit_user_id / audit_time / audit_note`。
  - `checkIn / checkOut`：状态机 0→1→3→4。

### 8.6 设备维修联动 `com.lab.module.lab`

- `LabDeviceRepairService.report()`：插入维修单；同时 `UPDATE lab_device SET status=2 WHERE id=device_id`。
- `LabDeviceRepairService.handle(..., status=2)`：将维修单标记为"已完成"；同时把设备状态恢复为 `1（在用）`，记录 finish_time。

---

## 9. 前端架构要点

### 9.1 代码组织

```
src/
├── api/                   # API 封装（按后端业务模块一一对应）
│   ├── auth.js            # 登录/info/menus
│   ├── system.js          # 用户/角色/菜单/部门/日志
│   ├── lab.js             # 实验室/设备/维修
│   ├── reserve.js         # 我的预约/审核/签到
│   ├── stock.js           # 耗材档案/出入库
│   └── stat.js            # 首页/使用率/故障/库存预警
├── utils/
│   ├── request.js         # Axios 实例 + JWT 注入 + 响应拦截
│   └── auth.js            # getToken/setToken/removeToken (js-cookie)
├── store/
│   ├── index.js           # Vuex store 构造
│   └── modules/
│       ├── user.js        # mutations: SET_TOKEN/USER/ROLES/PERMS
│       └── permission.js  # buildRoutes() + router.addRoutes()
├── router/
│   └── index.js           # 静态路由：/login、/→dashboard；动态路由在 permission.js 注册
├── layout/
│   ├── index.vue          # 侧栏菜单（从 routes 生成）+ 顶栏 + <router-view>
│   └── components/
│       └── SidebarItem.vue
└── views/                 # 业务页面（每个页面对应一个 sys_menu.component）
    ├── login/
    ├── dashboard/
    ├── system/{user,role,menu,dept,log}/index.vue
    ├── lab/{room,device,repair}/index.vue
    ├── reserve/{mine,audit,check}/index.vue
    ├── stock/{item,record}/index.vue
    └── stat/{usage,fault}/index.vue
```

### 9.2 请求拦截器（utils/request.js）

- **request 拦截**：`cfg.headers['Authorization'] = 'Bearer ' + getToken()`
- **response 拦截**：`code === 200` 返回 `res.data`；否则 `Message.error()`。
- **error 处理**：`status === 401` → 清 token，MessageBox 弹"登录已过期"，`router.replace('/login')`；其他状态直接 toast。

### 9.3 动态路由

核心流程：

1. `permission.js` 检测到 `user.user` 为空 → 触发 `store.dispatch('user/fetchInfo')`。
2. 拉取 `/auth/info`，写入 `user/roles/perms`。
3. 触发 `store.dispatch('permission/generateRoutes')` → 拉 `/auth/menus`。
4. `buildRoutes()` 按 `parent_id` 构造嵌套路由；顶级菜单使用 `Layout`，子菜单使用 `component` 字段映射成 `() => import('@/views/${component}.vue')`。
5. `router.addRoutes(routes)` 注册；然后 `next({ ...to, replace: true })` 重新进入目标路由。

### 9.4 按钮级权限

前端使用 `store.state.user.perms`（如 `"lab:device:list"`）+ `v-if` 控制按钮显隐；后端 `@PreAuthorize("hasAuthority('...')")` 做最终校验（403 由 `GlobalExceptionHandler` 返回 JSON）。

---

## 10. 依赖关系

### 10.1 后端关键依赖

| GroupId | ArtifactId | 版本 | 用途 |
|---------|------------|------|------|
| `org.springframework.boot` | `spring-boot-starter-web` | 3.2.x | Web 容器、REST 支持 |
| `org.springframework.boot` | `spring-boot-starter-security` | 3.2.x | 认证/授权 |
| `org.springframework.boot` | `spring-boot-starter-validation` | 3.2.x | JSR-380 参数校验 |
| `org.springframework.boot` | `spring-boot-starter-aop` | 3.2.x | 操作日志切面 |
| `org.mybatis.spring.boot` | `mybatis-spring-boot-starter` | 3.0.3 | MyBatis 整合 |
| `com.github.pagehelper` | `pagehelper-spring-boot-starter` | 2.1.0 | 物理分页 |
| `com.mysql` | `mysql-connector-j` | - | MySQL 驱动 |
| `cn.hutool` | `hutool-all` | 5.8.27 | 字符串工具/集合工具 |
| `io.jsonwebtoken` | `jjwt-api / jjwt-impl / jjwt-jackson` | 0.11.5 | JWT 签发/校验 |
| `org.projectlombok` | `lombok` | provided | 消除样板代码 |

### 10.2 前端关键依赖

| 包名 | 版本 | 用途 |
|------|------|------|
| `vue` | 2.6.x | 主框架 |
| `vue-router` | 3.6.x | SPA 路由（含 addRoutes） |
| `vuex` | 3.6.x | 全局状态 |
| `element-ui` | 2.15.x | UI 组件库 |
| `axios` | 0.27.x | HTTP 客户端 |
| `echarts` | 5.4.x | 图表 |
| `js-cookie` | 3.x | JWT 持久化 |
| `nprogress` | 0.2.x | 顶部加载条 |

### 10.3 内部模块依赖关系

```
Controller   → Service   → Mapper   → MySQL
                    ↑
                    └── (OperationLogAspect 切面)
                    └── (SecurityUtil.current() 读取上下文)

 frontend api → utils/request → Spring MVC Controller
                      ↑
                      └── Cookie: lab_token (getToken / setToken / removeToken)
```

模块间依赖方向：`system ← lab/reserve/stock/stat`（用户权限被所有模块读取），其他业务模块相互独立，耦合点仅限 FK（如 `lab_reservation.lab_id → lab_room.id`）。

---

## 11. 关键业务约束总结

- **JWT**：payload 含 `sub=userId` 与 `username`，exp 默认 720 分钟；secret 至少 32 字节。
- **密码**：统一使用 BCryptPasswordEncoder，`password` 字段以 `$2a$10$` 开头的哈希存储。
- **状态码**：预约 `0~5`；设备 `1~3`；维修单 `0~3`；用户 `0/1`；统一与前端枚举同步。
- **冲突校验**：`lab_id + (start, end)` 时间区间重叠，同时排除被驳回的预约。
- **AOP 日志**：仅记录 POST/PUT/DELETE，`sys_log.params` 字段截断到 1000 字符，`error_msg` 截断到 480 字符。
- **RBAC**：角色→菜单的 `perm` 字段决定按钮可见性与 `@PreAuthorize` 权限；菜单 `component` 决定前端路由的懒加载路径。

---

## 12. 常见问题排查

| 现象 | 原因 | 解决 |
|------|------|------|
| 登录返回 401，密码正确但提示错误 | 数据库中 `password` 字段密文被改写为明文 | 以 `$2a$10$rmo6sNdR3...` 重新写入 |
| 前端 `/api` 请求 404 / 502 | 前端代理未生效或后端未启动 | 确保后端 `8080` 已监听；`vue.config.js` 中 `pathRewrite` 保持 `/api` |
| 所有接口返回 401 | JWT 未携带或已过期 | 检查 `request.js` 请求头；或延长 `lab.jwt.expire-minutes` |
| 分页查询未生效 | `PageHelper.startPage()` 必须紧跟第一条 select | 不要在中间做其他查询；`PageInfo` 构造必须紧跟 |
| MyBatis 日志在控制台刷太多 | 把 `mybatis.configuration.log-impl` 改为 `org.apache.ibatis.logging.nologging.NoLoggingImpl` 或降低 `logging.level.com.lab` |
| 数据库连不上 | MySQL 8.x 默认 caching_sha2_password；或字符集非 utf8mb4 | 使用命令行 `ALTER USER ... IDENTIFIED WITH mysql_native_password BY '...'` 或升级连接器 |

---

## 13. 下一步可拓展方向

- **移动端小程序**：将前端 `lab-frontend` 改造为 uni-app，复用 `/api/*` 接口与 JWT 方案即可快速获得微信小程序版。
- **消息通知**：在 `audit / check-in` 等状态变化节点接入邮件/企业微信推送。
- **统计报表增强**：`stat/*` 当前以简单 SQL 聚合为主，可引入动态时间粒度（周/月/学期），新增按学院/按课程维度。
- **预约单 & 耗材联动**：目前已在 `stock_record.reservation_id` 预留字段，可新增"审核通过时自动创建耗材领用单"的业务规则。
- **操作日志可视化**：当前仅提供分页列表，可增加按模块/按用户/按时间范围的饼图与柱状图。

---

> **文档生成时间**：2026-06-17
> **生成依据**：`lab-backend/src/**` + `lab-frontend/src/**` + `sql/lab_system.sql`

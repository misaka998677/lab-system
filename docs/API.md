# API 接口清单

> 实验室资源预约管理系统 · 课程考核材料
> 文档版本：v1.0 · 更新日期：2026-06-20

---

## 1. 接口规范总览

### 1.1 统一前缀

| 项 | 说明 |
|----|------|
| 接口统一前缀 | `/api` |
| 数据传输格式 | `application/json` |
| 字符编码 | `UTF-8` |

### 1.2 认证方式

- **JWT Token**：所有接口（登录/注册除外）都需要在请求头中携带 Token。
- **请求头**：
  ```
  Authorization: Bearer <token>
  ```
- **Token 来源**：调用 `/api/auth/login` 成功后，响应 `data.token` 字段中获取。
- **Token 有效期**：默认 24 小时，过期后需重新登录。

### 1.3 统一响应格式 Result\<T\>

所有接口返回统一结构 `Result<T>`（见 `lab-backend/src/main/java/com/lab/common/Result.java`）：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 响应码，`200` 表示成功，其他表示失败 |
| `message` | String | 响应消息，成功时为 `success` |
| `data` | T | 业务数据，可为对象、数组或 null |

### 1.4 分页响应格式 PageResult\<T\>

分页列表接口使用 `PageResult<T>`（见 `lab-backend/src/main/java/com/lab/common/PageResult.java`）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 128,
    "records": [ { ... }, { ... } ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | long | 总记录数 |
| `records` | List\<T\> | 当前页数据列表 |

> 分页查询参数（Query String）：`pageNum`（页码，默认 1）、`pageSize`（每页条数，默认 10）。

### 1.5 错误码说明

系统通过 `BizException` + `ErrorCode` 枚举统一管理业务错误（见 `lab-backend/src/main/java/com/lab/common/ErrorCode.java`）。核心错误码如下：

| 错误码 | HTTP 状态 | 说明 | 常见触发场景 |
|--------|-----------|------|-------------|
| **200** | 200 OK | 操作成功 | 正常响应 |
| **1000** | 400 | 参数校验失败 | 请求字段不符合约束 |
| **1001** | 400 | 参数无效 | 传入的 ID 格式错误等 |
| **1002** | 400 | 缺少必要参数 | 必填字段为空 |
| **2001** | 401 | 未登录或登录已过期 | Token 缺失或已过期 |
| **2003** | 403 | 无访问权限 | 角色/菜单权限不足 |
| **2004** | 401 | Token 无效或已过期 | Token 被篡改/过期 |
| **2005** | 403 | 账号已禁用 | 用户 `status=0` 仍尝试登录 |
| **401** | 400 | 账号或密码错误 | 登录校验失败 |
| **402** | 400 | 用户名已存在 | 注册/新增用户冲突 |
| **410** | 404 | 实验室不存在 | 实验室 ID 未找到 |
| **411** | 403 | 无权限管理该实验室 | 非实验室管理员操作 |
| **412** | 400 | 预约人数超过实验室容量 | capacity 校验失败 |
| **420** | 404 | 设备不存在 | 设备 ID 未找到 |
| **430** | 404 | 预约记录不存在 | 预约 ID 未找到 |
| **431** | 403 | 无权操作他人的预约 | 普通用户修改他人预约 |
| **432** | 400 | 预约时间冲突 | 同一时间段已被预约 |
| **433** | 400 | 预约时间不合法 | 结束时间早于开始时间等 |
| **434** | 400 | 预约状态不是待审核 | 重复审核/非待审核状态 |
| **435** | 400 | 预约未通过审核 | 未审核通过即签到 |
| **436** | 400 | 已签到，无法取消 | 签到后尝试取消 |
| **440** | 404 | 耗材不存在 | 耗材 ID 未找到 |
| **441** | 400 | 库存不足 | 出库数量大于当前库存 |
| **450** | 403 | 账号已锁定 | 连续登录失败过多 |
| **5000** | 500 | 服务器内部错误 | 未捕获异常 |
| **5002** | 500 | 数据库操作失败 | MyBatis 执行异常 |

---

## 2. 接口分组清单

### 表 2.1 认证接口 AuthController（POST /api/auth/*）

| 路径 | 方法 | 功能 | 鉴权 | 说明 |
|------|------|------|------|------|
| `/api/auth/login` | POST | 账号登录 | 否 | 登录成功返回 JWT token + 用户信息；失败次数过多触发账号锁定 |
| `/api/auth/register` | POST | 学生/教师注册 | 否 | 新用户注册，默认 `status=0`（教师需管理员审核，学生自动激活） |
| `/api/auth/logout` | POST | 退出登录 | 是 | 后端清理 Token 缓存，前端清除本地 token |
| `/api/auth/info` | GET | 当前登录用户信息 | 是 | 返回用户详情 + 角色集合 + 菜单树（前端用于路由与鉴权） |

---

### 表 2.2 用户管理 SysUserController（/api/user/*）

| 路径 | 方法 | 功能 | 鉴权 | 备注 |
|------|------|------|------|------|
| `/api/user/list` | GET | 用户列表 + 条件查询 | 是 | 支持按 `username`、`deptId`、`status` 过滤；返回 `PageResult` 分页 |
| `/api/user/{id}` | GET | 用户详情 | 是 | 返回单条用户记录（含角色 ID 列表） |
| `/api/user` | POST | 新增用户 | 是 | 创建用户 + 分配角色；需 `ROLE_ADMIN` |
| `/api/user` | PUT | 更新用户 | 是 | 修改用户信息 + 角色；需 `ROLE_ADMIN` |
| `/api/user/{id}` | DELETE | 删除用户 | 是 | 逻辑删除 `deleted=1` |
| `/api/user/batch` | DELETE | 批量删除用户 | 是 | 支持多 id 批量（`ids` 参数） |
| `/api/user/reset/{id}` | PUT | 重置密码 | 是 | 将密码重置为 `BCrypt('123456')` |
| `/api/user/export` | GET | 导出用户 Excel | 是 | 调用 `ExcelExportUtil` 生成 `.xlsx` 下载 |

---

### 表 2.3 角色管理 SysRoleController（/api/role/*）

| 路径 | 方法 | 功能 | 鉴权 | 备注 |
|------|------|------|------|------|
| `/api/role/list` | GET | 角色列表 | 是 | 全部角色（含条件过滤，如 `roleName`）；分页 |
| `/api/role/all` | GET | 所有角色 | 是 | 无分页，用于分配角色下拉框 |
| `/api/role/{id}` | GET | 角色详情 | 是 | 返回角色信息 + 已分配菜单 ID 列表 |
| `/api/role` | POST | 新增角色 + 分配菜单 | 是 | 含 `sys_role_menu` 中间表写入 |
| `/api/role` | PUT | 更新角色 | 是 | 同时更新角色信息与菜单关联 |
| `/api/role/{id}` | DELETE | 删除角色 | 是 | 删除角色及其菜单关联（需无用户使用） |
| `/api/role/export` | GET | 导出角色 | 是 | 导出为 Excel |

---

### 表 2.4 菜单管理 SysMenuController（/api/menu/*）

| 路径 | 方法 | 功能 | 鉴权 |
|------|------|------|------|
| `/api/menu/tree` | GET | 菜单树 | 是 | 返回完整树形结构（用于侧边栏 + 角色分配） |
| `/api/menu` | GET/POST/PUT/DELETE | 菜单增删改查 | 是 | 完整 CRUD，仅管理员可操作 |

---

### 表 2.5 部门管理 SysDeptController（/api/dept/*）

| 路径 | 方法 | 功能 | 鉴权 |
|------|------|------|------|
| `/api/dept/tree` | GET | 部门树 | 是 | 返回部门层级树形结构 |
| `/api/dept` | GET/POST/PUT/DELETE | 部门增删改查 | 是 | 完整 CRUD；删除前检查是否含下级/用户 |

---

### 表 2.6 实验室管理 LabRoomController（/api/lab/room/*）

| 路径 | 方法 | 功能 | 鉴权 | 说明 |
|------|------|------|------|------|
| `/api/lab/room/list` | GET | 实验室列表 + 条件查询 | 是 | 按名称、位置、管理员、状态过滤；分页 |
| `/api/lab/room/all` | GET | 全部实验室 | 是 | 无分页，用于预约下拉选择 |
| `/api/lab/room/{id}` | GET | 实验室详情 | 是 | 返回实验室信息 + 已关联设备简要列表 |
| `/api/lab/room` | POST | 新增实验室 | 是 | 创建后自动为管理员用户分配 `ROLE_LABADMIN` |
| `/api/lab/room` | PUT | 更新实验室 | 是 | 修改基础信息 + 管理员 |
| `/api/lab/room/{id}` | DELETE | 删除实验室 | 是 | 逻辑删除；删除前检查是否有关联预约/设备 |
| `/api/lab/room/batch` | DELETE | 批量删除 | 是 | 支持多 id 批量 |
| `/api/lab/room/export` | GET | 导出实验室 Excel | 是 | 导出为 `.xlsx` |
| `/api/lab/room/import` | POST | 批量导入实验室 | 是 | 上传 Excel 文件解析插入（`ExcelImportUtil`） |

---

### 表 2.7 设备管理 LabDeviceController（/api/lab/device/*）

| 路径 | 方法 | 功能 | 鉴权 | 说明 |
|------|------|------|------|------|
| `/api/lab/device/list` | GET | 设备列表 + 条件查询 | 是 | 按设备名称、实验室、状态过滤；分页 |
| `/api/lab/device/{id}` | GET | 设备详情 | 是 | 返回设备完整信息 |
| `/api/lab/device` | POST | 新增设备 | 是 | 关联到指定实验室 |
| `/api/lab/device` | PUT | 更新设备 | 是 | 修改设备名称、型号、状态等 |
| `/api/lab/device/{id}` | DELETE | 删除设备 | 是 | 逻辑删除；检查是否有维修记录关联 |
| `/api/lab/device/batch` | DELETE | 批量删除设备 | 是 | 支持多 id 批量 |
| `/api/lab/device/export` | GET | 导出设备 Excel | 是 | 导出为 `.xlsx` |

---

### 表 2.8 设备报修管理 LabDeviceRepairController（/api/lab/repair/*）

| 路径 | 方法 | 功能 | 鉴权 | 说明 |
|------|------|------|------|------|
| `/api/lab/repair/list` | GET | 维修工单列表 + 条件查询 | 是 | 支持按设备、实验室、状态过滤；分页 |
| `/api/lab/repair/mine` | GET | 我的报修记录 | 是 | 只返回 `reporter_id = 当前用户` |
| `/api/lab/repair/{id}` | GET | 维修详情 | 是 | 返回维修记录 + 设备/实验室信息 |
| `/api/lab/repair` | POST | 提交报修 | 是 | 自动检查设备状态 + 阻止同一设备重复未完结报修 |
| `/api/lab/repair` | PUT | 更新维修记录 | 是 | 含状态流转（0→1→2） |
| `/api/lab/repair/{id}` | DELETE | 删除维修记录 | 是 | 仅管理员或报修人可删除 |
| `/api/lab/repair/export` | GET | 导出维修记录 | 是 | 导出为 Excel |

---

### 表 2.9 预约管理 ReservationController（/api/reservation/*）

| 路径 | 方法 | 功能 | 鉴权 | 说明 |
|------|------|------|------|------|
| `/api/reservation/list` | GET | 预约列表 + 条件查询 | 是 | 按实验室、用户、状态、时间段过滤；分页 |
| `/api/reservation/mine` | GET | 我的预约 | 是 | 只返回 `user_id = 当前用户` |
| `/api/reservation/audit` | GET | 待审核列表 | 是 | 需实验室管理员；返回自己管理的实验室的待审核预约 |
| `/api/reservation/{id}` | GET | 预约详情 | 是 | 返回完整预约信息 |
| `/api/reservation` | POST | 提交预约申请 | 是 | 自动检查时间冲突 + 实验室容量校验；初始状态 `0=待审核` |
| `/api/reservation` | PUT | 更新预约 | 是 | 修改预约信息（需本人或管理员） |
| `/api/reservation/{id}/approve` | PUT | 审核通过 | 是 | 需实验室管理员；状态 `0 → 1`（已通过） |
| `/api/reservation/{id}/reject` | PUT | 审核拒绝 | 是 | 状态 `0 → 2`（已拒绝） |
| `/api/reservation/{id}/check-in` | PUT | 签到 | 是 | 状态 `1 → 3`；写入 `check_in_time` |
| `/api/reservation/{id}/check-out` | PUT | 签退 | 是 | 状态 `3 → 4`；写入 `check_out_time` |
| `/api/reservation/{id}/cancel` | PUT | 取消预约 | 是 | 状态 → `5`（已取消）；签到后不可取消 |
| `/api/reservation/{id}` | DELETE | 删除预约 | 是 | 逻辑删除 `deleted=1` |
| `/api/reservation/export` | GET | 导出预约 Excel | 是 | 导出为 `.xlsx` |

**预约状态流转图**：

```
0(待审核) ── approve ──→ 1(已通过) ── check-in ──→ 3(使用中) ── check-out ──→ 4(已完成)
   │                          │
   └── reject ──→ 2(已拒绝)   └── cancel ──→ 5(已取消)
```

---

### 表 2.10 耗材管理 StockController（/api/stock/*）

| 路径 | 方法 | 功能 | 鉴权 | 说明 |
|------|------|------|------|------|
| `/api/stock/item/list` | GET | 耗材档案列表 + 条件查询 | 是 | 按名称、实验室、是否预警过滤；分页 |
| `/api/stock/item/{id}` | GET | 耗材详情 | 是 | 返回单条耗材信息 |
| `/api/stock/item` | POST | 新增耗材 | 是 | 录入耗材档案（含 `warning_threshold` 阈值） |
| `/api/stock/item` | PUT | 更新耗材 | 是 | 修改耗材基本信息、阈值 |
| `/api/stock/item/{id}` | DELETE | 删除耗材 | 是 | 逻辑删除；检查是否有出入库记录关联 |
| `/api/stock/item/export` | GET | 导出耗材 | 是 | 导出为 Excel |
| `/api/stock/record/list` | GET | 出入库记录列表 | 是 | 分页 + 条件过滤（按耗材、类型、时间范围） |
| `/api/stock/record/import` | POST | 入库登记 | 是 | `stock_item.quantity` 累加 + 写 `stock_record` 记录 |
| `/api/stock/record/export` | POST | 出库登记 | 是 | 写 `stock_record`；校验 `quantity >= 出库数量`，不足返回 `441 STOCK_INSUFFICIENT` |
| `/api/stock/warnings` | GET | 库存预警清单 | 是 | 仅返回 `quantity < warning_threshold` 的耗材 |

---

### 表 2.11 统计 StatController（/api/stat/*）

| 路径 | 方法 | 功能 | 鉴权 | 说明 |
|------|------|------|------|------|
| `/api/stat/overview` | GET | 工作台总览数据 | 是 | 返回统计卡片 + 最近预约 + 最近维修 + 预警列表；按角色 `dataScope` 过滤（`StatService.overview()`） |
| `/api/stat/usage` | GET | 实验室使用率统计 | 是 | 按实验室分组统计预约数量，用于 ECharts 柱状图/饼图 |
| `/api/stat/fault` | GET | 设备故障分布 | 是 | 按实验室/设备统计维修记录数量 |
| `/api/stock-warning` | GET | 库存预警详情 | 是 | 同 `/api/stock/warnings`，由 `StatService` 统一聚合，面向工作台展示 |

---

### 表 2.12 操作日志 SysLogController（/api/log/*）

| 路径 | 方法 | 功能 | 鉴权 | 备注 |
|------|------|------|------|------|
| `/api/log/list` | GET | 操作日志列表 + 条件查询 | 是 | 按用户、模块、操作类型过滤；分页；由 `OperationLogAspect` 自动写入 |
| `/api/log/export` | GET | 导出操作日志 | 是 | 导出为 Excel |

---

## 3. 接口总览统计

### 3.1 Controller 数量汇总

| 序号 | Controller 类 | 模块 | 接口数 |
|------|---------------|------|--------|
| 1 | `AuthController` | 认证模块 | 4 |
| 2 | `SysUserController` | 用户管理 | 8 |
| 3 | `SysRoleController` | 角色管理 | 7 |
| 4 | `SysMenuController` | 菜单管理 | 5 |
| 5 | `SysDeptController` | 部门管理 | 5 |
| 6 | `LabRoomController` | 实验室管理 | 9 |
| 7 | `LabDeviceController` | 设备管理 | 7 |
| 8 | `LabDeviceRepairController` | 设备报修 | 7 |
| 9 | `ReservationController` | 预约管理 | 13 |
| 10 | `StockController` | 耗材管理 | 10 |
| 11 | `StatController` | 统计模块 | 4 |
| 12 | `SysLogController` | 操作日志 | 2 |
| **合计** | —— | **12 个 Controller** | **约 81 个接口** |

### 3.2 按功能模块分布

| 模块 | 接口数 | 占比 | 主要功能 |
|------|--------|------|---------|
| 系统管理（用户/角色/菜单/部门） | 25 | 30.9% | 权限体系核心 |
| 实验室资源（实验室/设备/报修） | 23 | 28.4% | 资源管理与维护 |
| 预约管理 | 13 | 16.0% | 核心业务流程 |
| 耗材与库存 | 10 | 12.3% | 出入库与预警 |
| 统计与日志 | 6 | 7.4% | 数据聚合与审计 |
| 认证与鉴权 | 4 | 4.9% | 登录/注册/Tokan |
| **总计** | **81** | **100%** | —— |

### 3.3 按 HTTP 方法分布

| HTTP 方法 | 接口数 | 说明 |
|-----------|--------|------|
| GET | 38 | 列表查询、详情、导出、统计 |
| POST | 21 | 新增、提交、入库/出库、登录、注册 |
| PUT | 17 | 更新、审核、签到、签退、取消、重置密码 |
| DELETE | 5 | 删除、批量删除 |
| **总计** | **81** | —— |

---

## 4. 数据互通与接口依赖（关键）

以下表格列出"**写入操作 → 下游受影响的数据接口 → 主动刷新机制**"的依赖关系，用于答辩时说明系统实时性设计（配合 `StatCacheInvalidator` + `StatPushService` + WebSocket 实现）。

### 4.1 核心依赖关系表

| 写入操作接口 | 触发更新的下游接口/统计 | 缓存与推送机制 |
|-------------|-------------------------|---------------|
| `POST /api/lab/repair`<br>`PUT /api/lab/repair`<br>`DELETE /api/lab/repair` | `GET /api/stat/overview` → `myRepairCount` / `pendingRepairs`<br>`GET /api/lab/repair/list`<br>`GET /api/lab/repair/mine`<br>`GET /api/stat/fault` | `StatCacheInvalidator` 清除 `repair:*` 缓存键；WebSocket 推送 `refresh-overview` / `refresh-repair` 事件 |
| `POST /api/reservation`<br>`PUT /api/reservation`<br>`PUT /api/reservation/{id}/approve`<br>`PUT /api/reservation/{id}/reject`<br>`PUT /api/reservation/{id}/check-in`<br>`PUT /api/reservation/{id}/check-out`<br>`PUT /api/reservation/{id}/cancel`<br>`DELETE /api/reservation` | `GET /api/stat/overview` → `pendingReservations` / `myReservationCount`<br>`GET /api/reservation/list`<br>`GET /api/reservation/mine`<br>`GET /api/reservation/audit`<br>`GET /api/stat/usage` | `StatCacheInvalidator` 清除 `reservation:*` 缓存键；WebSocket 推送 `refresh-overview` / `refresh-reservation` 事件 |
| `POST /api/stock/item`<br>`PUT /api/stock/item`<br>`DELETE /api/stock/item`<br>`POST /api/stock/record/import`<br>`POST /api/stock/record/export` | `GET /api/stat/overview` → `stockWarnings`<br>`GET /api/stock/item/list`<br>`GET /api/stock/warnings`<br>`GET /api/stat/stock-warning`<br>`GET /api/stock/record/list` | `StatCacheInvalidator` 清除 `stock:*` 缓存键；WebSocket 推送 `refresh-overview` / `refresh-stock` 事件 |
| `POST /api/lab/room`<br>`PUT /api/lab/room`<br>`DELETE /api/lab/room` | `GET /api/lab/room/list`<br>`GET /api/lab/room/all`<br>`GET /api/stat/usage`<br>`GET /api/stat/overview` → 实验室总数统计 | 清除 `lab-room:*` 缓存；WebSocket 推送 `refresh-room` |
| `POST /api/lab/device`<br>`PUT /api/lab/device`<br>`DELETE /api/lab/device` | `GET /api/lab/device/list`<br>`GET /api/lab/room/{id}`（关联设备列表） | 清除 `lab-device:*` 缓存 |
| `POST /api/user`<br>`PUT /api/user`<br>`DELETE /api/user`<br>`PUT /api/user/reset/{id}` | `GET /api/user/list`<br>`GET /api/auth/info` | 清除 `user:*` 缓存；被重置密码用户下次请求触发重新登录 |
| `POST /api/role`<br>`PUT /api/role`<br>`DELETE /api/role` | `GET /api/role/list`<br>`GET /api/role/all`<br>`GET /api/auth/info`（菜单树刷新） | 清除 `role:*` 缓存；受影响用户 `auth/info` 下次返回最新角色 |
| `*`（所有写操作） | `GET /api/log/list` | `OperationLogAspect` AOP 切面自动记录 `sys_log` |

### 4.2 实时刷新机制说明

```
┌────────────────────────────────────────────────────────────┐
│  写入操作（POST/PUT/DELETE）                                │
│          ↓                                                  │
│  @OperationLogAspect ──→ 记录 sys_log（审计用途）             │
│          ↓                                                  │
│  @CacheEvict / StatCacheInvalidator ──→ 清除相关 Redis 缓存   │
│          ↓                                                  │
│  StatPushService.broadcast("refresh-overview")              │
│          ↓                                                  │
│  WebSocket（/ws/stat） ──→ 前端监听并自动刷新工作台卡片       │
└────────────────────────────────────────────────────────────┘
```

### 4.3 WebSocket 推送事件列表

| 事件类型（Event） | 触发场景 | 前端处理 |
|------------------|---------|---------|
| `refresh-overview` | 统计总览数据发生变化（预约/维修/库存任一写操作） | 工作台 dashboard 重新调用 `/api/stat/overview` |
| `refresh-reservation` | 预约列表/状态变化 | `reservation/mine`、`reservation/audit` 列表重新加载 |
| `refresh-repair` | 维修记录变化 | `lab/repair/mine`、`lab/repair/list` 列表重新加载 |
| `refresh-stock` | 耗材库存变化 | `stock/item/list`、`stock/warnings` 列表重新加载 |
| `refresh-room` | 实验室信息变化 | `lab/room/list` 重新加载 |

---

## 5. 前端 API 模块对应

前端项目 `lab-frontend/src/api/` 目录下共 **6 个 JS 文件**，与后端 Controller 映射关系如下：

### 表 5.1 前端 API 模块映射

| 前端文件 | 对应后端 Controller | 主要方法（函数） | 对应页面 |
|---------|---------------------|-----------------|---------|
| **`auth.js`** | `AuthController` | `login()`、`register()`、`logout()`、`getInfo()` | `views/auth/login/`、`views/auth/register/` |
| **`system.js`** | `SysUserController`<br>`SysRoleController`<br>`SysMenuController`<br>`SysDeptController`<br>`SysLogController` | `user.list()`、`user.add()`、`user.update()`、`user.del()`、`user.reset()`<br>`role.list()`、`role.add()`、`role.update()`、`role.del()`<br>`menu.tree()`、`menu.save()`<br>`dept.tree()`、`dept.save()`<br>`log.list()`、`log.export()` | `views/system/user/`<br>`views/system/role/`<br>`views/system/dept/`<br>`views/system/log/` |
| **`lab.js`** | `LabRoomController`<br>`LabDeviceController`<br>`LabDeviceRepairController` | `room.list()`、`room.add()`、`room.update()`、`room.del()`、`room.export()`、`room.import()`<br>`device.list()`、`device.add()`、`device.update()`、`device.del()`<br>`repair.list()`、`repair.mine()`、`repair.add()`、`repair.update()`、`repair.del()` | `views/lab/room/`<br>`views/lab/device/`<br>`views/lab/repair/`<br>`views/lab/repair/mine/` |
| **`reserve.js`** | `ReservationController` | `reservation.list()`、`reservation.mine()`、`reservation.audit()`<br>`reservation.add()`、`reservation.update()`、`reservation.del()`<br>`reservation.approve()`、`reservation.reject()`<br>`reservation.checkIn()`、`reservation.checkOut()`、`reservation.cancel()` | `views/reserve/mine/`<br>`views/reserve/audit/`<br>`views/reserve/check/` |
| **`stock.js`** | `StockController` | `stockItem.list()`、`stockItem.add()`、`stockItem.update()`、`stockItem.del()`<br>`stockRecord.list()`、`stockRecord.import()`（入库）、`stockRecord.export()`（出库）<br>`stock.warnings()` | `views/stock/item/`<br>`views/stock/record/` |
| **`stat.js`** | `StatController` | `stat.overview()`、`stat.usage()`、`stat.fault()`、`stat.stockWarning()` | `views/dashboard/`<br>`views/stat/usage/`<br>`views/stat/fault/`<br>`views/stat/stock-warning/` |

### 5.2 请求拦截器说明（`src/utils/request.js`）

前端通过 `axios` 封装统一请求工具，配合路由守卫（`src/router/router-guards.js`）完成前端鉴权：

| 拦截点 | 处理逻辑 |
|--------|---------|
| **请求拦截器** | 自动在 Header 中注入 `Authorization: Bearer ${token}`；统一拼接 `/api` 前缀 |
| **响应拦截器** | 解析 `Result<T>`：`code=200` 返回 `data`；`code=2001/2004` 跳转登录页并清除 token；其他错误码通过 `Message.error(msg)` 弹窗提示 |
| **路由守卫** | 检查 `token` 是否存在 + 菜单树是否已加载；未登录跳转 `/login`；已登录访问登录页跳转 `/dashboard` |

### 5.3 前端模块目录树

```
lab-frontend/src/api/
├── auth.js          # 登录/注册/登出/用户信息（AuthController）
├── system.js        # 用户/角色/菜单/部门/日志（Sys*Controller）
├── lab.js           # 实验室/设备/报修（Lab*Controller）
├── reserve.js       # 预约管理（ReservationController）
├── stock.js         # 耗材/库存（StockController）
└── stat.js          # 统计（StatController）
```

---

## 附录：快速索引

| 章节 | 内容 |
|------|------|
| §1 | 接口规范总览（统一前缀、JWT、Result/PageResult、错误码） |
| §2.1 | 认证接口（AuthController，4 个接口） |
| §2.2 | 用户管理（SysUserController，8 个接口） |
| §2.3 | 角色管理（SysRoleController，7 个接口） |
| §2.4 | 菜单管理（SysMenuController，5 个接口） |
| §2.5 | 部门管理（SysDeptController，5 个接口） |
| §2.6 | 实验室管理（LabRoomController，9 个接口） |
| §2.7 | 设备管理（LabDeviceController，7 个接口） |
| §2.8 | 设备报修（LabDeviceRepairController，7 个接口） |
| §2.9 | 预约管理（ReservationController，13 个接口） |
| §2.10 | 耗材管理（StockController，10 个接口） |
| §2.11 | 统计模块（StatController，4 个接口） |
| §2.12 | 操作日志（SysLogController，2 个接口） |
| §3 | 接口总览统计（12 Controller，约 81 接口） |
| §4 | 数据互通与接口依赖（缓存失效 + WebSocket 推送） |
| §5 | 前端 API 模块对应（6 个 JS 文件 → 后端 Controller 映射） |

> **答辩提示**：本系统接口采用统一的 RESTful 风格与 `Result<T>` 响应结构，通过 JWT + 角色权限体系实现分层鉴权，配合 AOP 操作日志 + WebSocket 实时推送 + Redis 缓存失效机制，构成一个完整、可扩展、具备实时反馈能力的后端服务体系。

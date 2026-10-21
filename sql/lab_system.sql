/*
 * 校园实验室预约与设备耗材管理系统 - 数据库脚本
 * MySQL 8.x / utf8mb4
 *
 * 默认账号密码：admin / 123456
 * 密码使用 BCrypt 加密：$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================================
-- 模块1：用户 / 角色 / 菜单 / 部门 / 操作日志
-- =========================================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  username     VARCHAR(50)  NOT NULL COMMENT '登录名',
  password     VARCHAR(120) NOT NULL COMMENT 'BCrypt 密文',
  real_name    VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  phone        VARCHAR(20)           DEFAULT NULL,
  email        VARCHAR(80)           DEFAULT NULL,
  gender       TINYINT      NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  avatar       VARCHAR(255)          DEFAULT NULL,
  dept_id      BIGINT                DEFAULT NULL COMMENT '所属部门/学院',
  status       TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  remark       VARCHAR(255)          DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  role_code    VARCHAR(50) NOT NULL COMMENT '角色编码 ROLE_xxx',
  role_name    VARCHAR(50) NOT NULL,
  sort_no      INT         NOT NULL DEFAULT 0,
  status       TINYINT     NOT NULL DEFAULT 1,
  remark       VARCHAR(255)         DEFAULT NULL,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id    BIGINT      NOT NULL DEFAULT 0,
  name         VARCHAR(50) NOT NULL COMMENT '菜单名',
  path         VARCHAR(120)         DEFAULT NULL COMMENT '前端路由',
  component    VARCHAR(120)         DEFAULT NULL COMMENT '前端组件',
  icon         VARCHAR(60)          DEFAULT NULL,
  perm         VARCHAR(120)         DEFAULT NULL COMMENT '按钮权限标识',
  type         TINYINT     NOT NULL DEFAULT 1 COMMENT '1目录 2菜单 3按钮',
  sort_no      INT         NOT NULL DEFAULT 0,
  visible      TINYINT     NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-角色';

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色-菜单';

DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT      NOT NULL DEFAULT 0,
  name        VARCHAR(60) NOT NULL,
  sort_no     INT         NOT NULL DEFAULT 0,
  status      TINYINT     NOT NULL DEFAULT 1,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门/学院';

DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT                DEFAULT NULL,
  username    VARCHAR(50)           DEFAULT NULL,
  module      VARCHAR(60)           DEFAULT NULL,
  action      VARCHAR(60)           DEFAULT NULL COMMENT '操作类型',
  method      VARCHAR(255)          DEFAULT NULL,
  params      TEXT,
  ip          VARCHAR(64)           DEFAULT NULL,
  cost_ms     BIGINT                DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  error_msg   VARCHAR(500)          DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_log_user (user_id),
  KEY idx_log_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

-- =========================================================================
-- 模块2：实验室 / 设备 / 维修
-- =========================================================================
DROP TABLE IF EXISTS lab_room;
CREATE TABLE lab_room (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(40)  NOT NULL COMMENT '实验室编号',
  name        VARCHAR(80)  NOT NULL,
  building    VARCHAR(60)           DEFAULT NULL COMMENT '所在楼栋',
  room_no     VARCHAR(40)           DEFAULT NULL,
  capacity    INT          NOT NULL DEFAULT 0  COMMENT '容纳人数',
  dept_id     BIGINT                DEFAULT NULL COMMENT '归属学院',
  manager_id  BIGINT                DEFAULT NULL COMMENT '管理员用户id',
  status      TINYINT      NOT NULL DEFAULT 1  COMMENT '1可用 0停用 2维护',
  qr_code     VARCHAR(255)          DEFAULT NULL,
  remark      VARCHAR(255)          DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lab_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '实验室';

DROP TABLE IF EXISTS lab_device;
CREATE TABLE lab_device (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  asset_no     VARCHAR(60)  NOT NULL COMMENT '资产编号',
  name         VARCHAR(80)  NOT NULL,
  category     VARCHAR(60)           DEFAULT NULL COMMENT '设备类型',
  brand        VARCHAR(60)           DEFAULT NULL,
  model        VARCHAR(60)           DEFAULT NULL,
  lab_id       BIGINT                DEFAULT NULL COMMENT '所属实验室',
  purchase_date DATE                 DEFAULT NULL,
  price        DECIMAL(12,2)         DEFAULT NULL,
  status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1在用 2维修 3报废',
  remark       VARCHAR(255)          DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_asset_no (asset_no),
  KEY idx_dev_lab (lab_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备';

DROP TABLE IF EXISTS lab_device_repair;
CREATE TABLE lab_device_repair (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  device_id   BIGINT       NOT NULL,
  reporter_id BIGINT       NOT NULL COMMENT '报修人',
  fault_desc  VARCHAR(500) NOT NULL,
  handler_id  BIGINT                DEFAULT NULL COMMENT '处理人',
  handle_note VARCHAR(500)          DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0待指派 1处理中 2已完成 3已驳回',
  report_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time DATETIME              DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_rep_dev (device_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '设备维修单';

-- =========================================================================
-- 模块3：预约 / 审核 / 签到
-- =========================================================================
DROP TABLE IF EXISTS lab_reservation;
CREATE TABLE lab_reservation (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  reserve_no    VARCHAR(40)  NOT NULL COMMENT '预约单号',
  user_id       BIGINT       NOT NULL COMMENT '申请人',
  teacher_id    BIGINT                DEFAULT NULL COMMENT '指导教师（学生申请时必填）',
  lab_id        BIGINT       NOT NULL,
  device_ids    VARCHAR(500)          DEFAULT NULL COMMENT '逗号分隔设备id（可选）',
  purpose       VARCHAR(255) NOT NULL COMMENT '使用目的',
  start_time    DATETIME     NOT NULL,
  end_time      DATETIME     NOT NULL,
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回 3已签到 4已签退 5已取消',
  audit_user_id BIGINT                DEFAULT NULL,
  audit_time    DATETIME              DEFAULT NULL,
  audit_note    VARCHAR(255)          DEFAULT NULL,
  check_in_time  DATETIME             DEFAULT NULL,
  check_out_time DATETIME             DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reserve_no (reserve_no),
  KEY idx_res_lab_time (lab_id, start_time, end_time),
  KEY idx_res_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约单';

-- =========================================================================
-- 模块4：耗材 / 出入库（结构 + 占位接口）
-- =========================================================================
DROP TABLE IF EXISTS stock_item;
CREATE TABLE stock_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(40)  NOT NULL,
  name        VARCHAR(80)  NOT NULL,
  category    VARCHAR(60)           DEFAULT NULL,
  unit        VARCHAR(20)           DEFAULT NULL,
  qty         INT          NOT NULL DEFAULT 0,
  warn_qty    INT          NOT NULL DEFAULT 0 COMMENT '预警阈值',
  lab_id      BIGINT                DEFAULT NULL,
  remark      VARCHAR(255)          DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stock_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '耗材档案';

DROP TABLE IF EXISTS stock_record;
CREATE TABLE stock_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  item_id       BIGINT       NOT NULL,
  type          TINYINT      NOT NULL COMMENT '1入库 2出库 3盘点',
  qty           INT          NOT NULL,
  reservation_id BIGINT               DEFAULT NULL COMMENT '关联预约单，模块联动',
  user_id       BIGINT                DEFAULT NULL COMMENT '经办人',
  remark        VARCHAR(255)          DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sr_item (item_id),
  KEY idx_sr_res  (reservation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出入库流水';

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================================
-- 初始化数据
-- =========================================================================

-- 部门
INSERT INTO sys_dept (id, parent_id, name, sort_no) VALUES
(1, 0, '总校',         0),
(2, 1, '计算机学院',   1),
(3, 1, '电子信息学院', 2),
(4, 1, '机械工程学院', 3);

-- 角色
INSERT INTO sys_role (id, role_code, role_name, sort_no, remark) VALUES
(1, 'ROLE_ADMIN',   '系统管理员',   1, '全部权限'),
(2, 'ROLE_LABADMIN','实验室管理员', 2, '负责本实验室设备/审核'),
(3, 'ROLE_TEACHER', '教师',         3, '可发起预约、指导学生'),
(4, 'ROLE_STUDENT', '学生',         4, '可发起预约');

-- 用户（密码均为 123456 的 BCrypt）
-- 核心测试账号：
--   1. admin           (ROLE_ADMIN)     : 系统超级管理员，可访问所有模块和全部实验室数据
--   2. labadmin        (ROLE_LABADMIN)  : 综合测试账号，管理 LAB-001
--   3. teacher         (ROLE_TEACHER)   : 教师李教师，可审核本学院预约
--   4. student         (ROLE_STUDENT)   : 学生张同学，可发起预约
--   5. teacher_sun     (ROLE_TEACHER)   : 教师孙教师
--   7. student_wang    (ROLE_STUDENT)   : 学生王同学
--
-- 各实验室独立管理员账号（数据隔离）：
--   102. labadmin_lab02 (LAB-002 网络与安全实验室)
--   103. labadmin_lab03 (LAB-003 嵌入式实验室)
--   104. labadmin_lab04 (LAB-004 软件工程实验室二)
--   105. labadmin_lab05 (LAB-005 数据库技术实验室)
--   106. labadmin_lab06 (LAB-006 人工智能实验室)
--   107. labadmin_lab07 (LAB-007 物联网综合实验室)
--   108. labadmin_lab08 (LAB-008 电子电工实验室)
--   109. labadmin_lab09 (LAB-009 移动应用开发实验室)
--   110. labadmin_lab10 (LAB-010 云计算与运维实验室)
--
-- 数据隔离：每个 LABADMIN 仅可操作自己管理的实验室（设备、报修、耗材、审核等）
INSERT INTO sys_user (id, username, password, real_name, phone, email, gender, dept_id) VALUES
(1, 'admin',           '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '超级管理员',    '13800000000', 'admin@lab.edu',       1, 1),
(2, 'labadmin',        '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '实验室王老师',  '13800000001', 'lab@lab.edu',         1, 2),
(3, 'teacher',         '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '李教师',        '13800000002', 'teacher@lab.edu',     1, 2),
(4, 'student',         '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '张同学',        '13800000003', 'student@lab.edu',     1, 2),
(5, 'teacher_sun',     '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '孙教师',        '13800000022', 'teacher_sun@lab.edu', 1, 3),
(7, 'student_wang',    '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '王同学',        '13800000004', 'student_wang@lab.edu',1, 2),
(102, 'labadmin_lab02', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '陈实验室管理员', '13800000102', 'lab02@lab.edu',     1, 2),
(103, 'labadmin_lab03', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '赵实验室管理员', '13800000103', 'lab03@lab.edu',     1, 2),
(104, 'labadmin_lab04', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '孙实验室管理员', '13800000104', 'lab04@lab.edu',     1, 2),
(105, 'labadmin_lab05', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '周实验室管理员', '13800000105', 'lab05@lab.edu',     1, 2),
(106, 'labadmin_lab06', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '吴实验室管理员', '13800000106', 'lab06@lab.edu',     1, 2),
(107, 'labadmin_lab07', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '郑实验室管理员', '13800000107', 'lab07@lab.edu',     1, 2),
(108, 'labadmin_lab08', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '冯实验室管理员', '13800000108', 'lab08@lab.edu',     1, 2),
(109, 'labadmin_lab09', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '褚实验室管理员', '13800000109', 'lab09@lab.edu',     1, 2),
(110, 'labadmin_lab10', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '卫实验室管理员', '13800000110', 'lab10@lab.edu',     1, 2);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1,1),                                                        -- 超级管理员
(2,2),                                                        -- 综合测试账号，管理 LAB-001
(3,3),(5,3),                                                  -- 教师（role_id=3 ROLE_TEACHER）
(4,4),(7,4),                                                  -- 学生（role_id=4 ROLE_STUDENT）
(102,2),(103,2),(104,2),(105,2),(106,2),                     -- 各实验室独立管理员
(107,2),(108,2),(109,2),(110,2);

-- 菜单（type:1目录 2菜单 3按钮）
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, perm, type, sort_no) VALUES
-- 目录
(1,   0, '系统管理',   '/system',     'Layout', 'el-icon-setting',           NULL,                  1, 1),
(2,   0, '实验室管理', '/lab',        'Layout', 'el-icon-office-building',   NULL,                  1, 2),
(3,   0, '预约中心',   '/reserve',    'Layout', 'el-icon-date',              NULL,                  1, 3),
(4,   0, '耗材管理',   '/stock',      'Layout', 'el-icon-box',               NULL,                  1, 4),
(5,   0, '统计报表',   '/stat',       'Layout', 'el-icon-data-line',         NULL,                  1, 5),

-- 系统管理
(11,  1, '用户管理',   'user',   'system/user/index',   'el-icon-user',          'system:user:list',  2, 1),
(12,  1, '角色管理',   'role',   'system/role/index',   'el-icon-s-custom',      'system:role:list',  2, 2),
(13,  1, '部门管理',   'dept',   'system/dept/index',   'el-icon-school',        'system:dept:list',  2, 3),
(14,  1, '操作日志',   'log',    'system/log/index',    'el-icon-document',      'system:log:list',   2, 4),

-- 实验室
(21,  2, '实验室档案', 'room',   'lab/room/index',     'el-icon-house',          'lab:room:list',     2, 1),
(22,  2, '设备台账',   'device', 'lab/device/index',   'el-icon-cpu',            'lab:device:list',   2, 2),
(23,  2, '维修单',     'repair', 'lab/repair/index',   'el-icon-s-tools',        'lab:repair:list',   2, 3),
(24,  2, '我的报修记录', 'repair/mine', 'lab/repair/mine/index', 'el-icon-document', 'lab:repair:mine', 2, 4),

-- 预约
(31,  3, '我的预约',   'mine',    'reserve/mine/index',  'el-icon-edit-outline', 'reserve:mine',      2, 1),
(32,  3, '预约审核',   'audit',   'reserve/audit/index', 'el-icon-finished',     'reserve:audit',     2, 2),
(33,  3, '签到记录',   'check',   'reserve/check/index', 'el-icon-circle-check', 'reserve:check',     2, 3),

-- 耗材
(41,  4, '耗材档案',   'item',    'stock/item/index',    'el-icon-goods',        'stock:item:list',   2, 1),
(42,  4, '出入库',     'record',  'stock/record/index',  'el-icon-sort',         'stock:record:list', 2, 2),

-- 统计
(51,  5, '使用率分析', 'usage',   'stat/usage/index',    'el-icon-pie-chart',    'stat:usage',        2, 1),
(52,  5, '设备故障',   'fault',   'stat/fault/index',    'el-icon-warning',      'stat:fault',        2, 2),
(53,  5, '库存预警',   'stock-warning', 'stat/stock-warning/index', 'el-icon-warning-outline', 'stat:stock-warning', 2, 3);

-- 角色-菜单（admin 全部，其余按需）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
-- 实验室管理员：实验室管理（本实验室）、预约审核/签到、耗材管理、统计分析、我的报修记录
--   - 可见模块：实验室管理 / 预约中心 / 耗材管理 / 统计分析
--   - 数据范围：仅本实验室（lab_room.manager_id = 当前用户）
(2,2),(2,21),(2,22),(2,23),(2,24),
(2,3),(2,31),(2,32),(2,33),
(2,4),(2,41),(2,42),
(2,5),(2,51),(2,52),(2,53),
-- 教师：实验室档案（只读）、设备台账（只读）、我的预约、签到记录、耗材档案（只读）、我的报修记录
--   - 可见模块：实验室管理 / 预约中心 / 耗材管理（仅耗材档案）
--   - ⚠️ 教师不可见统计分析模块（stat/* 接口仅开放给 ADMIN/LABADMIN），避免出现"无访问权限"
--   - 数据范围：仅本学院（dept_id）下的实验室可审核预约、可见设备与耗材
(3,2),(3,21),(3,22),(3,24),
(3,3),(3,31),(3,32),(3,33),
(3,4),(3,41),
-- 学生：实验室档案（只读）、设备台账（只读）、我的预约、签到记录、我的报修记录
--   - 数据范围：仅本人相关（自己提交的预约、自己的报修记录）
(4,2),(4,21),(4,22),(4,24),
(4,3),(4,31),(4,33);

-- 实验室
--   - 每个实验室绑定独立实验室管理员账号（manager_id -> sys_user.id），对应角色 ROLE_LABADMIN
--   - LABADMIN 登录后只能查看/操作自己管理的实验室数据（设备、报修、耗材、审核预约）
INSERT INTO lab_room (id, code, name, building, room_no, capacity, dept_id, manager_id, status) VALUES
(1,  'LAB-001', '软件工程实验室一', '信息楼', 'A301', 60, 2, 2,   1),   -- manager = labadmin (用户ID=2, 综合测试账号)
(2,  'LAB-002', '网络与安全实验室', '信息楼', 'A302', 40, 2, 102, 1),   -- manager = labadmin_lab02 (用户ID=102)
(3,  'LAB-003', '嵌入式实验室',     '信息楼', 'A303', 30, 2, 103, 1),   -- manager = labadmin_lab03 (用户ID=103)
(10, 'LAB-004', '软件工程实验室二', '信息楼', 'A304', 50, 2, 104, 1),   -- manager = labadmin_lab04 (用户ID=104)
(11, 'LAB-005', '数据库技术实验室', '信息楼', 'A305', 40, 2, 105, 1),   -- manager = labadmin_lab05 (用户ID=105)
(12, 'LAB-006', '人工智能实验室',   '科创楼', 'B201', 60, 2, 106, 1),   -- manager = labadmin_lab06 (用户ID=106)
(13, 'LAB-007', '物联网综合实验室', '科创楼', 'B202', 45, 2, 107, 1),   -- manager = labadmin_lab07 (用户ID=107)
(14, 'LAB-008', '电子电工实验室',   '实验楼', 'C101', 35, 2, 108, 1),   -- manager = labadmin_lab08 (用户ID=108)
(15, 'LAB-009', '移动应用开发实验室','信息楼', 'A306', 40, 2, 109, 1),   -- manager = labadmin_lab09 (用户ID=109)
(16, 'LAB-010', '云计算与运维实验室','信息楼', 'A401', 50, 2, 110, 1);   -- manager = labadmin_lab10 (用户ID=110)

-- 设备
INSERT INTO lab_device (id, asset_no, name, category, brand, model, lab_id, status, price) VALUES
(1, 'DEV-0001', '台式机', '计算机', '联想', 'M540',     1, 1, 4500.00),
(2, 'DEV-0002', '台式机', '计算机', '联想', 'M540',     1, 1, 4500.00),
(3, 'DEV-0003', '交换机', '网络',   '华为', 'S5700',    2, 1, 6800.00),
(4, 'DEV-0004', '示波器', '仪器',   'RIGOL','DS1054Z',  3, 1, 3200.00);

-- 示例预约（已通过）
INSERT INTO lab_reservation
(id, reserve_no, user_id, teacher_id, lab_id, device_ids, purpose, start_time, end_time, status, audit_user_id, audit_time)
VALUES
(1, 'R20260616001', 7, 5, 1, '1,2', '《Java EE 课程实验》', '2026-06-17 14:00:00', '2026-06-17 17:00:00', 1, 2, NOW());

-- 耗材
INSERT INTO stock_item (id, code, name, category, unit, qty, warn_qty, lab_id) VALUES
(1, 'C-0001', '网线（5米）', '耗材', '根',  100, 20, 2),
(2, 'C-0002', 'A4 打印纸',   '办公', '包',   30, 10, 1),
(3, 'C-0003', '电阻 1k 欧',  '电子', '只', 1000, 200, 3);

-- 耗材出入库流水（由各实验室管理员操作）
INSERT INTO stock_record (id, item_id, type, qty, reservation_id, user_id, remark, create_time) VALUES
(1, 1, 1, 60, NULL, 102, '学期初网络实验耗材入库', '2026-06-10 09:00:00'),     -- labadmin_lab02 操作 lab_id=2
(2, 2, 1, 20, NULL, 2,   '实验报告打印纸补充',    '2026-06-10 09:30:00'),     -- labadmin 操作 lab_id=1
(3, 3, 1, 500, NULL, 103, '电子实验基础元件补充', '2026-06-10 10:00:00'),     -- labadmin_lab03 操作 lab_id=3
(4, 1, 2, 8, 1, 2,   'Java EE 课程实验预约领用', '2026-06-16 13:40:00'),
(5, 3, 2, 120, 1, 103, '嵌入式实验课领用',         '2026-06-16 14:10:00');

-- ============================================================
-- 【权限修复脚本】在已部署数据库执行：防止教师/学生出现"无访问权限"
-- ------------------------------------------------------------
-- 统计分析（stat/* 接口仅开放给 ADMIN/LABADMIN），教师/学生不应有该菜单
DELETE FROM sys_role_menu WHERE role_id IN (3, 4) AND menu_id IN (5, 51, 52, 53);
-- 耗材出入库记录（stock_record/* 接口仅开放给 ADMIN/LABADMIN），教师/学生不应有该菜单
DELETE FROM sys_role_menu WHERE role_id IN (3, 4) AND menu_id = 42;
-- 维修单（lab/repair/page 接口仅开放给 ADMIN/LABADMIN），教师/学生不应有该菜单
DELETE FROM sys_role_menu WHERE role_id IN (3, 4) AND menu_id = 23;

-- ============================================================
-- 数据范围：按账号-角色-实验室匹配（数据初始化参考）
-- 核心测试账号：
--   sys_user.id=1   -> 角色 ADMIN (sys_role.id=1)     -> 全部实验室
--   sys_user.id=2   -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=2 (LAB-001 软件工程实验室一)
--   sys_user.id=3   -> 角色 TEACHER (sys_role.id=3)    -> 学院 dept_id=2
--   sys_user.id=4   -> 角色 STUDENT (sys_role.id=4)    -> 学院 dept_id=2
--   sys_user.id=5   -> 角色 TEACHER (sys_role.id=3)    -> 学院 dept_id=3
--   sys_user.id=7   -> 角色 STUDENT (sys_role.id=4)    -> 学院 dept_id=2
-- 各实验室独立管理员：
--   sys_user.id=102 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=102 (LAB-002 网络与安全实验室)
--   sys_user.id=103 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=103 (LAB-003 嵌入式实验室)
--   sys_user.id=104 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=104 (LAB-004 软件工程实验室二)
--   sys_user.id=105 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=105 (LAB-005 数据库技术实验室)
--   sys_user.id=106 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=106 (LAB-006 人工智能实验室)
--   sys_user.id=107 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=107 (LAB-007 物联网综合实验室)
--   sys_user.id=108 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=108 (LAB-008 电子电工实验室)
--   sys_user.id=109 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=109 (LAB-009 移动应用开发实验室)
--   sys_user.id=110 -> 角色 LABADMIN (sys_role.id=2)   -> lab_room.manager_id=110 (LAB-010 云计算与运维实验室)

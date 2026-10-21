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
INSERT INTO sys_user (id, username, password, real_name, phone, email, gender, dept_id) VALUES
(1, 'admin',    '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '超级管理员', '13800000000', 'admin@lab.edu', 1, 1),
(2, 'labadmin', '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '实验室王老师', '13800000001', 'lab@lab.edu',   1, 2),
(3, 'teacher',  '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '李教师',     '13800000002', 't@lab.edu',     1, 2),
(4, 'student',  '$2a$10$rmo6sNdR3/ZdbPAX1rEGGOaCNiYfk/Sw13a0uFixgKHLLz9KKpR/G', '张同学',     '13800000003', 's@lab.edu',     1, 2);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1,1),(2,2),(3,3),(4,4);

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
-- 实验室管理员：实验室管理、预约审核/签到、耗材、统计
(2,2),(2,21),(2,22),(2,23),
(2,3),(2,31),(2,32),(2,33),
(2,4),(2,41),(2,42),
(2,5),(2,51),(2,52),(2,53),
-- 教师：实验室档案、设备台账、我的预约、签到记录、耗材档案查看、统计
(3,2),(3,21),(3,22),
(3,3),(3,31),(3,33),
(3,4),(3,41),(3,42),
(3,5),(3,51),(3,52),(3,53),
-- 学生：实验室档案、我的预约、签到记录
(4,2),(4,21),
(4,3),(4,31),(4,33);

-- 实验室
INSERT INTO lab_room (id, code, name, building, room_no, capacity, dept_id, manager_id, status) VALUES
(1, 'LAB-001', '软件工程实验室一', '信息楼', 'A301', 60, 2, 2, 1),
(2, 'LAB-002', '网络与安全实验室', '信息楼', 'A302', 40, 2, 2, 1),
(3, 'LAB-003', '嵌入式实验室',     '信息楼', 'A303', 30, 3, 2, 1);

-- 设备
INSERT INTO lab_device (id, asset_no, name, category, brand, model, lab_id, status, price) VALUES
(1, 'DEV-0001', '台式机', '计算机', '联想', 'M540',     1, 1, 4500.00),
(2, 'DEV-0002', '台式机', '计算机', '联想', 'M540',     1, 1, 4500.00),
(3, 'DEV-0003', '交换机', '网络',   '华为', 'S5700',    2, 1, 6800.00),
(4, 'DEV-0004', '示波器', '仪器',   'RIGOL','DS1054Z',  3, 1, 3200.00);

-- 一条示例预约（已通过）
INSERT INTO lab_reservation
(id, reserve_no, user_id, teacher_id, lab_id, device_ids, purpose, start_time, end_time, status, audit_user_id, audit_time)
VALUES
(1, 'R20260616001', 4, 3, 1, '1,2', '《Java EE 课程实验》', '2026-06-17 14:00:00', '2026-06-17 17:00:00', 1, 2, NOW());

-- 耗材
INSERT INTO stock_item (id, code, name, category, unit, qty, warn_qty, lab_id) VALUES
(1, 'C-0001', '网线（5米）', '耗材', '根',  100, 20, 2),
(2, 'C-0002', 'A4 打印纸',   '办公', '包',   30, 10, 1),
(3, 'C-0003', '电阻 1k 欧',  '电子', '只', 1000, 200, 3);

-- 耗材出入库流水（用于库存预警、预约联动和统计展示）
INSERT INTO stock_record (id, item_id, type, qty, reservation_id, user_id, remark, create_time) VALUES
(1, 1, 1, 60, NULL, 2, '学期初网络实验耗材入库', '2026-06-10 09:00:00'),
(2, 2, 1, 20, NULL, 2, '实验报告打印纸补充', '2026-06-10 09:30:00'),
(3, 3, 1, 500, NULL, 2, '电子实验基础元件补充', '2026-06-10 10:00:00'),
(4, 1, 2, 8, 1, 2, 'Java EE 课程实验预约领用', '2026-06-16 13:40:00'),
(5, 3, 2, 120, 1, 2, '嵌入式实验课领用', '2026-06-16 14:10:00');

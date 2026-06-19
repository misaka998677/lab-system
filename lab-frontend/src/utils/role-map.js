/**
 * 角色专属默认路由与标签映射
 * 规则：
 *  - ROLE_ADMIN: 系统管理工作台 (用户管理)
 *  - ROLE_LABADMIN: 实验室运营工作台 (实验室档案)
 *  - ROLE_TEACHER: 教师预约工作台 (我的预约)
 *  - ROLE_STUDENT: 学生预约工作台 (实验室浏览 + 我的预约)
 *  - 其他角色或未分配: 默认 /dashboard
 */

/** 角色常量 */
export const ROLE = {
  ADMIN: 'ROLE_ADMIN',
  LABADMIN: 'ROLE_LABADMIN',
  TEACHER: 'ROLE_TEACHER',
  STUDENT: 'ROLE_STUDENT'
}

/** 角色 -> 默认首页路径 */
export const ROLE_DEFAULT_ROUTE = {
  [ROLE.ADMIN]: '/system/user',
  [ROLE.LABADMIN]: '/lab/room',
  [ROLE.TEACHER]: '/reserve/mine',
  [ROLE.STUDENT]: '/reserve/mine'
}

/** 角色 -> 中文名标签 */
export const ROLE_TEXT = {
  [ROLE.ADMIN]: '系统管理员',
  [ROLE.LABADMIN]: '实验室管理员',
  [ROLE.TEACHER]: '教师',
  [ROLE.STUDENT]: '学生'
}

/** 角色 -> 主题色边条（浅色） */
export const ROLE_THEME = {
  [ROLE.ADMIN]: '#409EFF',        // 蓝色
  [ROLE.LABADMIN]: '#67C23A',      // 绿色
  [ROLE.TEACHER]: '#E6A23C',       // 橙色
  [ROLE.STUDENT]: '#909399'        // 深灰
}

/** 角色 -> 侧边栏分组标题（仅用于展示，不影响路由） */
export const ROLE_SIDEBAR_GROUP = {
  [ROLE.ADMIN]: ['系统管理', '统计分析'],
  [ROLE.LABADMIN]: ['实验室运营', '预约中心', '耗材管理', '统计分析'],
  [ROLE.TEACHER]: ['预约中心'],
  [ROLE.STUDENT]: ['实验室浏览', '预约中心']
}

/**
 * 从 roles 数组中取"最高优先级角色"
 * 优先级: ADMIN > LABADMIN > TEACHER > STUDENT
 */
export function primaryRole(roles = []) {
  if (!Array.isArray(roles) || roles.length === 0) return null
  const priority = [ROLE.ADMIN, ROLE.LABADMIN, ROLE.TEACHER, ROLE.STUDENT]
  for (const r of priority) {
    if (roles.includes(r)) return r
  }
  return roles[0]
}

/**
 * 根据角色数组返回默认首页路径
 * @param {string[]} roles
 * @param {string} fallback
 */
export function defaultRouteForRole(roles, fallback = '/dashboard') {
  const r = primaryRole(roles)
  return r ? (ROLE_DEFAULT_ROUTE[r] || fallback) : fallback
}

/**
 * 判断角色是否为"管理员类"（可见系统管理模块）
 */
export function isAdminClass(roles) {
  return !!primaryRole(roles) && [ROLE.ADMIN].includes(primaryRole(roles))
}

/**
 * 角色注册类型（前端表单 roleType -> 后端 ROLE_*）
 */
export function roleTypeToRole(roleType) {
  if (roleType === 'teacher') return ROLE.TEACHER
  if (roleType === 'student') return ROLE.STUDENT
  return ROLE.STUDENT
}

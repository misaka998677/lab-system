/**
 * 权限辅助工具。统一集中维护，禁止在页面内直接写 `roles.includes('xxx')`。
 *
 * 规范：
 *   - 角色字符串使用 "ROLE_" 前缀（由后端返回），这里统一封装，调用方无需关心。
 *   - 可组合能力：canManageLab / canAudit / canReportRepair ...
 */

/** 在运行态从 Vuex 中取当前用户的 roles/perms。 */
let roleGetter = () => []
let permGetter = () => []

export function installGetters(fn)  { roleGetter = fn }
export function installPermGetter(fn) { permGetter = fn }

export const hasRole = (roles, target) => {
  if (!roles || !target) return false
  const list = Array.isArray(target) ? target : [target]
  return list.some(r => roles.includes(r))
}

export const hasPerm = (perms, target) => {
  if (!perms || !target) return false
  const list = Array.isArray(target) ? target : [target]
  return list.some(p => perms.includes(p))
}

/** 常用角色判断。 */
export const isAdmin      = roles => hasRole(roles, 'ROLE_ADMIN')
export const isLabAdmin   = roles => hasRole(roles, 'ROLE_LABADMIN')
export const isTeacher    = roles => hasRole(roles, 'ROLE_TEACHER')
export const isStudent    = roles => hasRole(roles, 'ROLE_STUDENT')

/** 业务能力判断。 */
export const canManageLab      = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN'])
export const canAudit          = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN'])
export const canReportRepair   = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT'])
export const canViewStats      = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN'])
export const canImport         = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN'])
export const canExport         = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN', 'ROLE_TEACHER'])
export const canManageUser     = roles => hasRole(roles, 'ROLE_ADMIN')
export const canManageSystem   = roles => hasRole(roles, 'ROLE_ADMIN')
export const canCreateReservation = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT'])

/** 基于 Vuex 的简便 API。页面内直接使用： if (isCurrentAdmin()) { ... } */
export const currentRoles       = () => roleGetter() || []
export const currentHasRole     = (target) => hasRole(currentRoles(), target)
export const currentIsAdmin     = () => isAdmin(currentRoles())
export const currentIsLabAdmin  = () => isLabAdmin(currentRoles())
export const currentIsTeacher   = () => isTeacher(currentRoles())
export const currentIsStudent   = () => isStudent(currentRoles())
export const currentCanManageLab    = () => canManageLab(currentRoles())
export const currentCanAudit        = () => canAudit(currentRoles())
export const currentCanReportRepair = () => canReportRepair(currentRoles())
export const currentCanViewStats    = () => canViewStats(currentRoles())

/** 权限/角色字符串映射。供 v-permission 指令或页面使用。 */
export const KEY_MAP = {
  admin:     () => currentIsAdmin(),
  labadmin:  () => currentIsLabAdmin(),
  teacher:   () => currentIsTeacher(),
  student:   () => currentIsStudent(),
  manageLab: () => currentCanManageLab(),
  audit:     () => currentCanAudit(),
  report:    () => currentCanReportRepair(),
  stats:     () => currentCanViewStats(),
  import:    () => canImport(currentRoles()),
  export:    () => canExport(currentRoles()),
  system:    () => canManageSystem(currentRoles())
}

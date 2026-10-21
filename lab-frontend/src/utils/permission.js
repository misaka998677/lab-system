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

export const isAdmin = roles => hasRole(roles, 'ROLE_ADMIN')
export const isLabAdmin = roles => hasRole(roles, 'ROLE_LABADMIN')
export const isTeacher = roles => hasRole(roles, 'ROLE_TEACHER')
export const isStudent = roles => hasRole(roles, 'ROLE_STUDENT')

export const canManageLab = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN'])
export const canAudit = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN'])
export const canReportRepair = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN', 'ROLE_TEACHER'])
export const canViewStats = roles => hasRole(roles, ['ROLE_ADMIN', 'ROLE_LABADMIN'])

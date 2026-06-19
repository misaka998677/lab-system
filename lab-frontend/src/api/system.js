import request from '@/utils/request'

export const userPage     = p => request.get('/system/user/page', { params: p })
export const userDetail   = id => request.get(`/system/user/${id}`)
export const userCreate   = d => request.post('/system/user', d)
export const userUpdate   = d => request.put('/system/user', d)
export const userDelete   = id => request.delete(`/system/user/${id}`)
export const userStatus   = (id, s) => request.put(`/system/user/${id}/status/${s}`)
export const userResetPwd = (id, password) =>
  request.put(`/system/user/${id}/password`, null, { params: { password } })
export const userExport   = () => request.get('/system/user/export', { responseType: 'blob' })

export const rolePage     = p => request.get('/system/role/page', { params: p })
export const roleAll      = () => request.get('/system/role/all')
export const roleDetail   = id => request.get(`/system/role/${id}`)
export const roleCreate   = d => request.post('/system/role', d)
export const roleUpdate   = d => request.put('/system/role', d)
export const roleDelete   = id => request.delete(`/system/role/${id}`)
// roleAssignMenu(id, menuIds) —— 后端若未提供对应接口，可在此注释后通过 roleUpdate 间接提交
export const roleAssignMenu = (id, menuIds) =>
  request.post(`/system/role/${id}/menus`, { menuIds })

export const menuTree     = () => request.get('/system/menu/tree')

export const deptAll      = () => request.get('/system/dept/all')
export const deptTree     = () => request.get('/system/dept/tree')
export const deptCreate   = d => request.post('/system/dept', d)
export const deptUpdate   = d => request.put('/system/dept', d)
export const deptDelete   = id => request.delete(`/system/dept/${id}`)

export const logPage      = p => request.get('/system/log/page', { params: p })
export const logClear     = () => request.delete('/system/log/clear')

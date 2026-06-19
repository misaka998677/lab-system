import request from '@/utils/request'

export const login    = d => request.post('/auth/login', d)
export const register = d => request.post('/auth/register', d)
export const logout   = () => request.post('/auth/logout')
export const getInfo  = () => request.get('/auth/info')
export const getMenus = () => request.get('/auth/menus')
export const captcha  = () => request.get('/auth/captcha', { responseType: 'blob' })

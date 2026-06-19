import request from '@/utils/request'

export const reservePage = p => request.get('/reserve/page', { params: p })
export const reserveMine = p => request.get('/reserve/mine', { params: p })
export const reserveCheckRecords = p => request.get('/reserve/check-records', { params: p })
export const reserveDetail = id => request.get(`/reserve/${id}`)
export const reserveApply = d => request.post('/reserve', d)
export const reserveUpdate = d => request.put('/reserve', d)
export const reserveCancel = id => request.put(`/reserve/${id}/cancel`)
export const reserveAudit = (id, params) => request.put(`/reserve/${id}/audit`, null, { params })
export const reserveCheckIn = id => request.put(`/reserve/${id}/check-in`)
export const reserveCheckOut = id => request.put(`/reserve/${id}/check-out`)

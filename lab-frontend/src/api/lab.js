import request from '@/utils/request'

export const roomPage   = p => request.get('/lab/room/page', { params: p })
export const roomAll    = () => request.get('/lab/room/all')
export const roomDetail = id => request.get(`/lab/room/${id}`)
export const roomCreate = d => request.post('/lab/room', d)
export const roomUpdate = d => request.put('/lab/room', d)
export const roomDelete = id => request.delete(`/lab/room/${id}`)

export const devicePage   = p => request.get('/lab/device/page', { params: p })
export const deviceDetail = id => request.get(`/lab/device/${id}`)
export const deviceCreate = d => request.post('/lab/device', d)
export const deviceUpdate = d => request.put('/lab/device', d)
export const deviceDelete = id => request.delete(`/lab/device/${id}`)
export const deviceStatus = (id, s) => request.put(`/lab/device/${id}/status/${s}`)
export const deviceExport = () => request.get('/lab/device/export', { responseType: 'blob' })
export const deviceTemplate = () => request.get('/lab/device/template', { responseType: 'blob' })
export const deviceImport = file => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/lab/device/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const repairPage  = p => request.get('/lab/repair/page', { params: p })
export const repairReport = d => request.post('/lab/repair/report', d)
export const repairHandle = (id, note, status) =>
  request.put(`/lab/repair/${id}/handle`, null, { params: { handlerId: '', note: note || '', status } })
export const repairDelete = id => request.delete(`/lab/repair/${id}`)
export const repairMine = p => request.get('/lab/repair/mine', { params: p })

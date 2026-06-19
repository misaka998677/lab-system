import request from '@/utils/request'

export const stockItemPage     = p => request.get('/stock/item/page', { params: p })
export const stockItemWarnings = p => request.get('/stock/item/warnings', { params: p })
export const stockItemDetail   = id => request.get(`/stock/item/${id}`)
export const stockItemCreate   = d => request.post('/stock/item', d)
export const stockItemUpdate   = d => request.put('/stock/item', d)
export const stockItemDelete   = id => request.delete(`/stock/item/${id}`)
export const stockItemTemplate = () => request.get('/stock/item/template', { responseType: 'blob' })
export const stockItemImport   = file => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/stock/item/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const stockItemExport   = () => request.get('/stock/item/export', { responseType: 'blob' })

export const stockRecordPage   = p => request.get('/stock/record/page', { params: p })
export const stockRecordIn     = d => request.post('/stock/record/in', d)
export const stockRecordOut    = d => request.post('/stock/record/out', d)
export const stockRecordExport = () => request.get('/stock/record/export', { responseType: 'blob' })

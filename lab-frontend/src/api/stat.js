import request from '@/utils/request'

export const statOverview     = () => request.get('/stat/overview')
export const statUsage        = () => request.get('/stat/lab-usage')
export const statFault        = () => request.get('/stat/device-fault')
export const statStockWarning = () => request.get('/stat/stock-warning')

import request from './request'

// 巡检
export const createInspection = (data) => request.post('/inspection', data)
export const getInspections = (params) => request.get('/inspection', { params })
export const getInspectionById = (id) => request.get(`/inspection/${id}`)
export const getInspectionsByBench = (benchId) => request.get(`/inspection/bench/${benchId}`)

// 维修工单
export const createRepairOrder = (data) => request.post('/repair-order', data)
export const getRepairOrders = (params) => request.get('/repair-order', { params })
export const getRepairOrderById = (id) => request.get(`/repair-order/${id}`)
export const getRepairOrdersByBench = (benchId) => request.get(`/repair-order/bench/${benchId}`)
export const startRepairOrder = (id) => request.put(`/repair-order/${id}/start`)
export const completeRepairOrder = (id, data) => request.put(`/repair-order/${id}/complete`, data)
export const closeRepairOrder = (id) => request.put(`/repair-order/${id}/close`)

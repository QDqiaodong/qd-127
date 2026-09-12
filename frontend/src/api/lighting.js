import request from './request'

// 夜间照明巡查
export const createLightingInspection = (data) => request.post('/lighting', data)
export const getLightingInspections = (params) => request.get('/lighting', { params })
export const getPointLightingStatuses = (params) => request.get('/lighting/point-status', { params })
export const getLightingInspectionsByPoint = (pointId) => request.get(`/lighting/point/${pointId}`)

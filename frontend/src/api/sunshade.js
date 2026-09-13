import request from './request'

// 遮阳棚巡查
export const createSunshadeInspection = (data) => request.post('/sunshade', data)
export const getSunshadeInspections = (params) => request.get('/sunshade', { params })
export const getPointSunshadeStatuses = (params) => request.get('/sunshade/point-status', { params })
export const getSunshadeInspectionsByPoint = (pointId) => request.get(`/sunshade/point/${pointId}`)
// 路段遮阳棚异常汇总（只含有异常点位的路段）
export const getSectionSunshadeSummary = () => request.get('/sunshade/section-summary')

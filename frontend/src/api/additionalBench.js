import request from './request'

export const getAdditionalBenchPlans = (params) => request.get('/additional-bench-plans', { params })
export const createAdditionalBenchPlan = (data) => request.post('/additional-bench-plans', data)
export const getAdditionalBenchPlanDetail = (id) => request.get(`/additional-bench-plans/${id}`)
export const verifyAdditionalBenchPlan = (id, data) => request.post(`/additional-bench-plans/${id}/verify`, data)

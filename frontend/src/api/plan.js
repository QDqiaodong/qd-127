import request from './request'

// 巡检计划
export const getPlans = () => request.get('/inspection-plan')
export const createPlan = (data) => request.post('/inspection-plan', data)
export const updatePlan = (id, data) => request.put(`/inspection-plan/${id}`, data)
export const togglePlan = (id) => request.put(`/inspection-plan/${id}/toggle`)
export const getPlanDetail = (id) => request.get(`/inspection-plan/${id}`)
export const generatePlanTasks = () => request.post('/inspection-plan/generate')

// 巡检任务
export const getPlanTasks = (params) => request.get('/inspection-plan/tasks', { params })
export const getTaskBenches = (taskId) => request.get(`/inspection-plan/tasks/${taskId}/benches`)
export const executeTask = (taskId, data) => request.post(`/inspection-plan/tasks/${taskId}/execute`, data)

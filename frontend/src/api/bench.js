import request from './request'

export const getAllBenches = (params) => request.get('/bench', { params })
export const getBenchById = (id) => request.get(`/bench/${id}`)
export const getBenchesByNode = (nodeId) => request.get(`/bench/node/${nodeId}`)
export const createBench = (data) => request.post('/bench', data)
export const updateBench = (id, data) => request.put(`/bench/${id}`, data)
export const deleteBench = (id) => request.delete(`/bench/${id}`)
export const changeBenchNode = (data) => request.post('/bench/change-node', data)
export const getAllChangeLogs = () => request.get('/bench/change-logs')
export const getChangeLogs = (id) => request.get(`/bench/${id}/change-logs`)
export const exportAssets = (sectionId) => request.get(`/bench/export/${sectionId}`)

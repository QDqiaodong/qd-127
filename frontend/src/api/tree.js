import request from './request'

export const getTree = () => request.get('/tree')
export const getNodesByLevel = (level) => request.get(`/tree/level/${level}`)
export const getChildren = (parentId) => request.get(`/tree/children/${parentId}`)
export const getNodeById = (id) => request.get(`/tree/${id}`)
export const createNode = (data) => request.post('/tree', data)
export const updateNode = (id, data) => request.put(`/tree/${id}`, data)
export const deleteNode = (id) => request.delete(`/tree/${id}`)

import request from './request'

// 雨天防滑垫领用台账
// 点位领用台账（每点位一行，含领出/归还/破损/未还汇总），outstanding=true 只看未还清
export const getAntiSlipMatLedgers = (params) => request.get('/anti-slip-mat/point-ledgers', { params })
// 领出登记
export const issueAntiSlipMat = (data) => request.post('/anti-slip-mat/issue', data)
// 归还登记（归还数量 = 完好归还 + 破损）
export const returnAntiSlipMat = (data) => request.post('/anti-slip-mat/return', data)
// 领用/归还流水，可按点位、动作类型、关键字筛选
export const getAntiSlipMatRecords = (params) => request.get('/anti-slip-mat/records', { params })
// 单个点位的领用/归还流水（台账详情）
export const getAntiSlipMatRecordsByPoint = (pointId) =>
  request.get(`/anti-slip-mat/point/${pointId}/records`)

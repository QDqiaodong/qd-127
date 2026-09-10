// 巡检结果
export const INSPECTION_RESULT = {
  NORMAL: 1,
  ABNORMAL: 0
}

// 严重程度
export const SEVERITY = {
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3
}

// 工单状态
export const ORDER_STATUS = {
  PENDING: 1,
  IN_PROGRESS: 2,
  COMPLETED: 3,
  CLOSED: 4
}

export const severityLabel = (v) => ({ 1: '低', 2: '中', 3: '高' }[v] || '-')
export const severityTagType = (v) => ({ 1: 'info', 2: 'warning', 3: 'danger' }[v] || 'info')

export const orderStatusLabel = (v) =>
  ({ 1: '待处理', 2: '维修中', 3: '已完成', 4: '已关闭' }[v] || '-')
export const orderStatusTagType = (v) =>
  ({ 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }[v] || 'info')

export const inspectionResultLabel = (v) => {
  if (v === 1) return '正常'
  if (v === 0) return '异常'
  return '未巡检'
}
export const inspectionResultTagType = (v) => {
  if (v === 1) return 'success'
  if (v === 0) return 'danger'
  return 'info'
}

export const PROBLEM_TYPES = [
  '结构松动',
  '椅面开裂',
  '漆面破损',
  '金属锈蚀',
  '螺丝缺失',
  '椅腿不稳',
  '靠背损坏',
  '其他'
]

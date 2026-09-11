// 巡检周期
export const CYCLE_TYPE = {
  DAILY: 1,
  WEEKLY: 2,
  MONTHLY: 3
}

export const cycleTypeLabel = (v) => ({ 1: '每天', 2: '每周', 3: '每月' }[v] || '-')

// 巡检任务状态（逾期由后端按 计划日期<今天 推导）
export const TASK_STATUS = {
  PENDING: 1,
  EXECUTED: 2,
  OVERDUE: 3
}

export const taskStatusLabel = (task) => {
  if (task.status === 2) return '已执行'
  if (task.overdue) return '逾期'
  return '待执行'
}

export const taskStatusTagType = (task) => {
  if (task.status === 2) return 'success'
  if (task.overdue) return 'danger'
  return 'warning'
}

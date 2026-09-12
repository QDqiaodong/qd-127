// 点位容量状态（与后端 fillCapacityStatus 口径一致：剩余0已满，剩余≤阈值为将满）
export const CAPACITY_STATUS = {
  FULL: 'FULL',
  NEARLY_FULL: 'NEARLY_FULL'
}

export const capacityStatusLabel = (s) => {
  if (s === CAPACITY_STATUS.FULL) return '已满'
  if (s === CAPACITY_STATUS.NEARLY_FULL) return '将满'
  return '充足'
}

export const capacityStatusTagType = (s) => {
  if (s === CAPACITY_STATUS.FULL) return 'danger'
  if (s === CAPACITY_STATUS.NEARLY_FULL) return 'warning'
  return 'success'
}

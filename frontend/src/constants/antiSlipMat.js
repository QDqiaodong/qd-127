// 防滑垫领用/归还动作类型
export const ANTI_SLIP_MAT_ACTION = {
  ISSUE: 1,
  RETURN: 2
}

export const antiSlipMatActionLabel = (v) => {
  if (v === 1) return '领出'
  if (v === 2) return '归还'
  return '-'
}

export const antiSlipMatActionTagType = (v) => {
  if (v === 1) return 'warning'
  if (v === 2) return 'success'
  return 'info'
}

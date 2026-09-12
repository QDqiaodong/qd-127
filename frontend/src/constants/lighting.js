// 夜间照明结论
export const LIGHTING_RESULT = {
  INTACT: 1,
  ABNORMAL: 0
}

export const lightingResultLabel = (v) => {
  if (v === 1) return '完好'
  if (v === 0) return '异常'
  return '未巡查'
}

export const lightingResultTagType = (v) => {
  if (v === 1) return 'success'
  if (v === 0) return 'danger'
  return 'info'
}

// 照明异常类型（异常时必须点名其一）
export const LIGHTING_PROBLEM_TYPES = ['缺灯', '损坏']

// 遮阳棚巡查结论
export const SUNSHADE_RESULT = {
  INTACT: 1,
  ABNORMAL: 0
}

export const sunshadeResultLabel = (v) => {
  if (v === 1) return '完好'
  if (v === 0) return '异常'
  return '未巡查'
}

export const sunshadeResultTagType = (v) => {
  if (v === 1) return 'success'
  if (v === 0) return 'danger'
  return 'info'
}

// 破损面积展示（平方米），空值显示 -
export const formatDamagedArea = (v) => {
  if (v === null || v === undefined || v === '') return '-'
  const num = Number(v)
  return Number.isNaN(num) ? '-' : `${num} ㎡`
}

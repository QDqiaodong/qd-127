// 点位封闭相关展示工具：后端 LocalDateTime 以 ISO-8601（含 T）返回，统一截取到分钟展示

const pad = (n) => String(n).padStart(2, '0')

/**
 * 把后端返回的时间字符串（如 2026-09-13T10:00:00 或 2026-09-13 10:00:00）
 * 格式化为 yyyy-MM-dd HH:mm；空值返回 '-'
 */
export const formatDateTimeMinute = (value) => {
  if (!value) return '-'
  const normalized = String(value).replace('T', ' ')
  const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2})[ ]?(\d{2}):(\d{2})/)
  if (!match) return normalized.slice(0, 16)
  return `${match[1]}-${match[2]}-${match[3]} ${match[4]}:${match[5]}`
}

/**
 * 封闭期文案：封闭开始 至 封闭结束
 */
export const closurePeriodText = (point) => {
  if (!point) return ''
  return `${formatDateTimeMinute(point.closedStartAt)} 至 ${formatDateTimeMinute(point.closedEndAt)}`
}

/**
 * Date 对象格式化为后端 LocalDateTime 可解析的 yyyy-MM-dd HH:mm:ss
 */
export const formatDateTimeForApi = (d) => {
  const date = d instanceof Date ? d : new Date(d)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

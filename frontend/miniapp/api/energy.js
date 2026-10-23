import http from '@/common/request.js'

/**
 * 月用电趋势
 * @param {string} year - 年份（yyyy），默认当年
 */
export function getMonthlyElectricTrend(year, meterId) {
  return http({
    url: '/api/v1/miniapp/energy-analysis/monthly-electric-trend',
    method: 'GET',
    data: { year, ...(meterId ? { meterId } : {}) }
  })
}

/**
 * 日用电趋势
 * @param {string} month - 月份（yyyy-MM），默认当月
 */
export function getDailyElectricTrend(month, meterId) {
  return http({
    url: '/api/v1/miniapp/energy-analysis/daily-electric-trend',
    method: 'GET',
    data: { month, ...(meterId ? { meterId } : {}) }
  })
}

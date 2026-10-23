import http from '@/common/request.js'

// ==================== 仪表盘模块 ====================

/**
 * 获取仪表盘数据（首页顶部概览）
 * @returns {Promise<{customerName, electricBalance, electricEstimatedDays, waterBalance, waterEstimatedDays, todayElectricUsage, last30DaysElectricUsage, yesterdayWaterUsage, lastMonthWaterUsage}>}
 */
export function getDashboard(meterId) {
  return http({
    url: '/api/v1/miniapp/dashboard',
    method: 'GET',
    data: meterId ? { meterId } : {}
  })
}

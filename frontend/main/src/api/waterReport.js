import http from './request.js'

/**
 * 统计报表（水表） API
 * 对应后端 WaterMeterReportController: /api/v1/water-meter-report
 */

// ═══════════════════════════════════════════
// 用水量日报表
// ═══════════════════════════════════════════

/**
 * 用水量日报表明细（按水表维度分页）
 * 响应 data = { list: WaterDailyUsageVO[], total: number }
 * @param {Object} params
 * @param {number}  params.pageNum     - 页码，默认 1
 * @param {number}  params.pageSize    - 每页条数，默认 20
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID（非ADMIN强制覆盖）
 * @param {string}  params.merchantName - 商户名称（模糊搜索）
 * @param {string}  params.deviceNo    - 设备编号
 */
export function getWaterDailyDetail(params) {
  return http.get('/v1/water-meter-report/daily', { params })
}

/**
 * 用水量日报表汇总（按日聚合）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID
 * @param {string}  params.deviceNo    - 设备编号
 */
export function getWaterDailySummary(params) {
  return http.get('/v1/water-meter-report/daily-summary', { params })
}

// ═══════════════════════════════════════════
// 用水量月报表
// ═══════════════════════════════════════════

/**
 * 用水量月报表明细（按水表维度分页）
 * 响应 data = { list: WaterMonthlyUsageVO[], total: number }
 * @param {Object} params
 * @param {number}  params.pageNum     - 页码，默认 1
 * @param {number}  params.pageSize    - 每页条数，默认 20
 * @param {string}  params.statMonth   - 统计月份 YYYY-MM
 * @param {string}  params.statYear    - 统计年份 YYYY
 * @param {number}  params.merchantId  - 商户ID（非ADMIN强制覆盖）
 * @param {string}  params.merchantName - 商户名称（模糊搜索）
 * @param {string}  params.deviceNo    - 设备编号
 */
export function getWaterMonthlyDetail(params) {
  return http.get('/v1/water-meter-report/monthly', { params })
}

/**
 * 用水量月报表汇总（按月聚合）
 * @param {Object} params
 * @param {string}  params.statMonth   - 统计月份 YYYY-MM
 * @param {number}  params.merchantId  - 商户ID
 * @param {string}  params.deviceNo    - 设备编号
 */
export function getWaterMonthlySummary(params) {
  return http.get('/v1/water-meter-report/monthly-summary', { params })
}

// ═══════════════════════════════════════════
// 用电量月报表（删除水表）
// ═══════════════════════════════════════════

/**
 * 用水量月报表（已删除水表）
 * @param {Object} params
 * @param {string}  params.statMonth    - 统计月份 YYYY-MM
 * @param {number}  params.merchantId   - 商户ID（非ADMIN强制覆盖）
 * @param {string}  params.merchantName - 商户名称（模糊搜索）
 * @param {string}  params.deviceNo     - 设备编号
 */
export function getWaterDeletedMeterMonthly(params) {
  return http.get('/v1/water-meter-report/deleted-meter-monthly', { params })
}

// ═══════════════════════════════════════════
// 全园区月度/年度用水量汇总
// ═══════════════════════════════════════════

/**
 * 全园区月度用水量汇总
 * @param {Object} params
 * @param {string}  params.statYear    - 统计年份 YYYY
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID（非ADMIN强制覆盖）
 */
export function getWaterCampusMonthlySummary(params) {
  return http.get('/v1/water-meter-report/campus-monthly-summary', { params })
}

/**
 * 全园区年度用水量汇总
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID（非ADMIN强制覆盖）
 */
export function getWaterCampusYearlySummary(params) {
  return http.get('/v1/water-meter-report/campus-yearly-summary', { params })
}

// ═══════════════════════════════════════════
// 计费水量报表
// ═══════════════════════════════════════════

/**
 * 计费水量汇总表（按天聚合，含平均单价）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID（非ADMIN强制覆盖）
 */
export function getWaterBillingDaily(params) {
  return http.get('/v1/water-meter-report/billing/daily', { params })
}

/**
 * 计费水量月报表（按月聚合，含平均单价）
 * @param {Object} params
 * @param {string}  params.statMonth   - 统计月份 YYYY-MM
 * @param {string}  params.statYear    - 统计年份 YYYY
 * @param {number}  params.merchantId  - 商户ID（非ADMIN强制覆盖）
 */
export function getWaterBillingMonthly(params) {
  return http.get('/v1/water-meter-report/billing/monthly', { params })
}

/**
 * 计费水量年报表（按年聚合，含平均单价）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID（非ADMIN强制覆盖）
 */
export function getWaterBillingYearly(params) {
  return http.get('/v1/water-meter-report/billing/yearly', { params })
}

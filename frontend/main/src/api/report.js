import http from './request.js'

/**
 * 统计报表（电表） API
 * 对应后端 ReportController: /api/v1/report
 *
 * 后端 VO 字段说明：
 * - DailyElectricityVO: reportDate, meterId, customerId, customerName, groupId,
 *     electricityConsumed, peakConsumed, busyConsumed, normalConsumed, idleConsumed, chargeAmount
 * - ReportSummaryVO:  period, totalConsumed, meterCount
 * - TimeOfUseReportVO: reportDate, period, totalConsumed, peakConsumed, busyConsumed,
 *     normalConsumed, idleConsumed, meterCount
 * - BillingReportVO:  period, totalChargeAmount, totalConsumed, meterCount, averagePrice
 * - CustomerComprehensiveVO: customerId, customerName, totalRecharge, totalConsumed,
 *     totalCharge, currentBalance
 * - ElectricLoadVO: period, maxLoad, averageLoad, minLoad, loadRate, dayCount, meterCount, totalConsumed
 */

// ═══════════════════════════════════════════
// 用电量日报表
// ═══════════════════════════════════════════

/**
 * 用电量日报表明细（分页）
 * 按设备维度查询每日用电量明细
 * @param {Object} params
 * @param {number}  params.pageNum     - 页码，默认 1
 * @param {number}  params.pageSize    - 每页条数，默认 20
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID（非ADMIN强制覆盖）
 * @param {number}  params.groupId     - 分组ID
 */
export function getDailyElectricity(params) {
  return http.get('/v1/report/daily-electricity', { params })
}

/**
 * 用电量日报汇总（按日聚合）
 * 按日聚合总用电量，支持总用户/分组/分户三维筛选
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getDailySummary(params) {
  return http.get('/v1/report/daily-summary', { params })
}

// ═══════════════════════════════════════════
// 用电量月报表
// ═══════════════════════════════════════════

/**
 * 用电量月报表汇总（按月聚合）
 * 按月聚合总用电量，支持总用户/分组/分户三维筛选
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getMonthlySummary(params) {
  return http.get('/v1/report/monthly-summary', { params })
}

/**
 * 月度用电量汇总明细（按表计×月度维度）
 * 包含正向/反向有功电量起止示数、总起止示数、余额等完整字段
 * @param {Object} params
 * @param {string}  params.statMonth  - 统计月份 yyyy-MM
 * @param {number}  params.customerId - 商户ID
 * @param {number}  params.groupId    - 分组ID
 */
export function getMonthlyElecSummary(params) {
  return http.get('/v1/report/monthly-elec-summary', { params })
}

// ═══════════════════════════════════════════
// 用电量月报表明细（按表计维度展开每日用量）
// ═══════════════════════════════════════════

/**
 * 用电量月报表明细（按表计维度展开每日用量）
 * 返回各表计的维度信息 + 每日用量 Map，前端动态生成日列
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getMonthlyDetail(params) {
  return http.get('/v1/report/monthly-detail', { params })
}

// ═══════════════════════════════════════════
// 用电量年报表
// ═══════════════════════════════════════════

/**
 * 用电量年报表汇总（按年聚合）
 * 按年聚合总用电量，支持总用户/分组/分户三维筛选
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getYearlySummary(params) {
  return http.get('/v1/report/yearly-summary', { params })
}

// ═══════════════════════════════════════════
// 复费率分时报表
// ═══════════════════════════════════════════

/**
 * 复费率计费电量日报表（按日聚合尖/峰/平/谷分时用电量）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getTimeOfUseDaily(params) {
  return http.get('/v1/report/time-of-use/daily', { params })
}

/**
 * 复费率计费电量日报表明细（按meter×date维度展开，含分时电费）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getTimeOfUseDailyDetail(params) {
  return http.get('/v1/report/time-of-use/daily-detail', { params })
}

/**
 * 复费率计费电量汇总表（按月聚合尖/峰/平/谷分时用电量）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getTimeOfUseMonthly(params) {
  return http.get('/v1/report/time-of-use/monthly', { params })
}

// ═══════════════════════════════════════════
// 计费报表（财务核费视角）
// ═══════════════════════════════════════════

/**
 * 计费电量汇总表（按日聚合，含平均电价）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getBillingDaily(params) {
  return http.get('/v1/report/billing/daily', { params })
}

/**
 * 计费电量月报表（按月聚合，含平均电价）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getBillingMonthly(params) {
  return http.get('/v1/report/billing/monthly', { params })
}

/**
 * 计费电量年报表（按年聚合，含平均电价）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getBillingYearly(params) {
  return http.get('/v1/report/billing/yearly', { params })
}

/**
 * 计费电量年报明细（按表计维度+年度/本月汇总宽表）
 * 数据来源：t_report_daily_electricity + tb_meter_monthly_report
 * @param {Object} params
 * @param {string}  params.year        - 统计年份 yyyy（默认当前年份）
 * @param {string}  params.statMonth   - 统计月份 yyyy-MM（默认当前月份）
 * @param {number}  params.merchantId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getBillingYearlyDetail(params) {
  return http.get('/v1/meter-report/billing-yearly-detail', { params })
}

// ═══════════════════════════════════════════
// 客户综合报表
// ═══════════════════════════════════════════

/**
 * 客户综合报表
 * 汇总每个商户的总充值、总用量、总费用、当前余额
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID（ADMIN可查全量，非ADMIN强制覆盖）
 */
export function getCustomerComprehensive(params) {
  return http.get('/v1/report/customer-comprehensive', { params })
}

// ═══════════════════════════════════════════
// 用电负荷
// ═══════════════════════════════════════════

/**
 * 查询用电负荷
 * 按日/月维度查询最大负荷、平均负荷、最小负荷及负荷率
 * @param {Object} params
 * @param {string}  params.granularity  - 聚合粒度：daily / monthly，默认 daily
 * @param {string}  params.startDate    - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate      - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId   - 商户ID（非ADMIN强制覆盖）
 * @param {number}  params.groupId      - 分组ID
 */
export function getElectricLoad(params) {
  return http.get('/v1/report/electric-load', { params })
}

/**
 * 用电负荷曲线图
 * 按日返回总用电量 + 尖峰平谷分时用电量，用于曲线图 / 趋势分析
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 * @returns {Promise<ResultListElectricLoadCurveVO>}
 *   data[].date, totalLoad, peakLoad, busyLoad, normalLoad, idleLoad, meterCount
 */
export function getElectricLoadCurve(params) {
  return http.get('/v1/report/electric-load-curve', { params })
}

// ═══════════════════════════════════════════
// 向后兼容别名（旧视图文件引用）
// ═══════════════════════════════════════════

/** @deprecated 请使用 getDailyElectricity */
export const getElecDaily = getDailyElectricity

/** @deprecated 请使用 getMonthlySummary */
export const getElecMonthly = getMonthlySummary

/** @deprecated 请使用 getMonthlySummary */
export const getElecMonthlySum = getMonthlySummary

/** @deprecated 请使用 getYearlySummary */
export const getElecYearlySum = getYearlySummary

/** @deprecated 请使用 getTimeOfUseDaily */
export const getElecTouDaily = getTimeOfUseDaily

/** @deprecated 请使用 getTimeOfUseMonthly */
export const getElecTouSum = getTimeOfUseMonthly

/** @deprecated 请使用 getBillingDaily */
export const getElecBDaily = getBillingDaily

/** @deprecated 请使用 getBillingMonthly */
export const getElecBMonthly = getBillingMonthly

/** @deprecated 请使用 getBillingYearly */
export const getElecBYearly = getBillingYearly

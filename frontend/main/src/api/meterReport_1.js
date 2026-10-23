import http from './request.js'

/**
 * 表计综合报表 API（水+电统一）
 * 对应后端 MeterReportController: /api/v1/meter-report
 *
 * 后端 VO 字段说明：
 * - MeterDailyReportVO:   statDate, projectName, groupName, merchantName, deviceNo,
 *     meterType, totalUsage, peakUsage, busyUsage, normalUsage, idleUsage,
 *     projectId, groupId, merchantId, meterId
 * - MeterMonthlyReportVO: statMonth, projectName, groupName, merchantName, deviceNo,
 *     meterType, totalUsage, peakUsage, busyUsage, normalUsage, idleUsage, dailyCount,
 *     projectId, groupId, merchantId, meterId
 * - MeterYearlyReportVO:  statYear, projectName, groupName, merchantName, deviceNo,
 *     meterType, totalUsage, peakUsage, busyUsage, normalUsage, idleUsage, monthCount,
 *     projectId, groupId, merchantId, meterId
 * - DeletedMeterReportVO: statMonth, projectId, projectName, groupId, groupName,
 *     merchantId, merchantName, deviceNo, meterId, meterType,
 *     totalUsage, peakUsage, busyUsage, normalUsage, idleUsage, deletedTime, dailyCount, dataSource
 * - BillingReportVO:      period, totalChargeAmount, totalConsumed, meterCount, averagePrice
 */

// ═══════════════════════════════════════════
// 日报表
// ═══════════════════════════════════════════

/**
 * 表计日报表明细（分页）
 * 按设备维度查询每日用电量明细
 * @param {Object} params
 * @param {number}  params.pageNum     - 页码，默认 1
 * @param {number}  params.pageSize    - 每页条数，默认 20
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID（非ADMIN强制覆盖）
 * @param {number}  params.groupId     - 分组ID
 */
export function getDailyReport(params) {
  return http.get('/v1/report/daily-electricity', { params })
}

/**
 * 表计日报表汇总（按日聚合）
 * 按日汇总总用电量
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
// 月报表
// ═══════════════════════════════════════════

/**
 * 表计月报表明细（按表计维度展开每日用量）
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.customerId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getMonthlyReport(params) {
  return http.get('/v1/report/monthly-detail', { params })
}

/**
 * 表计月报表汇总（按月聚合）
 * 按月汇总总用量 + 尖峰平谷分时用量
 * @param {Object} params
 * @param {string}  params.statMonth     - 统计月份 YYYY-MM
 * @param {number}  params.projectId     - 项目ID
 * @param {number}  params.groupId       - 分组ID
 * @param {number}  params.merchantId    - 商户ID
 * @param {string}  params.deviceNo      - 设备号
 * @param {string}  params.meterType     - 表计类型：ELECTRIC/WATER
 */
export function getMonthlySummary(params) {
  return http.get('/v1/meter-report/monthly-summary', { params })
}

// ═══════════════════════════════════════════
// 年度用电量汇总（按表计×12月明细）
// ═══════════════════════════════════════════

/**
 * 年度用电量汇总表（按表计×12月明细）
 * 返回各表计在指定年份1~12月用电量分列及小计
 * @param {Object} params
 * @param {string}  params.year        - 统计年份 yyyy
 * @param {number}  params.merchantId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getYearlyDetail(params) {
  return http.get('/v1/meter-report/yearly-detail', { params })
}

// ═══════════════════════════════════════════
// 年报表
// ═══════════════════════════════════════════

/**
 * 表计年报表汇总（按年聚合）
 * 按年汇总总用量 + 尖峰平谷分时用量
 * @param {Object} params
 * @param {string}  params.startDate     - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate       - 结束日期 yyyy-MM-dd
 * @param {number}  params.projectId     - 项目ID
 * @param {number}  params.groupId       - 分组ID
 * @param {number}  params.merchantId    - 商户ID
 * @param {string}  params.deviceNo      - 设备号
 * @param {string}  params.meterType     - 表计类型：ELECTRIC/WATER
 */
export function getYearlySummary(params) {
  return http.get('/v1/meter-report/yearly-summary', { params })
}

// ═══════════════════════════════════════════
// 计费电量汇总（按表计维度）
// ═══════════════════════════════════════════

/**
 * 计费电量汇总表（按表计维度）
 * 按表计聚合指定日期范围内的计费金额与用电量，含平均电价和虚拟表信息
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getBillingMeterSummary(params) {
  return http.get('/v1/meter-report/billing/meter-summary', { params })
}

/**
 * 计费电量月报表明细（按表计+每日计费）
 * 按表计维度展开当月每日的计费电量与计费金额
 * @param {Object} params
 * @param {string}  params.startDate   - 开始日期 yyyy-MM-dd
 * @param {string}  params.endDate     - 结束日期 yyyy-MM-dd
 * @param {number}  params.merchantId  - 商户ID
 * @param {number}  params.groupId     - 分组ID
 */
export function getBillingMonthlyMeterDetail(params) {
  return http.get('/v1/meter-report/billing/monthly-meter-detail', { params })
}

// ═══════════════════════════════════════════
// 已删除表计月报表
// ═══════════════════════════════════════════

/**
 * 已删除表计月报表
 * 查询已逻辑删除的电表/水表在指定月份的用能汇总
 * @param {Object} params
 * @param {string}  params.statMonth     - 统计月份 YYYY-MM
 * @param {number}  params.projectId     - 项目ID
 * @param {number}  params.groupId       - 分组ID
 * @param {number}  params.merchantId    - 商户ID
 * @param {string}  params.merchantName  - 商户名称（模糊搜索）
 * @param {string}  params.deviceNo      - 设备号
 * @param {string}  params.meterType     - 表计类型：ELECTRIC/WATER
 */
export function getDeletedMeterMonthly(params) {
  return http.get('/v1/meter-report/deleted-meter-monthly', { params })
}

// ═══════════════════════════════════════════
// 计费报表（水+电统一，财务核费视角）
// ═══════════════════════════════════════════

/**
 * 计费汇总表（按日聚合，含平均单价）
 * @param {Object} params
 * @param {string}  params.startDate     - 开始日期
 * @param {string}  params.endDate       - 结束日期
 * @param {number}  params.merchantId    - 商户ID
 * @param {string}  params.meterType     - 表计类型：ELECTRIC/WATER
 */
export function getBillingDaily(params) {
  return http.get('/v1/meter-report/billing/daily', { params })
}

/**
 * 计费月报表（按月聚合，含平均单价）
 * @param {Object} params
 * @param {string}  params.startDate     - 开始日期
 * @param {string}  params.endDate       - 结束日期
 * @param {number}  params.merchantId    - 商户ID
 * @param {string}  params.meterType     - 表计类型：ELECTRIC/WATER
 */
export function getBillingMonthly(params) {
  return http.get('/v1/meter-report/billing/monthly', { params })
}

/**
 * 计费年报表（按年聚合，含平均单价）
 * @param {Object} params
 * @param {string}  params.startDate     - 开始日期
 * @param {string}  params.endDate       - 结束日期
 * @param {number}  params.merchantId    - 商户ID
 * @param {string}  params.meterType     - 表计类型：ELECTRIC/WATER
 */
export function getBillingYearly(params) {
  return http.get('/v1/meter-report/billing/yearly', { params })
}

// ═══════════════════════════════════════════
// 导出接口（已移除，后端不再提供 /v1/meter-report/export/* 端点）
// 请使用各视图内置的 downloadXLSX 工具导出
// ═══════════════════════════════════════════

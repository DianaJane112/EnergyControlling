import http from './request.js'
import axios from 'axios'

// ══════════════════════════════════════════════════════════════════════════
//  即时账单模块 API
//  TAG: 业务办理 - 即时账单
//  Base: /api/v1/property/instant-bill
// ══════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  查询 & 统计
// ──────────────────────────────────────────────────────────────────────

/**
 * 即时账单分页列表
 * GET /api/v1/property/instant-bill/list
 * @param {Object} params - InstantBillPageRequest
 * @param {number} [params.pageNum] - 页码（默认1）
 * @param {number} [params.pageSize] - 每页条数（默认20）
 * @param {number} [params.customerId] - 商户ID
 * @param {string} [params.meterNo] - 表号（模糊匹配）
 * @param {string} [params.customerName] - 商户名称（模糊匹配）
 * @param {number} [params.categoryId] - 账单分类ID
 * @param {string} [params.startTime] - 开始时间
 * @param {string} [params.endTime] - 结束时间
 * @param {string} [params.billStatus] - 账单状态：UNPAID-未缴, PAID-已缴, OVERDUE-逾期
 * @param {number} [params.minAmount] - 最小账单金额
 * @param {number} [params.maxAmount] - 最大账单金额
 * @returns {Promise<ResultIPageInstantBillListVO>}
 *   data.records[].meterId, meterName, meterNo, customerId, customerName,
 *     currentReading, lastSettledReading, consumption, multiplier,
 *     unitPrice, billAmount, currentBalance, meterType(0水/1电),
 *     status(0停用/1运行/2欠费停用), calculateTime
 */
export function getInstantBillList(params = {}) {
  return http.get('/v1/property/instant-bill/list', { params })
}

/**
 * 即时账单统计概览
 * GET /api/v1/property/instant-bill/stats
 * @param {Object} params - InstantBillPageRequest（同筛选条件）
 * @returns {Promise<ResultMapStringObject>}
 */
export function getInstantBillStats(params = {}) {
  return http.get('/v1/property/instant-bill/stats', { params })
}

/**
 * 单表实时账单详情
 * GET /api/v1/property/instant-bill/{meterId}
 * @param {number} meterId - 表计ID
 * @returns {Promise<ResultInstantBillVO>}
 *   data.meterId, meterNo, customerId, customerName,
 *     currentReading, lastSettledReading, consumption,
 *     multiplier, unitPrice, billAmount, currentBalance, calculateTime
 */
export function getInstantBillDetail(meterId) {
  return http.get(`/v1/property/instant-bill/${meterId}`)
}

// ──────────────────────────────────────────────────────────────────────
//  批量操作
// ──────────────────────────────────────────────────────────────────────

/**
 * 批量生成账单
 * POST /api/v1/property/instant-bill/batch-create
 * @param {Object} data - BatchBillCreateRequest
 * @param {number[]} data.meterIds - 表计ID列表（必填）
 * @param {number} [data.categoryId] - 账单分类ID
 * @param {string} [data.billDescription] - 账单说明
 * @param {number} [data.fixedAmount] - 固定金额（元），为空则实时计算
 * @param {string} [data.operatorName] - 操作人
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultBatchOperationResponse>}
 *   data.total, data.success, data.failed, data.message, data.errors[]
 */
export function batchCreateBills(data) {
  return http.post('/v1/property/instant-bill/batch-create', data)
}

/**
 * 批量导入账单
 * POST /api/v1/property/instant-bill/batch-import
 * @param {Object} data - BillImportRequest
 * @param {Object[]} data.bills - 导入账单列表
 * @param {string} data.bills[].meterNo - 表号
 * @param {string} [data.bills[].customerCode] - 商户编码
 * @param {string} [data.bills[].customerName] - 商户名称
 * @param {number} [data.bills[].billAmount] - 账单金额
 * @param {number} [data.bills[].consumption] - 用量
 * @param {number} [data.bills[].unitPrice] - 单价
 * @param {string} [data.bills[].remark] - 备注
 * @param {number} [data.categoryId] - 账单分类ID
 * @param {string} [data.operatorName] - 操作人
 * @param {number} [data.meterId] - 表计ID
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchImportBills(data) {
  return http.post('/v1/property/instant-bill/batch-import', data)
}

/**
 * 批量延期
 * PUT /api/v1/property/instant-bill/batch-defer
 * @param {Object} data - BatchDeferRequest
 * @param {number[]} data.meterIds - 表计ID列表（必填）
 * @param {string} data.deferTo - 延期至时间（必填，如 "2026-07-01 00:00:00"）
 * @param {string} [data.deferReason] - 延期原因
 * @param {string} [data.operatorName] - 操作人
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchDeferBills(data) {
  return http.put('/v1/property/instant-bill/batch-defer', data)
}

/**
 * 批量删除账单
 * DELETE /api/v1/property/instant-bill/batch-delete
 * @param {number[]} ids - 表计ID列表
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchDeleteBills(ids) {
  return http.delete('/v1/property/instant-bill/batch-delete', { data: ids })
}

// ──────────────────────────────────────────────────────────────────────
//  导出 & 模板
// ──────────────────────────────────────────────────────────────────────

/**
 * 导出即时账单 Excel
 * GET /api/v1/property/instant-bill/export
 * @param {Object} params - InstantBillPageRequest（同筛选条件）
 * @returns {Promise<Blob>} Excel 文件流
 */
export async function exportInstantBillList(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/property/instant-bill/export', {
    params,
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const blob = resp.data
  const disposition = resp.headers['content-disposition'] || ''
  let filename = '即时账单.xlsx'
  const match = disposition.match(/filename\*?=(?:UTF-8''|")?([^";\n]+)/)
  if (match) filename = decodeURIComponent(match[1])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * 下载批量导入模板
 * GET /api/v1/property/instant-bill/template/download
 * @returns {Promise<Blob>} Excel 模板文件流
 */
export async function downloadBillTemplate() {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/property/instant-bill/template/download', {
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const blob = resp.data
  const disposition = resp.headers['content-disposition'] || ''
  let filename = '批量账单模板.xlsx'
  const match = disposition.match(/filename\*?=(?:UTF-8''|")?([^";\n]+)/)
  if (match) filename = decodeURIComponent(match[1])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// ══════════════════════════════════════════════════════════════════════════
//  缴费轨迹（计费轨迹）模块 API
//  Base: /api/v1/property/billing-track
// ══════════════════════════════════════════════════════════════════════════

/**
 * 缴费轨迹分页查询
 * GET /api/v1/property/billing-track/list
 * @param {Object} params - BillingTrackPageRequest
 * @param {number} [params.pageNum] - 页码（默认1）
 * @param {number} [params.pageSize] - 每页条数（默认20）
 * @param {number} [params.customerId] - 商户ID
 * @param {number} [params.meterId] - 表计ID
 * @param {number} [params.tradeType] - 交易类型：1-充值, 2-消费扣费, 3-退费, 4-冲正, 5-人工缴费, 6-账单缴费, 7-公共分摊
 * @param {string} [params.startTime] - 开始时间
 * @param {string} [params.endTime] - 结束时间
 * @param {string} [params.keyword] - 关键词（流水号/表号模糊匹配）
 * @returns {Promise<ResultIPageBillingTrackVO>}
 *   data.records[].id, flowNo, balanceBefore, changeAmount, balanceAfter,
 *     tradeType, tradeTypeName, bizNo, operatorName, operateTime, remark
 */
export function getBillingTrackList(params = {}) {
  return http.get('/v1/property/billing-track/list', { params })
}

/**
 * 导出缴费轨迹 Excel
 * GET /api/v1/property/billing-track/export
 * @param {Object} params - BillingTrackPageRequest（同筛选条件）
 * @returns {Promise<Blob>} Excel 文件流
 */
export async function exportBillingTrackList(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/property/billing-track/export', {
    params,
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const blob = resp.data
  const disposition = resp.headers['content-disposition'] || ''
  let filename = '缴费轨迹.xlsx'
  const match = disposition.match(/filename\*?=(?:UTF-8''|")?([^";\n]+)/)
  if (match) filename = decodeURIComponent(match[1])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

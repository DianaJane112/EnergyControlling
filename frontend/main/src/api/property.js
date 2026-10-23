import http from './request.js'
import axios from 'axios'

// ═══════════════════════════════════════════════════════════════════════════════
// 物业服务 - 即时账单 API
// 后端 Controller: PropertyController  ->  /api/v1/property
// ═══════════════════════════════════════════════════════════════════════════════

// ── 生成即时账单（单表计）────────────────────────────────────────────────────
// GET /api/v1/property/instant-bill/{meterId}
// 返回: Result<InstantBillVO>
// InstantBillVO: meterId, meterNo, customerId, customerName, currentReading,
//   lastSettledReading, consumption, multiplier, unitPrice, billAmount,
//   currentBalance, calculateTime
export function generateInstantBill(meterId) {
  return http.get(`/v1/property/instant-bill/${meterId}`)
}

// ── 即时账单分页列表 ─────────────────────────────────────────────────────────
// GET /api/v1/property/instant-bill/list
// 请求参数 InstantBillPageRequest:
//   pageNum, pageSize, customerId, meterNo, customerName, categoryId,
//   startTime, endTime, billStatus (UNPAID/PAID/OVERDUE), minAmount, maxAmount
// 返回: Result<IPage<InstantBillListVO>>
// InstantBillListVO: meterId, meterName, meterNo, customerId, customerName,
//   currentReading, lastSettledReading, consumption, multiplier, unitPrice,
//   billAmount, currentBalance, meterType (0-水表,1-电表),
//   status (0-停用,1-正常运行,2-欠费停机), calculateTime
export function listInstantBills(params = {}) {
  return http.get('/v1/property/instant-bill/list', { params })
}

// ── 即时账单统计汇总 ─────────────────────────────────────────────────────────
// GET /api/v1/property/instant-bill/stats
// 请求参数: InstantBillPageRequest (同分页列表)
// 返回: Result<Map<String, Object>> — 汇总统计数据
export function getInstantBillStats(params = {}) {
  return http.get('/v1/property/instant-bill/stats', { params })
}

// ── 导出即时账单列表（Excel）─────────────────────────────────────────────────
// GET /api/v1/property/instant-bill/export
// 请求参数: InstantBillPageRequest (同分页列表)
// 返回: Excel Blob
export function exportInstantBills(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/property/instant-bill/export', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 批量创建账单 ─────────────────────────────────────────────────────────────
// POST /api/v1/property/instant-bill/batch-create
// 请求体 BatchBillCreateRequest:
//   meterIds: Long[] (必填), categoryId: Long, billDescription: String,
//   fixedAmount: BigDecimal, operatorName: String, remark: String
// 返回: Result<BatchOperationResponse>
// BatchOperationResponse: total, success, failed, message, errors
export function batchCreateBills(data = {}) {
  return http.post('/v1/property/instant-bill/batch-create', data)
}

// ── 批量延期账单 ─────────────────────────────────────────────────────────────
// PUT /api/v1/property/instant-bill/batch-defer
// 请求体 BatchDeferRequest:
//   billIds: Long[] (必填), deferTo: LocalDateTime (必填),
//   deferReason: String, operatorName: String
// 返回: Result<BatchOperationResponse>
export function batchDeferBills(data = {}) {
  return http.put('/v1/property/instant-bill/batch-defer', data)
}

// ── 批量删除账单 ─────────────────────────────────────────────────────────────
// DELETE /api/v1/property/instant-bill/batch-delete
// 请求体: List<Long> billIds
// 返回: Result<BatchOperationResponse>
export function batchDeleteBills(billIds = []) {
  return http.delete('/v1/property/instant-bill/batch-delete', { data: billIds })
}

// ── 批量导入账单 ─────────────────────────────────────────────────────────────
// POST /api/v1/property/instant-bill/batch-import
// 请求体 BillImportRequest:
//   bills: BillImportItem[] { meterNo, customerCode, customerName,
//     billAmount, consumption, unitPrice, remark },
//   categoryId: Long, operatorName: String, meterId: Long
// 返回: Result<BatchOperationResponse>
export function batchImportBills(data = {}) {
  return http.post('/v1/property/instant-bill/batch-import', data)
}

// ── 下载批量导入模板（Excel）─────────────────────────────────────────────────
// GET /api/v1/property/instant-bill/template/download
// 返回: Excel Blob
export function downloadImportTemplate() {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/property/instant-bill/template/download', {
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 缴费轨迹分页查询 ─────────────────────────────────────────────────────────
// GET /api/v1/property/billing-track/list
// 请求参数 BillingTrackPageRequest:
//   pageNum, pageSize, customerId, meterId, tradeType
//   (1-充值,2-消费扣费,3-退费,4-调账,5-人工缴费,6-余额清零,7-公共分摊),
//   startTime, endTime, keyword
// 返回: Result<IPage<BillingTrackVO>>
// BillingTrackVO: id, flowNo, balanceBefore, changeAmount, balanceAfter,
//   tradeType, tradeTypeName, bizNo, operatorName, operateTime, remark
export function listBillingTracks(params = {}) {
  return http.get('/v1/property/billing-track/list', { params })
}

// ── 导出缴费轨迹（Excel）────────────────────────────────────────────────────
// GET /api/v1/property/billing-track/export
// 请求参数: BillingTrackPageRequest (同轨迹查询)
// 返回: Excel Blob
export function exportBillingTracks(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/property/billing-track/export', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

import http from './request.js'
import axios from 'axios'

// ══════════════════════════════════════════════════════════════════════════
//  账单记录管理 API
//  TAG: 即时账单 - 账单记录管理
//  Base: /api/v1/bill
// ══════════════════════════════════════════════════════════════════════════

// ── 查询 ────────────────────────────────────────────────────────────────

/**
 * 账单记录分页列表
 * GET /api/v1/bill/page
 */
export function getBillList(params = {}) {
  return http.get('/v1/bill/page', { params })
}

/**
 * 账单详情
 * GET /api/v1/bill/{id}
 */
export function getBillDetail(id) {
  return http.get(`/v1/bill/${id}`)
}

// ── 编辑 ────────────────────────────────────────────────────────────────

/**
 * 编辑账单（修改支付状态等）
 * PUT /api/v1/bill/{id}
 */
export function updateBill(id, data) {
  return http.put(`/v1/bill/${id}`, data)
}

// ── 删除 ────────────────────────────────────────────────────────────────

/**
 * 删除单条账单
 * DELETE /api/v1/bill/{id}
 */
export function deleteBill(id) {
  return http.delete(`/v1/bill/${id}`)
}

/**
 * 批量删除账单
 * POST /api/v1/bill/batch-delete
 */
export function batchDeleteBills(ids) {
  return http.post('/v1/bill/batch-delete', ids)
}

// ── 导出 ────────────────────────────────────────────────────────────────

/**
 * 导出账单列表 Excel
 * GET /api/v1/bill/export
 */
export async function exportBillList(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/bill/export', {
    params,
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const blob = resp.data
  const disposition = resp.headers['content-disposition'] || ''
  let filename = '账单记录.xlsx'
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

import http from './request.js'
import axios from 'axios'

// ══════════════════════════════════════════════════════════════════════════
//  商户信息模块 API
//  TAG: 电表/水能管理 - 项目信息 - 商户信息
//  Base: /api/v1/merchant
// ══════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  查询
// ──────────────────────────────────────────────────────────────────────

/**
 * 商户列表分页查询
 * GET /api/v1/merchant/page
 * @param {Object} params
 * @param {number} params.pageNum - 页码（从1开始）
 * @param {number} params.pageSize - 每页条数
 * @param {string} [params.customerName] - 商户名称（模糊查询）
 * @param {string} [params.customerCode] - 用户编号
 * @param {string} [params.areaName] - 小区名称
 * @param {string} [params.customerType] - 用户类型
 * @param {number} [params.status] - 状态：0-停用，1-启用
 * @returns {Promise<ResultIPageCustomerResponse>}
 */
export function getMerchantList(params = {}) {
  return http.get('/v1/merchant/page', { params })
}

/**
 * 导出商户信息 Excel 文件
 * GET /api/v1/merchant/export
 * @param {Object} params - 同分页查询的筛选参数
 * @returns {Promise<Blob>} Excel 文件流
 */
export async function exportMerchantList(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/merchant/export', {
    params,
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const blob = resp.data
  const disposition = resp.headers['content-disposition'] || ''
  let filename = '商户信息.xlsx'
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

// ──────────────────────────────────────────────────────────────────────
//  创建 & 修改
// ──────────────────────────────────────────────────────────────────────

/**
 * 商户创建
 * POST /api/v1/merchant/create
 * @param {Object} data - CustomerCreateRequest
 * @param {string} data.customerCode - 用户编号（必填）
 * @param {string} data.customerName - 用户名称/商户名（必填）
 * @param {string} [data.areaName] - 小区名称
 * @param {string} [data.address] - 地址
 * @param {string} [data.phone] - 用户电话
 * @param {string} [data.customerType] - 用户类型
 * @param {string} [data.useNature] - 用水性质/用电性质
 * @param {string} [data.type] - 业务类型：0电/1水/2气/3热
 * @param {string} [data.operator] - 运营商
 * @param {string} [data.sign] - 签订
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultCustomerResponse>}
 */
export function createMerchant(data) {
  return http.post('/v1/merchant/create', data)
}

/**
 * 商户信息修改
 * PUT /api/v1/merchant/update
 * @param {number} id - 商户ID（query 参数）
 * @param {Object} data - CustomerUpdateRequest
 * @param {string} [data.customerName] - 用户名称/商户名
 * @param {string} [data.areaName] - 小区名称
 * @param {string} [data.address] - 地址
 * @param {string} [data.phone] - 用户电话
 * @param {string} [data.customerType] - 用户类型
 * @param {string} [data.useNature] - 用水性质/用电性质
 * @param {string} [data.operator] - 运营商
 * @param {string} [data.sign] - 签订
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultCustomerResponse>}
 */
export function updateMerchant(id, data) {
  return http.put('/v1/merchant/update', data, { params: { id } })
}

// ──────────────────────────────────────────────────────────────────────
//  状态 & 注销
// ──────────────────────────────────────────────────────────────────────

/**
 * 商户状态变更
 * PUT /api/v1/merchant/{id}/status
 * @param {number} id - 商户ID
 * @param {number} status - 状态：0-停用，1-启用
 * @returns {Promise<ResultVoid>}
 */
export function updateMerchantStatus(id, status) {
  return http.put(`/v1/merchant/${id}/status`, { status })
}

/**
 * 商户注销（逻辑删除，isDeleted 置为 1）
 * DELETE /api/v1/merchant/{id}
 * @param {number} id - 商户ID
 * @returns {Promise<ResultVoid>}
 */
export function deleteMerchant(id) {
  return http.delete(`/v1/merchant/${id}`)
}

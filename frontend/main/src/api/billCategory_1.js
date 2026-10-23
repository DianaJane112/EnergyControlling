import http from './request.js'

// ══════════════════════════════════════════════════════════════════════════
//  账单分类模块 API
//  TAG: 即时账单 - 账单分类
//  Base: /api/v1/bill-category
// ══════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  查询
// ──────────────────────────────────────────────────────────────────────

/**
 * 分页查询账单分类列表
 * GET /api/v1/bill-category/page
 * @param {Object} params
 * @param {number} [params.pageNum] - 页码（从1开始，默认1）
 * @param {number} [params.pageSize] - 每页条数（默认20）
 * @param {string} [params.keyword] - 关键词（模糊匹配分类名称/编码）
 * @returns {Promise<ResultIPageBillCategory>}
 */
export function getBillCategoryList(params = {}) {
  return http.get('/v1/bill-category/page', { params })
}

/** @deprecated 别名，同 getBillCategoryList */
export const listBillCategories = getBillCategoryList

/**
 * 账单分类详情
 * GET /api/v1/bill-category/{id}
 * @param {number} id - 分类ID
 * @returns {Promise<ResultBillCategory>}
 */
export function getBillCategoryDetail(id) {
  return http.get(`/v1/bill-category/${id}`)
}

/** @deprecated 别名，同 getBillCategoryDetail */
export const getBillCategory = getBillCategoryDetail

/**
 * 获取所有启用的账单分类（下拉列表）
 * GET /api/v1/bill-category/enabled
 * @returns {Promise<ResultListBillCategory>}
 */
export function getEnabledBillCategories() {
  return http.get('/v1/bill-category/enabled')
}

/** @deprecated 别名，同 getEnabledBillCategories */
export const listEnabledBillCategories = getEnabledBillCategories

// ──────────────────────────────────────────────────────────────────────
//  创建 & 修改
// ──────────────────────────────────────────────────────────────────────

/**
 * 创建账单分类
 * POST /api/v1/bill-category
 * @param {Object} data - BillCategoryRequest
 * @param {string} data.categoryName - 分类名称（必填）
 * @param {string} data.categoryCode - 分类编码（必填，唯一）
 * @param {string} [data.description] - 分类描述
 * @param {number} [data.sortOrder] - 排序号
 * @param {number} [data.status] - 状态：0-停用，1-启用（默认1）
 * @returns {Promise<ResultBillCategory>}
 */
export function createBillCategory(data) {
  return http.post('/v1/bill-category', data)
}

/**
 * 编辑账单分类
 * PUT /api/v1/bill-category/{id}
 * @param {number} id - 分类ID
 * @param {Object} data - BillCategoryRequest（字段同 createBillCategory）
 * @returns {Promise<ResultBillCategory>}
 */
export function updateBillCategory(id, data) {
  return http.put(`/v1/bill-category/${id}`, data)
}

// ──────────────────────────────────────────────────────────────────────
//  删除
// ──────────────────────────────────────────────────────────────────────

/**
 * 删除账单分类（逻辑删除）
 * DELETE /api/v1/bill-category/{id}
 * @param {number} id - 分类ID
 * @returns {Promise<ResultVoid>}
 */
export function deleteBillCategory(id) {
  return http.delete(`/v1/bill-category/${id}`)
}

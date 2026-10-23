import http from './request.js'

// ══════════════════════════════════════════════════════════════════════════
//  银行账户设置模块 API
//  TAG: 即时账单 - 银行账户设置
//  Base: /api/v1/bank-account
// ══════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  查询
// ──────────────────────────────────────────────────────────────────────

/**
 * 分页查询银行账户列表
 * GET /api/v1/bank-account/page
 * @param {Object} params
 * @param {number} [params.pageNum] - 页码（从1开始，默认1）
 * @param {number} [params.pageSize] - 每页条数（默认20）
 * @param {string} [params.keyword] - 关键词（模糊匹配银行名称/账户名称/账号）
 * @returns {Promise<ResultIPageBankAccount>}
 */
export function getBankAccountList(params = {}) {
  return http.get('/v1/bank-account/page', { params })
}

/** @deprecated 别名，同 getBankAccountList */
export const listBankAccounts = getBankAccountList

/**
 * 银行账户详情
 * GET /api/v1/bank-account/{id}
 * @param {number} id - 账户ID
 * @returns {Promise<ResultBankAccount>}
 */
export function getBankAccountDetail(id) {
  return http.get(`/v1/bank-account/${id}`)
}

/** @deprecated 别名，同 getBankAccountDetail */
export const getBankAccount = getBankAccountDetail

/**
 * 获取所有启用的银行账户（下拉列表）
 * GET /api/v1/bank-account/enabled
 * @returns {Promise<ResultListBankAccount>}
 */
export function getEnabledBankAccounts() {
  return http.get('/v1/bank-account/enabled')
}

/** @deprecated 别名，同 getEnabledBankAccounts */
export const listEnabledBankAccounts = getEnabledBankAccounts

// ──────────────────────────────────────────────────────────────────────
//  创建 & 修改
// ──────────────────────────────────────────────────────────────────────

/**
 * 创建银行账户
 * POST /api/v1/bank-account
 * @param {Object} data - BankAccountRequest
 * @param {string} data.bankName - 银行名称（必填）
 * @param {string} data.accountName - 账户名称（必填）
 * @param {string} data.accountNumber - 银行账号（必填）
 * @param {string} [data.bankBranch] - 开户支行
 * @param {string} [data.bankCode] - 联行号
 * @param {string} [data.accountType] - 账户类型：COMPANY-对公账户, PERSONAL-个人账户
 * @param {number} [data.isDefault] - 是否默认账户：0-否, 1-是
 * @param {string} [data.contactPerson] - 联系人
 * @param {string} [data.contactPhone] - 联系电话
 * @param {string} [data.remark] - 备注
 * @param {number} [data.status] - 状态：0-停用，1-启用（默认1）
 * @returns {Promise<ResultBankAccount>}
 */
export function createBankAccount(data) {
  return http.post('/v1/bank-account', data)
}

/**
 * 编辑银行账户
 * PUT /api/v1/bank-account/{id}
 * @param {number} id - 账户ID
 * @param {Object} data - BankAccountRequest（字段同 createBankAccount）
 * @returns {Promise<ResultBankAccount>}
 */
export function updateBankAccount(id, data) {
  return http.put(`/v1/bank-account/${id}`, data)
}

// ──────────────────────────────────────────────────────────────────────
//  删除
// ──────────────────────────────────────────────────────────────────────

/**
 * 删除银行账户（逻辑删除）
 * DELETE /api/v1/bank-account/{id}
 * @param {number} id - 账户ID
 * @returns {Promise<ResultVoid>}
 */
export function deleteBankAccount(id) {
  return http.delete(`/v1/bank-account/${id}`)
}

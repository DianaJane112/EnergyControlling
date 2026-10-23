import http from './request.js'
import axios from 'axios'

// ── 充值明细（财务管理） ────────────────────────────────────────────
export function getDepositLog(params = {}) {
  return http.get('/v1/financial/deposit-log', { params })
}

// ── 充值明细收据打印 ──────────────────────────────────────────────
export function getDepositLogPrint(id) {
  return http.get(`/v1/financial/deposit-log/${id}/print`)
}

// ── 充值明细导出 ──────────────────────────────────────────────────
export function exportDepositLog(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/financial/export/deposit-log', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 充值汇总统计（按日/月聚合） ────────────────────────────────────
export function getDepositSummary(params = {}) {
  return http.get('/v1/financial/deposit-summary', { params })
}

// ── 充值汇总导出 ──────────────────────────────────────────────────
export function exportDepositSummary(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/financial/export/deposit-summary', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 分账统计汇总 ──────────────────────────────────────────────────
export function getAccountFlowSummary(params = {}) {
  return http.get('/v1/account-flow/summary', { params })
}

// ── 分账明细分页查询 ──────────────────────────────────────────────
export function getAccountFlowList(params = {}) {
  return http.get('/v1/account-flow/list', { params })
}

// ── 分账统计导出 ──────────────────────────────────────────────────
export function exportAccountFlow(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/account-flow/export', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 分账统计汇总导出 ────────────────────────────────────────────────
export function exportAccountFlowSummary(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/account-flow/export-summary', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 退费明细分页查询 ──────────────────────────────────────────────
export function getRefundDetailList(params = {}) {
  return http.get('/v1/refund/detail-list', { params })
}

// ── 退费明细详情 ──────────────────────────────────────────────────
export function getRefundDetail(id) {
  return http.get(`/v1/refund/detail/${id}`)
}

// ── 退费明细导出 ──────────────────────────────────────────────────
export function exportRefundLog(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/refund/export-detail', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 退费记录（财务管理 - 退费日志） ──────────────────────────────────
export function getRefundLogList(params = {}) {
  return http.get('/v1/financial/refund-log', { params })
}

// ── 退费日志导出 ────────────────────────────────────────────────────
export function exportFinancialRefundLog(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/financial/export/refund-log', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── 余额退补统一执行 ──────────────────────────────────────────────
export function executeBalanceAdjustment(data = {}) {
  return http.post('/v1/balance-adjustment/execute', data)
}

// ── 余额调整记录分页查询 ────────────────────────────────────────────
export function getBalanceAdjustmentList(params = {}) {
  return http.get('/v1/balance-adjustment/list', { params })
}

// ── 余额调整记录详情 ────────────────────────────────────────────────
export function getBalanceAdjustmentDetail(id) {
  return http.get(`/v1/balance-adjustment/${id}`)
}

// ── 余额调整统计聚合 ────────────────────────────────────────────────
export function getBalanceAdjustmentSummary(params = {}) {
  return http.get('/v1/balance-adjustment/summary', { params })
}

// ── 余额调整记录导出 ────────────────────────────────────────────────
export function exportBalanceAdjustment(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/balance-adjustment/export', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}


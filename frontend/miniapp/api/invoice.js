import http from '@/common/request.js'

// ==================== 票据管理模块 ====================

/**
 * 票据详情
 * @param {number} invoiceId - 票据ID
 */
export function getInvoiceDetail(invoiceId) {
  return http({
    url: `/api/v1/miniapp/invoice/${invoiceId}`,
    method: 'GET'
  })
}

/**
 * 票据列表（分页）
 * @param {Object} params
 * @param {number} [params.pageNum=1] - 页码
 * @param {number} [params.pageSize=20] - 每页条数
 * @param {string} [params.invoiceStatus] - 开票状态: NOT_ISSUED/ISSUED，不传查全部
 */
export function getInvoiceList({ pageNum = 1, pageSize = 20, invoiceStatus } = {}) {
  const data = { pageNum, pageSize }
  if (invoiceStatus) {
    data.invoiceStatus = invoiceStatus
  }
  return http({
    url: '/api/v1/miniapp/invoice/list',
    method: 'GET',
    data
  })
}

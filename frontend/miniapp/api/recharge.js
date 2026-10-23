import http from '@/common/request.js'

// ==================== 充值缴费模块 ====================

/**
 * 提交充值申请
 * @param {number} meterId - 表计ID
 * @param {number} amount - 充值金额（元），必须大于0
 * @param {string} [payMethod='VIRTUAL'] - 支付方式
 */
export function submitRecharge(meterId, amount, payMethod = 'VIRTUAL') {
  return http({
    url: '/api/v1/miniapp/recharge',
    method: 'POST',
    data: { meterId, amount, payMethod }
  })
}

/**
 * 充值申请详情
 * @param {number} requestId - 申请ID
 */
export function getRechargeDetail(requestId) {
  return http({
    url: `/api/v1/miniapp/recharge/${requestId}`,
    method: 'GET'
  })
}

/**
 * 充值历史（分页）
 * @param {Object} params
 * @param {number} [params.pageNum=1] - 页码
 * @param {number} [params.pageSize=20] - 每页条数
 * @param {string} [params.auditStatus] - 审核状态: PENDING/APPROVED/REJECTED，不传查全部
 */
export function getRechargeHistory({ pageNum = 1, pageSize = 20, auditStatus } = {}) {
  return http({
    url: '/api/v1/miniapp/recharge/history',
    method: 'GET',
    data: { pageNum, pageSize, auditStatus }
  })
}

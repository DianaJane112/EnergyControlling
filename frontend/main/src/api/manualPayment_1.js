import http from './request.js'

// ══════════════════════════════════════════════════════════════════════════
//  人工缴费模块 API
//  TAG: 客服收费 - 人工缴费
//  Base: /api/v1/charge, /api/v1/billing
// ══════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  人工缴费 & 退费（表计级）
// ──────────────────────────────────────────────────────────────────────

/**
 * 人工缴费
 * POST /api/v1/charge/deposit
 * @param {Object} data - DepositRequest
 * @param {number} [data.meterId] - 表计ID（可选，绑定到具体表计）
 * @param {number} [data.customerId] - 商户ID（ADMIN可指定，非ADMIN强制使用当前登录商户）
 * @param {number} data.amount - 缴费金额（元），必须大于0
 * @param {string} [data.paymentMethod] - 缴费方式：CASH-现金, TRANSFER-转账, WECHAT-微信, ALIPAY-支付宝
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultString>}
 */
export function chargeDeposit(data) {
  return http.post('/v1/charge/deposit', data)
}

/**
 * 人工退费（表计级）
 * POST /api/v1/charge/refund
 * @param {Object} data - ManualRefundRequest
 * @param {number} data.meterId - 表计ID（必填，定位退费来源表计）
 * @param {number} [data.customerId] - 商户ID（ADMIN可指定，非ADMIN强制使用当前登录商户）
 * @param {number} data.amount - 退费金额（元），必须大于0
 * @param {string} data.reason - 退费原因（必填，用于审计追溯）
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultString>}
 */
export function chargeRefund(data) {
  return http.post('/v1/charge/refund', data)
}

/**
 * 退费预览（表计级）
 * POST /api/v1/charge/refund/preview
 * @param {Object} data - RefundPreviewRequest
 * @param {number} data.meterId - 表计ID（必填）
 * @param {number} data.refundAmount - 拟退费金额（元），必须大于0
 * @param {number} [data.adjustedPrice] - 调整后电价（元/kWh），可选
 * @returns {Promise<ResultRefundPreviewVO>}
 *   data.meterId, dbNo, meterAddress, meterType, meterTypeName,
 *     meterStatus, meterStatusName, customerId, customerName, customerPhone,
 *     currentBalance, warnline, credit, currentPrice, priceStrategyName,
 *     refundAmount, balanceAfterRefund, willTriggerShutdown, previewTime
 */
export function previewRefund(data) {
  return http.post('/v1/charge/refund/preview', data)
}

// ──────────────────────────────────────────────────────────────────────
//  商户级 充值 & 退费 & 调账
// ──────────────────────────────────────────────────────────────────────

/**
 * 商户充值
 * POST /api/v1/billing/recharge
 * @param {Object} data - RechargeRequest
 * @param {number} data.customerId - 商户ID（必填）
 * @param {number} data.amount - 充值金额（元），必须大于0
 * @param {string} data.operatorName - 操作人（必填）
 * @param {string} [data.userName] - 用户名/缴费人
 * @param {number} [data.tradeType] - 交易类型：1-充值电费, 2-充值水费
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultString>}
 */
export function rechargeCustomer(data) {
  return http.post('/v1/billing/recharge', data)
}

/**
 * 商户退费
 * POST /api/v1/billing/refund
 * @param {Object} data - RefundRequest
 * @param {number} data.customerId - 商户ID（必填）
 * @param {number} data.amount - 退费金额（元），必须大于0
 * @param {string} data.operatorName - 操作人（必填）
 * @param {string} [data.reason] - 退费原因
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultString>}
 */
export function refundCustomer(data) {
  return http.post('/v1/billing/refund', data)
}

/**
 * 余额调整（调账）
 * POST /api/v1/billing/adjust-balance
 * @param {Object} data - AdjustBalanceRequest
 * @param {number} data.customerId - 商户ID（必填）
 * @param {number} data.amount - 调整金额（元），正数=增加余额，负数=扣减余额，不能为0
 * @param {string} data.operatorName - 操作人（必填）
 * @param {string} data.reason - 调账原因（必填，用于审计）
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultString>}
 */
export function adjustBalance(data) {
  return http.post('/v1/billing/adjust-balance', data)
}

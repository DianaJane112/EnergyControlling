import http from './request.js'
import axios from 'axios'

// ══════════════════════════════════════════════════════════════════════════
//  公共分摊模块 API
//  TAG: 账务流水 - 公共分摊
//  Base: /api/v1/public-allocation
// ══════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  执行 & 查询
// ──────────────────────────────────────────────────────────────────────

/**
 * 执行公共分摊
 * POST /api/v1/public-allocation/execute
 * @param {Object} data - PublicAllocationExecuteRequest
 * @param {number} data.projectId - 项目ID（必填）
 * @param {number} data.sourceMeterId - 总表ID/公区表（必填）
 * @param {number} data.totalAmount - 本次分摊总金额，元（必填，>=0.01）
 * @param {string} data.allocationType - 分摊方式（必填）：PER_HOUSEHOLD-按户均分, BY_USAGE_RATIO-按用量比例分摊
 * @param {number[]} data.targetMeterIds - 参与分摊的目标表ID列表（必填）
 * @param {string} [data.allocationName] - 分摊名称（如：2026年5月公共分摊）
 * @param {string} [data.effectiveDate] - 生效日期
 * @param {string} [data.remark] - 备注
 * @returns {Promise<ResultPublicAllocationResultVO>}
 *   data.allocationId   - 分摊记录ID
 *   data.allocationName - 分摊名称
 *   data.totalAmount    - 分摊总金额
 *   data.allocationType - 分摊方式
 *   data.successCount   - 成功扣费户数
 *   data.failCount      - 失败户数
 *   data.items[]        - 每户分摊明细 [{meterId, meterNo, customerId, customerName,
 *                          allocatedAmount, ratio, balanceBefore, balanceAfter, success, failReason}]
 */
export function executePublicAllocation(data) {
  return http.post('/v1/public-allocation/execute', data)
}

/**
 * 公共分摊列表分页查询
 * GET /api/v1/public-allocation/page
 * @param {Object} params
 * @param {number} params.pageNum - 页码（从1开始）
 * @param {number} params.pageSize - 每页条数
 * @param {string} [params.allocationName] - 分摊名称（模糊查询）
 * @param {string} [params.allocationType] - 分摊方式：PER_HOUSEHOLD / BY_USAGE_RATIO
 * @param {number} [params.status] - 状态：0-未执行，1-已执行，2-已冲销
 * @param {string} [params.startDate] - 生效日期起（含）
 * @param {string} [params.endDate] - 生效日期止（含）
 * @returns {Promise<ResultIPagePublicAllocationResultVO>}
 */
export function getAllocationList(params = {}) {
  return http.get('/v1/public-allocation/page', { params })
}

// ──────────────────────────────────────────────────────────────────────
//  统计
// ──────────────────────────────────────────────────────────────────────

/**
 * 公共分摊统计聚合（按日/月/年）
 * GET /api/v1/public-allocation/summary
 * @param {Object} params
 * @param {string} [params.granularity] - 统计粒度：DAY-按日, MONTH-按月, YEAR-按年
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @param {string} [params.allocationType] - 分摊方式
 * @returns {Promise<ResultListPublicAllocationSummaryVO>}
 *   data[].period              - 统计时段
 *   data[].totalAllocatedAmount- 该时段分摊总金额
 *   data[].allocationCount     - 该时段分摊执行次数
 *   data[].avgAmount           - 该时段平均每次分摊金额
 */
export function getAllocationSummary(params = {}) {
  return http.get('/v1/public-allocation/summary', { params })
}

// ──────────────────────────────────────────────────────────────────────
//  导出
// ──────────────────────────────────────────────────────────────────────

/**
 * 导出公共分摊 Excel
 * GET /api/v1/public-allocation/export
 * @param {Object} params
 * @param {number[]} [params.ids] - 指定分摊记录ID列表（不传则按筛选条件导出全部）
 * @param {string} [params.allocationName] - 分摊名称
 * @param {string} [params.allocationType] - 分摊方式
 * @param {number} [params.status] - 状态：0-未执行，1-已执行，2-已冲销
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @returns {Promise<Blob>} Excel 文件流
 */
export async function exportAllocationList(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/public-allocation/export', {
    params,
    paramsSerializer: (p) => {
      const qs = []
      for (const [k, v] of Object.entries(p)) {
        if (Array.isArray(v)) v.forEach(item => qs.push(`${k}=${encodeURIComponent(item)}`))
        else if (v !== undefined && v !== '') qs.push(`${k}=${encodeURIComponent(v)}`)
      }
      return qs.join('&')
    },
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const blob = resp.data
  const disposition = resp.headers['content-disposition'] || ''
  let filename = '公共分摊.xlsx'
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
 * 导出公共分摊统计 Excel
 * GET /api/v1/public-allocation/export/summary
 * @param {Object} params - 同 getAllocationSummary 参数
 * @param {string} [params.granularity] - DAY / MONTH / YEAR
 * @param {string} [params.startDate] - 开始日期
 * @param {string} [params.endDate] - 结束日期
 * @param {string} [params.allocationType] - 分摊方式
 * @returns {Promise<Blob>} Excel 文件流
 */
export async function exportAllocationSummary(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/public-allocation/export/summary', {
    params,
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const blob = resp.data
  const disposition = resp.headers['content-disposition'] || ''
  let filename = '公共分摊统计.xlsx'
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

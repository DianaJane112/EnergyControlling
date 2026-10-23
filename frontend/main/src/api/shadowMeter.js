import http from './request.js'
import axios from 'axios'

// ══════════════════════════════════════════════════════════════════════════
//  虚拟电表/水表模块 API（孪生电/水表 - 总分表关系管理）
//  TAG: 电能/水能管理 - 项目信息 - 孪生电/水表
//  Base: /api/v1/shadow-meter
// ══════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  分页查询 & 统计
// ──────────────────────────────────────────────────────────────────────

/**
 * 总分表关系分页查询
 * GET /api/v1/shadow-meter/page
 * @param {Object} params
 * @param {number} params.pageNum - 页码（从1开始）
 * @param {number} params.pageSize - 每页条数
 * @param {string} [params.keyword] - 关键词（模糊匹配总表号/分表号/关系描述）
 * @param {number} [params.projectId] - 项目ID
 * @param {number} [params.parentMeterId] - 总表ID（上级表计）
 * @param {number} [params.childMeterId] - 分表ID（下级表计）
 * @param {string} [params.shadowType] - 类型：AUTO-自动创建, MANUAL-手动创建
 * @param {number} [params.status] - 状态：0-停用，1-启用
 * @param {number} [params.meterType] - 表计大类：0-水表, 1-电表, null-全部
 * @returns {Promise<ResultIPageShadowMeterVO>}
 */
export function getShadowMeterList(params = {}) {
  return http.get('/v1/shadow-meter/page', { params })
}

/**
 * 虚拟表计统计概要
 * GET /api/v1/shadow-meter/statistics
 * @returns {Promise<ResultShadowMeterStatisticsVO>}
 *   data.totalCount   - 总分关系总数
 *   data.activeCount  - 启用数量
 *   data.inactiveCount- 停用数量
 *   data.autoCount    - 自动创建数量
 *   data.manualCount  - 手动创建数量
 *   data.byProject    - 按项目分组统计
 *   data.byShadowType - 按类型分组统计
 */
export function getShadowMeterStatistics() {
  return http.get('/v1/shadow-meter/statistics')
}

/**
 * Physical electric meters available for binding to a virtual electric meter.
 * Supports the same keyword/name, meter number and groupId filters as the
 * meter-management page. The backend forces meterType=1 and excludes no data
 * by creating placeholders.
 */
export function getAvailableRealElectricMeters(params = {}) {
  return http.get('/v1/shadow-meter/available-real-meters', { params })
}

export function getAvailableRealWaterMeters(params = {}) {
  return http.get('/v1/shadow-meter/available-real-meters', { params: { ...params, meterType: 0 } })
}

// ──────────────────────────────────────────────────────────────────────
//  关联真实电表
// ──────────────────────────────────────────────────────────────────────

/**
 * 查询指定虚拟电表下关联的真实电表列表
 * GET /api/v1/shadow-meter/{virtualMeterId}/real-meters
 * @param {number} virtualMeterId - 虚拟电表ID（主列表每行的 childMeterId）
 * @returns {Promise<ResultShadowMeterRealMeterVO[]>}
 */
export function getRealMeters(virtualMeterId) {
  return http.get(`/v1/shadow-meter/${virtualMeterId}/real-meters`)
}

/**
 * 为虚拟电表关联真实电表
 * POST /api/v1/shadow-meter/{virtualMeterId}/real-meters
 * @param {number} virtualMeterId - 虚拟电表ID
 * @param {number[]} meterIds - 要关联的真实电表ID列表
 * @returns {Promise<ResultVoid>}
 */
export function bindRealMeters(virtualMeterId, meterIds) {
  return http.post(`/v1/shadow-meter/${virtualMeterId}/real-meters`, { meterIds })
}

/**
 * 解除虚拟电表与真实电表的关联
 * DELETE /api/v1/shadow-meter/real-meters/{relationId}
 * @param {number} relationId - 关联关系ID
 * @returns {Promise<ResultVoid>}
 */
export function unbindRealMeter(relationId) {
  return http.delete(`/v1/shadow-meter/real-meters/${relationId}`)
}

// ──────────────────────────────────────────────────────────────────────
//  创建 & 修改
// ──────────────────────────────────────────────────────────────────────

/**
 * 创建总分表关系（虚拟表计）
 * POST /api/v1/shadow-meter
 * @param {Object} data - ShadowMeterRequest
 * @param {number} data.projectId - 项目ID（必填）
 * @param {number} data.parentMeterId - 总表ID/上级表计（必填）
 * @param {number} data.childMeterId - 分表ID/下级表计（必填）
 * @param {string} data.effectiveDate - 生效日期（必填）
 * @param {string} [data.shadowType] - 类型：AUTO-自动创建, MANUAL-手动创建
 * @param {number} [data.coefficient] - 系数（分表占总表用量比例，默认1.0）
 * @param {string} [data.relationDesc] - 关系描述
 * @param {string} [data.expireDate] - 失效日期（NULL=永久有效）
 * @param {number} [data.status] - 状态：0-停用，1-启用
 * @param {number} [data.groupId] - 分表所属分组ID
 * @param {string} [data.name] - 分表商户名称
 * @param {number} [data.warnline] - 分表告警线（元）
 * @param {number} [data.credit] - 分表透支额度（元）
 * @param {number} [data.feetype] - 分表消费方式：1-预付费, 2-后付费
 * @param {number} [data.ctRatio] - 分表CT变比
 * @param {number} [data.ptRatio] - 分表PT变比
 * @param {string} [data.markStatus] - 分表标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE
 * @param {string} [data.commStatusDesc] - 分表通讯状态描述
 * @param {number} [data.disableWarning] - 分表是否停用告警：0-告警, 1-停用
 * @param {string} [data.address] - 分表安装地址
 * @returns {Promise<ResultShadowMeterVO>}
 */
export function createShadowMeter(data) {
  return http.post('/v1/shadow-meter', data)
}

/**
 * 编辑总分表关系
 * PUT /api/v1/shadow-meter/{id}
 * @param {number} id - 关系ID
 * @param {Object} data - ShadowMeterRequest（字段同 createShadowMeter）
 * @returns {Promise<ResultShadowMeterVO>}
 */
export function updateShadowMeter(id, data) {
  return http.put(`/v1/shadow-meter/${id}`, data)
}

// ──────────────────────────────────────────────────────────────────────
//  查询详情
// ──────────────────────────────────────────────────────────────────────

/**
 * 查询总分表关系详情
 * GET /api/v1/shadow-meter/{id}
 * @param {number} id - 关系ID
 * @returns {Promise<ResultShadowMeterVO>}
 */
export function getShadowMeterDetail(id) {
  return http.get(`/v1/shadow-meter/${id}`)
}

// ──────────────────────────────────────────────────────────────────────
//  删除
// ──────────────────────────────────────────────────────────────────────

/**
 * 删除总分表关系（逻辑删除）
 * DELETE /api/v1/shadow-meter/{id}
 * @param {number} id - 关系ID
 * @returns {Promise<ResultVoid>}
 */
export function deleteShadowMeter(id) {
  return http.delete(`/v1/shadow-meter/${id}`)
}

/**
 * 批量删除总分表关系
 * DELETE /api/v1/shadow-meter/batch
 * @param {number[]} ids - 关系ID数组
 * @returns {Promise<ResultString>}
 */
export function batchDeleteShadowMeters(ids) {
  return http.delete('/v1/shadow-meter/batch', { data: ids })
}

// ──────────────────────────────────────────────────────────────────────
//  分表（子表）相关查询
// ──────────────────────────────────────────────────────────────────────

/**
 * 查询分表计费轨迹（资金流水明细）
 * GET /api/v1/shadow-meter/{childMeterId}/billing-track
 * @param {number} childMeterId - 分表ID
 * @param {Object} [params] - 分页参数
 * @param {number} [params.pageNum] - 页码
 * @param {number} [params.pageSize] - 每页条数
 * @returns {Promise<ResultIPageBillingTrackVO>}
 */
export function getChildBillingTrack(childMeterId, params = {}) {
  return http.get(`/v1/shadow-meter/${childMeterId}/billing-track`, { params })
}

/**
 * 查询分表操作日志
 * GET /api/v1/shadow-meter/{childMeterId}/operation-logs
 * @param {number} childMeterId - 分表ID
 * @param {Object} [params] - 分页参数
 * @param {number} [params.pageNum] - 页码
 * @param {number} [params.pageSize] - 每页条数
 * @returns {Promise<ResultIPageOperationLogVO>}
 */
export function getChildOperationLogs(childMeterId, params = {}) {
  return http.get(`/v1/shadow-meter/${childMeterId}/operation-logs`, { params })
}

/**
 * 获取分表二维码信息
 * GET /api/v1/shadow-meter/{childMeterId}/qrcode
 * @param {number} childMeterId - 分表ID
 * @returns {Promise<ResultMeterResponse>}
 */
export function getChildQrcode(childMeterId) {
  return http.get(`/v1/shadow-meter/${childMeterId}/qrcode`)
}

/**
 * 查询分表充值记录
 * GET /api/v1/shadow-meter/{childMeterId}/recharge-records
 * @param {number} childMeterId - 分表ID
 * @param {Object} [params] - 分页参数
 * @param {number} [params.pageNum] - 页码
 * @param {number} [params.pageSize] - 每页条数
 * @returns {Promise<ResultIPageRechargeRecordVO>}
 */
export function getChildRechargeRecords(childMeterId, params = {}) {
  return http.get(`/v1/shadow-meter/${childMeterId}/recharge-records`, { params })
}

// ──────────────────────────────────────────────────────────────────────
//  导出
// ──────────────────────────────────────────────────────────────────────

/**
 * 批量导出虚拟表计（按ID列表）
 * GET /api/v1/shadow-meter/export-batch
 * @param {number[]} ids - 关系ID数组
 * @returns {Promise} Blob 文件流
 */
export async function exportShadowMeterBatch(ids) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/shadow-meter/export-batch', {
    params: { ids },
    paramsSerializer: { indexes: null },
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const disposition = resp.headers['content-disposition'] || ''
  const match = disposition.match(/filename=(.+)/)
  const filename = match ? decodeURIComponent(match[1]) : `shadow_meters_${Date.now()}.xlsx`
  const url = URL.createObjectURL(resp.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

// ──────────────────────────────────────────────────────────────────────
//  总表损耗/增益分析
// ──────────────────────────────────────────────────────────────────────

/**
 * 总表损耗/增益分析
 * GET /api/v1/shadow-meter/{masterId}/loss-analysis
 *
 * 计算逻辑：
 * 1. 获取总表当月读数
 * 2. 遍历所有分表，获取每台分表当月读数并累加
 * 3. 损耗量 = 总表读数 - 分表读数总和
 * 4. 损耗率 = 损耗量 / 总表读数 × 100%
 *
 * @param {number} masterId - 总表ID
 * @returns {Promise<ResultMasterLossVO>}
 *   data.masterMeterId        - 总表ID
 *   data.masterMeterNo        - 总表编号
 *   data.masterMeterName      - 总表名称
 *   data.masterReading        - 总表当月读数
 *   data.subMeterCount        - 分表数量
 *   data.subMetersTotalReading- 分表读数总和
 *   data.absoluteLoss         - 损耗量
 *   data.lossRatePercent      - 损耗率（%）
 *   data.subMeters            - 各分表明细 [{meterId, meterNo, meterName, reading, coefficient}]
 */
export function getMasterLossAnalysis(masterId) {
  return http.get(`/v1/shadow-meter/${masterId}/loss-analysis`)
}

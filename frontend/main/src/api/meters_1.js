import http from './request.js'
import axios from 'axios'

// ════════════════════════════════════════════════════════════════════════
//  表计管理 API（电表/水表统一入口）
//  对应后端 tag: 电能/水能管理 - 项目信息 - 表计管理
// ════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────
//  基础 CRUD
// ──────────────────────────────────────────────────────────────────────

/**
 * 表计分页列表
 * GET /api/v1/meters
 * @param {Object} params - MeterPageRequest
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页条数
 * @param {string} [params.stealNo] - 积成表号
 * @param {string} [params.dbNo] - 辰鳗表号
 * @param {string} [params.meteraddress] - 捷先表地址
 * @param {string} [params.name] - 表计名称 / 商户名称（模糊匹配）
 * @param {number} [params.customerId] - 商户ID
 * @param {number} [params.projectId] - 项目ID
 * @param {string} [params.vender] - 供应商
 * @param {number} [params.status] - 运行状态：0-停用，1-正常运行
 * @param {number} [params.online] - 在线状态：0-离线，1-在线
 * @param {number} [params.bjlx] - 业务类型：1-租户，2-总表，3-公区，4-楼栋，5-公摊，6-虚拟总表
 * @param {number} [params.meterType] - 表计大类：0-水表，1-电表
 * @param {string} [params.terminalName] - 集中器名称
 * @param {string} [params.markStatus] - 标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE
 * @param {string} [params.commStatusDesc] - 通讯状态描述
 * @param {number} [params.disableWarning] - 停用告警：0-告警，1-停用
 * @param {number} [params.groupId] - 分组ID
 * @returns {Promise<ResultIPageMeterResponse>}
 */
export function getMeterList(params = {}) {
  return http.get('/v1/meters', { params })
}

/**
 * 新增表计
 * POST /api/v1/meters
 * @param {Object} data - MeterRequest
 * @param {number} [data.meterType] - 表计大类：0-水表，1-电表
 * @param {number} [data.customerId] - 商户ID
 * @param {number} [data.groupId] - 分组ID
 * @param {string} [data.name] - 表计名称
 *   === 积成水表字段 (jicheng) ===
 * @param {string} [data.stealNo] - 表号
 * @param {string} [data.vender] - 供应商
 * @param {string} [data.meterTypeName] - 表计类型：公共表/商户/总表
 * @param {string} [data.caliber] - 口径
 * @param {string} [data.switchOp] - 开关阀操作
 * @param {string} [data.startVal] - 初始读数
 * @param {string} [data.terminalName] - 集中器名称
 * @param {string} [data.channelType] - 通道类型：99-
 * @param {string} [data.meterPhone] - 表计手机号
 * @param {string} [data.telecommSn] - 通信号
 * @param {string} [data.imei] - IMEI
 * @param {string} [data.iccid] - ICCID
 * @param {string} [data.address] - 安装地址
 * @param {string} [data.type] - 类型
 *   === 辰鳗电表字段 (chenman) ===
 * @param {string} [data.dbNo] - 表号
 * @param {number} [data.bei] - 倍率
 * @param {number} [data.bjlx] - 业务类型：1-租户，2-总表，3-公区，4-楼栋，5-公摊，6-虚拟总表
 * @param {number} [data.credit] - 信用额度
 * @param {number} [data.warnline] - 告警线
 * @param {string} [data.creatuser] - 创建人
 * @param {string} [data.cttime] - 创建时间
 * @param {number} [data.feetype] - 费用类型
 * @param {number} [data.proId] - 项目ID
 * @param {string} [data.proName] - 项目名称
 * @param {number} [data.plusPrice] - 附加费（元/kWh 或 元/吨）
 * @param {number} [data.priceType] - 价格类型：1-固定单价，2-费率单价，3-阶梯单价
 * @param {string} [data.priceContent] - 价格配置JSON
 * @param {string} [data.joinPhone] - 联系电话
 * @param {string} [data.collectorDeviceNo] - 采集器设备号
 * @param {number} [data.phase] - 相位
 *   === 捷先水表字段 (jiexian) ===
 * @param {string} [data.meteraddress] - 表地址
 * @param {string} [data.companycode] - 公司编码
 * @param {string} [data.customerNo] - 客户编号
 * @param {string} [data.meterCaliber] - 表口径
 * @param {number} [data.controlling] - 控阀状态
 * @param {number} [data.stateValve] - 阀门状态
 * @param {number} [data.signalStrength] - 信号强度
 * @param {number} [data.batteryVoltage] - 电池电压
 *   === 通用字段 (general) ===
 * @param {number} [data.status] - 运行状态：0-停用，1-正常运行
 * @param {number} [data.online] - 在线状态：0-离线，1-在线
 * @param {number} [data.meterSignal] - 表计信号
 * @param {number} [data.ctRatio] - CT变比
 * @param {number} [data.ptRatio] - PT变比
 * @param {string} [data.markStatus] - 标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE
 * @param {string} [data.enabledTime] - 启用时间 (date-time)
 * @param {number} [data.disableWarning] - 停用告警：0-告警，1-停用
 * @param {string} [data.commStatusDesc] - 通讯状态描述
 * @param {number} [data.rateConfigId] - 费率配置ID（关联 t_price_strategy）
 *   === 通讯参数 (commParams) ===
 * @param {string} [data.collectorCode] - 采集器编码
 * @param {string} [data.bpsType] - 波特率类型：2400/9600
 * @param {string} [data.bitType] - 数据位：8
 * @param {string} [data.protoType] - 协议类型：CJ/T188/M-BUS
 * @param {number} [data.serial] - 序号
 * @param {string} [data.deviceType] - 设备类型：WATER/ELEC
 * @returns {Promise<ResultMeterResponse>}
 */
export function createMeter(data) {
  return http.post('/v1/meters', data)
}

/**
 * 查询表计详情
 * GET /api/v1/meters/{id}
 * @param {number} id - 表计ID
 * @returns {Promise<ResultMeterResponse>}
 */
export function getMeterDetail(id) {
  return http.get(`/v1/meters/${id}`)
}

/**
 * 编辑表计
 * PUT /api/v1/meters/{id}
 * @param {number} id - 表计ID
 * @param {Object} data - MeterRequest（字段同 createMeter）
 * @returns {Promise<ResultMeterResponse>}
 */
export function updateMeter(id, data) {
  return http.put(`/v1/meters/${id}`, data)
}

/**
 * 删除表计（逻辑删除）
 * DELETE /api/v1/meters/{id}
 * @param {number} id - 表计ID
 * @returns {Promise<ResultVoid>}
 */
export function deleteMeter(id) {
  return http.delete(`/v1/meters/${id}`)
}

/**
 * 批量新增表计
 * POST /api/v1/meters/batch
 * @param {Object[]} meters - MeterRequest 数组
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchCreateMeters(meters) {
  return http.post('/v1/meters/batch', { meters })
}

// ──────────────────────────────────────────────────────────────────────
//  状态变更 & 阀门控制
// ──────────────────────────────────────────────────────────────────────

/**
 * 表计状态变更
 * PUT /api/v1/meters/{id}/status
 * @param {number} id - 表计ID
 * @param {number} status - 目标状态：0-停用，1-正常运行，2-欠费停用
 * @returns {Promise<ResultVoid>}
 */
export function updateMeterStatus(id, status) {
  return http.put(`/v1/meters/${id}/status`, { status })
}

/**
 * 远程开阀（合闸/通电）
 * PUT /api/v1/meters/{id}/valve/open
 * @param {number} id - 表计ID
 * @param {string} reason - 操作原因（必填）
 * @returns {Promise<ResultVoid>}
 */
export function openValve(id, reason) {
  return http.put(`/v1/meters/${id}/valve/open`, { reason })
}

/**
 * 远程关阀（拉闸/断电）
 * PUT /api/v1/meters/{id}/valve/close
 * @param {number} id - 表计ID
 * @param {string} reason - 操作原因（必填）
 * @returns {Promise<ResultVoid>}
 */
export function closeValve(id, reason) {
  return http.put(`/v1/meters/${id}/valve/close`, { reason })
}

/**
 * 清零余额
 * PUT /api/v1/meters/{id}/clear-balance
 * @param {number} id - 表计ID（由路径参数自动注入，无需手动传）
 * @param {string} reason - 清零原因（必填）
 * @returns {Promise<ResultVoid>}
 */
export function clearBalance(id, reason) {
  return http.put(`/v1/meters/${id}/clear-balance`, { meterId: id, reason })
}

/**
 * 表计绑定商户
 * PUT /api/v1/meters/{id}/bind-customer
 * @param {number} id - 表计ID
 * @param {number} customerId - 商户ID（必填）
 * @returns {Promise<ResultVoid>}
 */
export function bindCustomer(id, customerId) {
  return http.put(`/v1/meters/${id}/bind-customer`, { customerId })
}

// ──────────────────────────────────────────────────────────────────────
//  批量操作
// ──────────────────────────────────────────────────────────────────────

/**
 * 批量修改表计信息
 * PUT /api/v1/meters/batch-update-info
 * @param {Object} data - BatchUpdateMeterRequest
 * @param {number[]} data.meterIds - 表计ID列表（必填）
 * @param {string} [data.name] - 商户名称
 * @param {number} [data.groupId] - 所属分组ID
 * @param {number} [data.bjlx] - 业务类型：1-租户，2-总表，3-公区，4-楼栋，5-公摊，6-虚拟总表
 * @param {number} [data.ctRatio] - CT变比
 * @param {number} [data.ptRatio] - PT变比
 * @param {string} [data.markStatus] - 标记状态：NORMAL/ATTENTION/ABNORMAL/MAINTENANCE
 * @param {number} [data.disableWarning] - 停用告警：0-告警，1-停用
 * @param {string} [data.commStatusDesc] - 通讯状态描述
 * @param {number} [data.status] - 运行状态：0-停用，1-正常运行，2-欠费停用
 * @param {string} [data.address] - 安装地址
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchUpdateMeterInfo(data) {
  return http.put('/v1/meters/batch-update-info', data)
}

/**
 * 批量/全局开关阀（分合闸）
 * PUT /api/v1/meters/batch-valve-control
 * @param {Object} data - BatchValveControlRequest
 * @param {number[]} [data.meterIds] - 表计ID列表（为空+isGlobal=true 表示全局操作）
 * @param {boolean} [data.isGlobal] - 是否全局操作（限管理员）
 * @param {number} [data.customerId] - 按商户ID筛选（与 meterIds 二选一）
 * @param {number} [data.meterType] - 表计大类筛选：0-水表，1-电表，null-全部（全局模式下可不限类型）
 * @param {string} data.action - 操作类型：OPEN-开阀(合闸)，CLOSE-关阀(拉闸)（必填）
 * @param {string} data.reason - 操作原因（必填）
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchValveControl(data) {
  return http.put('/v1/meters/batch-valve-control', data)
}

/**
 * 批量修改水价
 * PUT /api/v1/meters/batch-update-water-price
 * @param {Object} data - BatchPriceRequest
 * @param {number[]} [data.meterIds] - 表计ID列表
 * @param {number} [data.customerId] - 按商户ID修改（与 meterIds 二选一）
 * @param {number} data.priceType - 水价类型：1-固定单价，2-费率单价，3-阶梯单价（必填）
 * @param {string} [data.priceContent] - 价格配置JSON（费率/阶梯单价时使用）
 * @param {number} [data.plusPrice] - 附加费（元/吨）
 * @param {number} [data.rateConfigId] - 费率策略ID（关联 t_price_strategy.id，可选）
 * @returns {Promise<ResultBatchUpdatePriceResponse>}
 */
export function batchUpdateWaterPrice(data) {
  return http.put('/v1/meters/batch-update-water-price', data)
}

/**
 * 批量修改电价
 * PUT /api/v1/meters/batch-update-electricity-price
 * @param {Object} data - BatchPriceRequest（字段同 batchUpdateWaterPrice）
 * @returns {Promise<ResultBatchUpdatePriceResponse>}
 */
export function batchUpdateElectricityPrice(data) {
  return http.put('/v1/meters/batch-update-electricity-price', data)
}

/**
 * 批量/全局设置时段参数
 * PUT /api/v1/meters/batch-set-time-slot
 * @param {Object} data - BatchTimeSlotRequest
 * @param {number[]} [data.meterIds] - 表计ID列表（为空+isGlobal=true 表示全局设置）
 * @param {boolean} [data.isGlobal] - 是否全局设置（限管理员）
 * @param {number} [data.customerId] - 按商户ID设置（可选）
 * @param {number} [data.meterType] - 表计大类筛选：0-水表，1-电表，null-全部（全局模式下可不限类型）
 * @param {number} data.rateConfigId - 费率策略ID（关联 t_price_strategy.id，必填）
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchSetTimeSlot(data) {
  return http.put('/v1/meters/batch-set-time-slot', data)
}

/**
 * 全局修改水价
 * PUT /api/v1/meters/global-update-water-price
 * @param {Object} data - GlobalPriceRequest
 * @param {number} data.priceType - 水价类型：1-固定单价，2-费率单价，3-阶梯单价（必填）
 * @param {string} [data.priceContent] - 价格配置JSON
 * @param {number} [data.plusPrice] - 附加费（元/吨）
 * @param {number} [data.rateConfigId] - 费率策略ID（可选）
 * @returns {Promise<ResultBatchUpdatePriceResponse>}
 */
export function globalUpdateWaterPrice(data) {
  return http.put('/v1/meters/global-update-water-price', data)
}

/**
 * 全局修改电价
 * PUT /api/v1/meters/global-update-electricity-price
 * @param {Object} data - GlobalPriceRequest（字段同 globalUpdateWaterPrice）
 * @returns {Promise<ResultBatchUpdatePriceResponse>}
 */
export function globalUpdateElectricityPrice(data) {
  return http.put('/v1/meters/global-update-electricity-price', data)
}

// ──────────────────────────────────────────────────────────────────────
//  已删除表计
// ──────────────────────────────────────────────────────────────────────

/**
 * 已删除水表列表
 * GET /api/v1/meters/deleted-water-list
 * @param {Object} params - DeletedMeterPageRequest
 * @param {number} [params.pageNum] - 页码（从1开始）
 * @param {number} [params.pageSize] - 每页条数
 * @param {string} [params.keyword] - 关键词（模糊匹配：表号/名称/商户名称）
 * @param {number} [params.status] - 运行状态：0-停用，1-正常运行，2-欠费停用
 * @param {number} [params.groupId] - 分组ID
 * @param {string} [params.startTime] - 删除开始时间（yyyy-MM-dd HH:mm:ss）
 * @param {string} [params.endTime] - 删除截止时间（yyyy-MM-dd HH:mm:ss）
 * @returns {Promise<ResultIPageMeterResponse>}
 */
export function getDeletedWaterList(params = {}) {
  return http.get('/v1/meters/deleted-water-list', { params })
}

/**
 * 已删除水表详情
 * GET /api/v1/meters/deleted-water/{id}
 * @param {number} id - 水表ID
 * @returns {Promise<ResultMeterResponse>}
 */
export function getDeletedWaterDetail(id) {
  return http.get(`/v1/meters/deleted-water/${id}`)
}

/**
 * 已删除电表列表
 * GET /api/v1/meters/deleted-electric-list
 * @param {Object} params - DeletedMeterPageRequest（字段同 getDeletedWaterList）
 * @returns {Promise<ResultIPageMeterResponse>}
 */
export function getDeletedElectricList(params = {}) {
  return http.get('/v1/meters/deleted-electric-list', { params })
}

/**
 * 已删除电表详情
 * GET /api/v1/meters/deleted-electric/{id}
 * @param {number} id - 电表ID
 * @returns {Promise<ResultMeterResponse>}
 */
export function getDeletedElectricDetail(id) {
  return http.get(`/v1/meters/deleted-electric/${id}`)
}

/**
 * 恢复已删除水表（单个）
 * PUT /api/v1/meters/{id}/restore-water
 * @param {number} id - 水表ID
 * @returns {Promise<ResultVoid>}
 */
export function restoreDeletedWater(id) {
  return http.put(`/v1/meters/${id}/restore-water`)
}

/**
 * 恢复已删除电表（单个）
 * PUT /api/v1/meters/{id}/restore-electric
 * @param {number} id - 电表ID
 * @returns {Promise<ResultVoid>}
 */
export function restoreDeletedElectric(id) {
  return http.put(`/v1/meters/${id}/restore-electric`)
}

/**
 * 批量恢复已删除水表
 * PUT /api/v1/meters/batch-restore-water
 * @param {number[]} ids - 水表ID数组
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchRestoreDeletedWater(ids) {
  return http.put('/v1/meters/batch-restore-water', ids)
}

/**
 * 批量恢复已删除电表
 * PUT /api/v1/meters/batch-restore-electric
 * @param {number[]} ids - 电表ID数组
 * @returns {Promise<ResultBatchOperationResponse>}
 */
export function batchRestoreDeletedElectric(ids) {
  return http.put('/v1/meters/batch-restore-electric', ids)
}

// ──────────────────────────────────────────────────────────────────────
//  报表 / 日志 / 导出
// ──────────────────────────────────────────────────────────────────────

/**
 * 表计统计概要
 * GET /api/v1/meters/statistics
 * @returns {Promise<ResultMeterStatisticsVO>}
 *   data.totalCount - 表计总数
 *   data.electricCount - 电表数量
 *   data.waterCount - 水表数量
 *   data.onlineCount - 在线数量
 *   data.offlineCount - 离线数量
 *   data.byStatus - 按运行状态分组统计
 *   data.byBjlx - 按业务类型分组统计
 *   data.byMarkStatus - 按标记状态分组统计
 *   data.byCommStatus - 按通讯状态分组统计
 */
export function getMeterStatistics() {
  return http.get('/v1/meters/statistics')
}

/**
 * 导出表计列表（返回文件流，直接触发下载）
 * GET /api/v1/meters/export
 * @param {Object} params - MeterPageRequest（同 getMeterList 查询参数）
 * @returns {Promise} Blob 文件流
 */
export async function exportMeterList(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/meters/export', {
    params,
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  const disposition = resp.headers['content-disposition'] || ''
  const match = disposition.match(/filename=(.+)/)
  const filename = match ? decodeURIComponent(match[1]) : `meters_${Date.now()}.xlsx`
  const url = URL.createObjectURL(resp.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

/**
 * 水表操作日志（分页）
 * GET /api/v1/meters/{id}/water-operation-logs
 * @param {number} id - 水表ID
 * @param {Object} params - 分页 & 筛选参数
 * @param {number} [params.pageNum] - 页码
 * @param {number} [params.pageSize] - 每页条数
 * @param {string} [params.operationType] - 操作类型筛选：QUERY/CREATE/UPDATE/DELETE/EXPORT
 * @param {string} [params.startTime] - 开始时间（yyyy-MM-dd HH:mm:ss）
 * @param {string} [params.endTime] - 截止时间（yyyy-MM-dd HH:mm:ss）
 * @param {string} [params.keyword] - 关键词（模糊匹配操作描述）
 * @returns {Promise<ResultIPageOperationLogVO>}
 */
export function getWaterOperationLogs(id, params = {}) {
  return http.get(`/v1/meters/${id}/water-operation-logs`, { params })
}

/**
 * 电表操作日志（分页）
 * GET /api/v1/meters/{id}/electric-operation-logs
 * @param {number} id - 电表ID
 * @param {Object} params - 分页 & 筛选参数（同 getWaterOperationLogs）
 * @returns {Promise<ResultIPageOperationLogVO>}
 */
export function getElectricOperationLogs(id, params = {}) {
  return http.get(`/v1/meters/${id}/electric-operation-logs`, { params })
}

/**
 * 批量导出选中表计（按ID列表）
 * GET /api/v1/meters/export-batch
 * @param {number[]} ids - 表计ID数组
 * @returns {Promise} Blob 文件流
 */
export async function exportMeterBatch(ids) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get('/api/v1/meters/export-batch', {
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
  const filename = match ? decodeURIComponent(match[1]) : `meters_batch_${Date.now()}.xlsx`
  const url = URL.createObjectURL(resp.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

// ── 二维码 ──────────────────────────────────────────────────────────

/**
 * 获取表计二维码信息
 * GET /api/v1/meters/{id}/qrcode
 * @param {number} id - 表计ID
 * @returns {Promise<ResultMeterQrCodeVO>}
 *   data.meterId, dbNo, meterName, meterType, meterTypeName, customerName, qrContent
 */
export function getMeterQrCode(id) {
  return http.get(`/v1/meters/${id}/qrcode`)
}

/**
 * 下载表计二维码图片
 * GET /api/v1/meters/{id}/qrcode-image
 * @param {number} id - 表计ID
 * @returns {Promise<Blob>} 二维码图片文件流
 */
export async function getMeterQrCodeImage(id) {
  const token = localStorage.getItem('smalleel_token')
  const resp = await axios.get(`/api/v1/meters/${id}/qrcode-image`, {
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'ngrok-skip-browser-warning': true,
    },
  })
  return URL.createObjectURL(resp.data)
}

// ── 向后兼容别名（DeletedMeterList.vue 引用） ──────────────────────────
export const restoreElectric = restoreDeletedElectric
export const batchRestoreElectric = batchRestoreDeletedElectric
export const restoreWater = restoreDeletedWater
export const batchRestoreWater = batchRestoreDeletedWater

// ════════════════════════════════════════════════════════════════════════
//  Mock 实现（开发调试用，后端暂不可用时使用）
// ════════════════════════════════════════════════════════════════════════

function buildMockRecords() {
  const records = []
  const groups = ['计量', '计费', '电子科大科技园天府园']
  const meterTypes = ['公共表', '商户', '总表']
  const venders = ['青岛积成电子', '春城水表厂', '江苏远传', '宁波水表']
  const statuses = [0, 1]
  const onlines = [0, 1]

  for (let i = 1; i <= 25; i++) {
    const id = 190000000000 + i
    records.push({
      id,
      customerId: 100000 + i,
      customerName: `商户${String(i).padStart(3, '0')}`,
      jicheng: {
        stealNo: `61452000${String(i).padStart(2, '0')}`,
        vender: venders[i % venders.length],
        meterTypeName: meterTypes[i % meterTypes.length],
        caliber: i % 2 === 0 ? '1.6' : '2.0',
        switchOp: i % 2 === 0 ? '1' : '2',
        startVal: String(i * 10),
        terminalName: `集中器${String(i % 5 + 1).padStart(2, '0')}`,
        channelType: i % 3 === 0 ? '99' : '0',
        meterPhone: `1064826176${String(i).padStart(3, '0')}`,
        telecommSn: String(i),
        imei: `0123456789${String(i).padStart(2, '0')}`,
        iccid: `12345${i}`,
        address: `商业用户物联表${i}`,
        createDate: `202${i % 6}-0${(i % 9) + 1}-${String((i % 28) + 1).padStart(2, '0')} 00:00:00`,
        type: '0',
        sign: 'HW123',
      },
      chenman: {
        dbNo: `DB${String(i).padStart(4, '0')}`,
        name: `表计${i}`,
        bei: i % 3 + 1,
        bjlx: i % 2,
        credit: 0,
        warnline: 50,
        creatuser: 'admin',
        cttime: '2025-01-01 00:00:00',
        feetype: 1,
        proId: 1000 + (i % 5),
        proName: groups[i % groups.length],
        plusPrice: 0,
        priceType: 1,
        priceContent: '{}',
        joinPhone: `1380013${String(i).padStart(4, '0')}`,
        collectorDeviceNo: `C3202300${String(i + 3000).padStart(4, '0')}`,
        phase: i % 3 + 1,
      },
      jiexian: {
        meteraddress: `成都市天府新区${i}号`,
        companycode: 'COMP001',
        customerNo: `CUS${String(i).padStart(5, '0')}`,
        meterCaliber: i % 2 === 0 ? '1.6' : '2.0',
        controlling: i % 2,
        stateValve: i % 2,
        signalStrength: 20 + (i % 10),
        batteryVoltage: 3.6,
      },
      general: {
        status: statuses[i % statuses.length],
        online: onlines[i % onlines.length],
        meterSignal: 70 + (i % 20),
        meterType: i % 2,
        rateConfigId: 5000 + i,
        groupId: 2000 + (i % 5),
        ctRatio: 1,
        ptRatio: 1,
        markStatus: 'NORMAL',
        enabledTime: '2025-01-01T00:00:00Z',
        disableWarning: 0,
        commStatusDesc: '正常',
      },
      commParams: {
        collectorCode: `jc0000${String(i % 5 + 1).padStart(2, '0')}`,
        bpsType: i % 2 === 0 ? '2400' : '9600',
        bitType: '8',
        protoType: i % 3 === 0 ? 'CJ/T188' : 'M-BUS',
        serial: i,
        deviceType: i % 2 === 0 ? 'WATER' : 'ELEC',
      },
      createTime: `2025-0${(i % 9) + 1}-${String((i % 28) + 1).padStart(2, '0')}T10:00:00Z`,
      updateTime: `2026-05-${String((i % 28) + 1).padStart(2, '0')}T12:00:00Z`,
      createBy: 'admin',
    })
  }
  return records
}

const ALL_MOCK_RECORDS = buildMockRecords()

function filterMockRecords(params = {}) {
  let list = [...ALL_MOCK_RECORDS]

  if (params.stealNo) {
    list = list.filter(r => r.jicheng.stealNo.includes(params.stealNo))
  }
  if (params.dbNo) {
    list = list.filter(r => r.chenman.dbNo.includes(params.dbNo))
  }
  if (params.meteraddress) {
    list = list.filter(r => r.jiexian.meteraddress.includes(params.meteraddress))
  }
  if (params.name) {
    list = list.filter(r => r.chenman.name.includes(params.name) || r.customerName.includes(params.name))
  }
  if (params.customerId) {
    list = list.filter(r => r.customerId === Number(params.customerId))
  }
  if (params.projectId) {
    list = list.filter(r => r.chenman.proId === Number(params.projectId))
  }
  if (params.vendor) {
    list = list.filter(r => r.jicheng.vender.includes(params.vendor))
  }
  if (params.status !== undefined && params.status !== '') {
    list = list.filter(r => r.general.status === Number(params.status))
  }
  if (params.online !== undefined && params.online !== '') {
    list = list.filter(r => r.general.online === Number(params.online))
  }
  if (params.meterType !== undefined && params.meterType !== '') {
    list = list.filter(r => r.general.meterType === Number(params.meterType))
  }
  if (params.bjlx !== undefined && params.bjlx !== '') {
    list = list.filter(r => r.chenman.bjlx === Number(params.bjlx))
  }
  if (params.terminalName) {
    list = list.filter(r => r.jicheng.terminalName.includes(params.terminalName))
  }
  if (params.groupId) {
    list = list.filter(r => r.general.groupId === Number(params.groupId))
  }
  if (params.markStatus) {
    list = list.filter(r => r.general.markStatus === params.markStatus)
  }
  return list
}

export function mockGetMeterList(params = {}) {
  return new Promise(resolve => {
    setTimeout(() => {
      const pageNum = Number(params.pageNum) || 1
      const pageSize = Number(params.pageSize) || 10
      const filtered = filterMockRecords(params)
      const total = filtered.length
      const start = (pageNum - 1) * pageSize
      const records = filtered.slice(start, start + pageSize)

      resolve({
        code: 1073741824,
        msg: '操作成功',
        data: {
          size: pageSize,
          total,
          records,
          current: pageNum,
        },
        timestamp: Date.now(),
        success: true,
      })
    }, 300)
  })
}

export function mockGetMeterDetail(id) {
  return new Promise(resolve => {
    setTimeout(() => {
      const meter = ALL_MOCK_RECORDS.find(r => r.id === Number(id))
      if (meter) {
        resolve({ code: 1073741824, msg: '操作成功', data: meter, timestamp: Date.now(), success: true })
      } else {
        resolve({ code: 1073741824, msg: '表计不存在', data: null, timestamp: Date.now(), success: false })
      }
    }, 200)
  })
}

export function mockDeleteMeter(id) {
  return new Promise(resolve => {
    setTimeout(() => {
      const idx = ALL_MOCK_RECORDS.findIndex(r => r.id === Number(id))
      if (idx >= 0) {
        ALL_MOCK_RECORDS.splice(idx, 1)
        resolve({ code: 1073741824, msg: '操作成功', data: {}, timestamp: Date.now(), success: true })
      } else {
        resolve({ code: 1073741824, msg: '表计不存在', data: {}, timestamp: Date.now(), success: false })
      }
    }, 200)
  })
}

export function mockClearBalance(id, data) {
  return new Promise(resolve => {
    setTimeout(() => {
      const meter = ALL_MOCK_RECORDS.find(r => r.id === Number(id))
      if (meter) {
        resolve({ code: 1073741824, msg: '操作成功', data: {}, timestamp: Date.now(), success: true })
      } else {
        resolve({ code: 1073741824, msg: '表计不存在', data: {}, timestamp: Date.now(), success: false })
      }
    }, 200)
  })
}

export function mockUpdateMeter(id, data) {
  return new Promise(resolve => {
    setTimeout(() => {
      const idx = ALL_MOCK_RECORDS.findIndex(r => r.id === Number(id))
      if (idx < 0) {
        resolve({ code: 1073741824, msg: '表计不存在', data: null, timestamp: Date.now(), success: false })
        return
      }
      const meter = ALL_MOCK_RECORDS[idx]
      if (data.customerId !== undefined) meter.customerId = data.customerId
      if (meter.jicheng) {
        const j = meter.jicheng
        if (data.stealNo !== undefined) j.stealNo = data.stealNo
        if (data.vender !== undefined) j.vender = data.vender
        if (data.meterTypeName !== undefined) j.meterTypeName = data.meterTypeName
        if (data.caliber !== undefined) j.caliber = data.caliber
        if (data.switchOp !== undefined) j.switchOp = data.switchOp
        if (data.startVal !== undefined) j.startVal = data.startVal
        if (data.terminalName !== undefined) j.terminalName = data.terminalName
        if (data.channelType !== undefined) j.channelType = data.channelType
        if (data.meterPhone !== undefined) j.meterPhone = data.meterPhone
        if (data.telecommSn !== undefined) j.telecommSn = data.telecommSn
        if (data.imei !== undefined) j.imei = data.imei
        if (data.iccid !== undefined) j.iccid = data.iccid
        if (data.address !== undefined) j.address = data.address
        if (data.type !== undefined) j.type = data.type
      }
      if (meter.chenman) {
        const c = meter.chenman
        if (data.dbNo !== undefined) c.dbNo = data.dbNo
        if (data.name !== undefined) c.name = data.name
        if (data.bei !== undefined) c.bei = data.bei
        if (data.bjlx !== undefined) c.bjlx = data.bjlx
        if (data.credit !== undefined) c.credit = data.credit
        if (data.warnline !== undefined) c.warnline = data.warnline
        if (data.creatuser !== undefined) c.creatuser = data.creatuser
        if (data.cttime !== undefined) c.cttime = data.cttime
        if (data.feetype !== undefined) c.feetype = data.feetype
        if (data.proId !== undefined) c.proId = data.proId
        if (data.proName !== undefined) c.proName = data.proName
        if (data.plusPrice !== undefined) c.plusPrice = data.plusPrice
        if (data.priceType !== undefined) c.priceType = data.priceType
        if (data.priceContent !== undefined) c.priceContent = data.priceContent
        if (data.joinPhone !== undefined) c.joinPhone = data.joinPhone
        if (data.collectorDeviceNo !== undefined) c.collectorDeviceNo = data.collectorDeviceNo
        if (data.phase !== undefined) c.phase = data.phase
      }
      if (meter.jiexian) {
        const x = meter.jiexian
        if (data.meteraddress !== undefined) x.meteraddress = data.meteraddress
        if (data.companycode !== undefined) x.companycode = data.companycode
        if (data.customerNo !== undefined) x.customerNo = data.customerNo
        if (data.meterCaliber !== undefined) x.meterCaliber = data.meterCaliber
        if (data.controlling !== undefined) x.controlling = data.controlling
        if (data.stateValve !== undefined) x.stateValve = data.stateValve
        if (data.signalStrength !== undefined) x.signalStrength = data.signalStrength
        if (data.batteryVoltage !== undefined) x.batteryVoltage = data.batteryVoltage
      }
      if (meter.general) {
        const g = meter.general
        if (data.status !== undefined) g.status = data.status
        if (data.online !== undefined) g.online = data.online
        if (data.meterSignal !== undefined) g.meterSignal = data.meterSignal
        if (data.rateConfigId !== undefined) g.rateConfigId = data.rateConfigId
        if (data.groupId !== undefined) g.groupId = data.groupId
        if (data.ctRatio !== undefined) g.ctRatio = data.ctRatio
        if (data.ptRatio !== undefined) g.ptRatio = data.ptRatio
        if (data.markStatus !== undefined) g.markStatus = data.markStatus
        if (data.disableWarning !== undefined) g.disableWarning = data.disableWarning
        if (data.commStatusDesc !== undefined) g.commStatusDesc = data.commStatusDesc
      }
      if (meter.commParams) {
        const p = meter.commParams
        if (data.collectorCode !== undefined) p.collectorCode = data.collectorCode
        if (data.bpsType !== undefined) p.bpsType = data.bpsType
        if (data.bitType !== undefined) p.bitType = data.bitType
        if (data.protoType !== undefined) p.protoType = data.protoType
        if (data.serial !== undefined) p.serial = data.serial
        if (data.deviceType !== undefined) p.deviceType = data.deviceType
      }
      meter.updateTime = new Date().toISOString()
      resolve({ code: 1073741824, msg: '操作成功', data: meter, timestamp: Date.now(), success: true })
    }, 300)
  })
}

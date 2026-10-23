import http from '@/common/request.js'

/**
 * 当前用户拥有或被授权查看的表计。
 */
export function getMyMeters() {
  return http({
    url: '/api/v1/miniapp/meter/my-meters',
    method: 'GET'
  })
}

// ==================== 表计查询模块（电表 + 水表） ====================

// ----- 电表 -----

/**
 * 扫码查询电表
 * @param {string} qrContent - 二维码内容
 */
export function scanElectricMeter(qrContent) {
  return http({
    url: '/api/v1/miniapp/electric-meter/scan',
    method: 'POST',
    data: { qrContent },
    silent: true
  })
}

/**
 * 电表详情（按ID）
 * @param {number} meterId - 表计ID
 */
export function getElectricMeterDetail(meterId) {
  return http({
    url: `/api/v1/miniapp/electric-meter/${meterId}`,
    method: 'GET'
  })
}

/**
 * 电表详情（按表号）
 * @param {string} meterNo - 表号
 */
export function getElectricMeterByNo(meterNo) {
  return http({
    url: `/api/v1/miniapp/electric-meter/by-no/${meterNo}`,
    method: 'GET',
    silent: true
  })
}

// ----- 水表 -----

/**
 * 扫码查询水表
 * @param {string} qrContent - 二维码内容
 */
export function scanWaterMeter(qrContent) {
  return http({
    url: '/api/v1/miniapp/water-meter/scan',
    method: 'POST',
    data: { qrContent },
    silent: true
  })
}

/**
 * 水表详情（按ID）
 * @param {number} meterId - 表计ID
 */
export function getWaterMeterDetail(meterId) {
  return http({
    url: `/api/v1/miniapp/water-meter/${meterId}`,
    method: 'GET'
  })
}

/**
 * 水表详情（按表号）
 * @param {string} meterNo - 表号
 */
export function getWaterMeterByNo(meterNo) {
  return http({
    url: `/api/v1/miniapp/water-meter/by-no/${meterNo}`,
    method: 'GET',
    silent: true
  })
}

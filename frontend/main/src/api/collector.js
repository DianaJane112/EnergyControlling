import http from './request.js'
import axios from 'axios'

// ── API：基站分页列表 ──────────────────────────────────────────────────
export function getCollectorPage(params = {}) {
  return http.get('/v1/collector/page', { params })
}

// ── API：基站详情 ──────────────────────────────────────────────────────
export function getCollectorDetail(id) {
  return http.get(`/v1/collector/${id}`)
}

// ── API：创建基站 ──────────────────────────────────────────────────────
export function createCollector(data) {
  return http.post('/v1/collector', data)
}

// ── API：更新基站 ──────────────────────────────────────────────────────
export function updateCollector(id, data) {
  return http.put(`/v1/collector/${id}`, data)
}

// ── API：删除基站 ──────────────────────────────────────────────────────
export function deleteCollector(id) {
  return http.delete(`/v1/collector/${id}`)
}

// ── API：基站统计 ──────────────────────────────────────────────────────
export function getCollectorStatistics(params = {}) {
  return http.get('/v1/collector/statistics', { params })
}

// ── API：导出基站列表 ──────────────────────────────────────────────────
export function exportCollector(params = {}) {
  const token = localStorage.getItem('smalleel_token')
  return axios.get('/api/v1/collector/export', {
    params,
    responseType: 'blob',
    headers: {
      'ngrok-skip-browser-warning': true,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
}

// ── API：基站下绑定的表计列表 ──────────────────────────────────────────
export function getCollectorMeters(id, params = {}) {
  return http.get(`/v1/collector/${id}/meters`, { params })
}

// ── API：绑定表计到基站 ────────────────────────────────────────────────
export function bindMeterToCollector(collectorId, meterId) {
  return http.post(`/v1/collector/${collectorId}/bind-meter`, { meterId })
}

// ── API：解绑单个表计 ──────────────────────────────────────────────────
export function unbindMeterFromCollector(collectorId, meterId) {
  return http.delete(`/v1/collector/${collectorId}/unbind-meter/${meterId}`)
}

// ── API：解绑全部表计 ──────────────────────────────────────────────────
export function unbindAllMetersFromCollector(collectorId) {
  return http.delete(`/v1/collector/${collectorId}/unbind-all-meters`)
}

// ── API：一键扫描绑定在线表计 ──────────────────────────────────────────
export function bindAllOnlineMeters(collectorId) {
  return http.post(`/v1/collector/${collectorId}/bind-all-online`)
}

// ── API：批量导入集中器 ──────────────────────────────────────────────
export function batchImportCollectors(requests) {
  return http.post('/v1/collector/batch', requests)
}

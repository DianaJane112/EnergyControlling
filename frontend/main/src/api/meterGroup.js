import http from './request.js'

// ── 分页查询分组列表 ──────────────────────────────────────────────────
export function getMeterGroupList(params = {}) {
  return http.get('/v1/meter-group', { params })
}

// ── 查询分组详情 ──────────────────────────────────────────────────────
export function getMeterGroupById(id) {
  return http.get(`/v1/meter-group/${id}`)
}

// ── 商户分组下拉列表 ──────────────────────────────────────────────────
export function getMeterGroupListByMerchant(merchantId) {
  return http.get('/v1/meter-group/list-by-merchant', { params: { merchantId } })
}

export function getMeterGroupTree(merchantId) {
  return http.get('/v1/meter-group/tree', { params: merchantId ? { merchantId } : {} })
}

// ── 创建分组 ──────────────────────────────────────────────────────────
export function createMeterGroup(data) {
  return http.post('/v1/meter-group', data)
}

// ── 更新分组 ──────────────────────────────────────────────────────────
export function updateMeterGroup(id, data) {
  return http.put(`/v1/meter-group/${id}`, data)
}

// ── 删除分组（逻辑删除）───────────────────────────────────────────────
export function deleteMeterGroup(id) {
  return http.delete(`/v1/meter-group/${id}`)
}

import http from './request.js'

export function getWaterMeterVendorPage(params = {}) {
  return http.get('/v1/water-meter-vendor/page', { params })
}

export function getWaterMeterVendorDetail(id) {
  return http.get(`/v1/water-meter-vendor/${id}`)
}

export function createWaterMeterVendor(data) {
  return http.post('/v1/water-meter-vendor', data)
}

export function updateWaterMeterVendorStatus(id, status) {
  return http.put(`/v1/water-meter-vendor/${id}/status`, null, { params: { status } })
}

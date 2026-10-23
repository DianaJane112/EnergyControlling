import http from './request.js'

export function getElectricMeterVendorPage(params = {}) {
  return http.get('/v1/electric-meter-vendor/page', { params })
}

export function getElectricMeterVendorDetail(id) {
  return http.get(`/v1/electric-meter-vendor/${id}`)
}

export function createElectricMeterVendor(data) {
  return http.post('/v1/electric-meter-vendor', data)
}

export function updateElectricMeterVendorStatus(id, status) {
  return http.put(`/v1/electric-meter-vendor/${id}/status`, null, { params: { status } })
}

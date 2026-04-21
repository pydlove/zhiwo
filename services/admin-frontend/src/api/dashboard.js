import request from './request.js'

export function getDashboardStats() {
  return request.get('/dashboard/stats')
}

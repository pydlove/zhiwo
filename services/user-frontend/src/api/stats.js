import request from './request.js'

export function getStats() {
  return request.get('/stats')
}

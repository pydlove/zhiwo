import request from './request.js'

export function listOrders(params) {
  return request.get('/orders', { params })
}

export function createOrder(data) {
  return request.post('/orders', data)
}

export function getOrderStats() {
  return request.get('/orders/stats')
}

export function exportOrders(params) {
  return request.get('/orders/export', {
    params,
    responseType: 'blob',
  })
}

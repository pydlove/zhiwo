import request from './request.js'

export function getProcessAutoStatus(date) {
  const params = {}
  if (date) params.date = date
  return request.get('/process-auto/status', { params })
}

export function getProcessAutoConfig() {
  return request.get('/process-auto/config')
}

export function saveProcessAutoConfig(data) {
  return request.post('/process-auto/config', data)
}

export function getTodayLog(date) {
  const params = {}
  if (date) params.date = date
  return request.get('/process-auto/today-log', { params })
}

export function getRecentLogs(limit) {
  return request.get('/process-auto/recent-logs', { params: { limit } })
}

export function triggerCheck(date) {
  const params = {}
  if (date) params.date = date
  return request.post('/process-auto/trigger-check', null, { params })
}

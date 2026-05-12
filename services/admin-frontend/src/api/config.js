import request from './request.js'

export function getVisibleTabs() {
  return request.get('/configs/visible-tabs')
}

export function getAllConfigs() {
  return request.get('/configs')
}

export function saveConfig(key, value) {
  return request.post('/configs', { [key]: value })
}

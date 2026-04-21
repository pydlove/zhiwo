import request from './request.js'

export function getConfigs() {
  return request.get('/configs')
}

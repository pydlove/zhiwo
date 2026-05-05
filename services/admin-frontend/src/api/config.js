import request from './request.js'

export function getVisibleTabs() {
  return request.get('/configs/visible-tabs')
}

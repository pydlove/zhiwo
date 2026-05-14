import request from './request.js'

export function createScheduledPush(data) {
  return request.post('/scheduled-push', data)
}

export function listScheduledPush(params) {
  return request.get('/scheduled-push', { params })
}

export function getScheduledPush(id) {
  return request.get('/scheduled-push/' + id)
}

export function cancelScheduledPush(id) {
  return request.delete('/scheduled-push/' + id)
}

export function updateScheduledPush(id, data) {
  return request.put('/scheduled-push/' + id, data)
}

export function deleteScheduledPush(id) {
  return request.delete('/scheduled-push/' + id + '/delete')
}

export function triggerScheduledPush(id) {
  return request.post('/scheduled-push/' + id + '/trigger')
}

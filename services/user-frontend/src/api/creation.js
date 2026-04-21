import request from './request.js'

export function listCreations(params) {
  return request.get('/creation-records', { params })
}

export function getCreation(id) {
  return request.get('/creation-records/' + id)
}

export function saveCreation(data) {
  return request.post('/creation-records', data)
}

export function deleteCreation(id) {
  return request.delete('/creation-records/' + id)
}

import request from './request.js'

export function listCreations(params) {
  return request.get('/creation-records', { params })
}

export function getCreation(id) {
  return request.get('/creation-records/' + id)
}

export function reviewCreation(id) {
  return request.post('/creation-records/' + id + '/review')
}

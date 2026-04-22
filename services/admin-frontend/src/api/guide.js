import request from './request.js'

export function listGuides(params) {
  return request.get('/guides', { params })
}

export function getGuide(id) {
  return request.get('/guides/' + id)
}

export function saveGuide(data) {
  if (data.id) {
    return request.put('/guides/' + data.id, data)
  }
  return request.post('/guides', data)
}

export function deleteGuide(id) {
  return request.delete('/guides/' + id)
}

export function batchUpdateRecommended(ids, isRecommended) {
  return request.post('/guides/batch-recommended', { ids, isRecommended })
}

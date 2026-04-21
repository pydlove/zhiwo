import request from './request.js'

export function listGuides() {
  return request.get('/guides')
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

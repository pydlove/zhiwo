import request from './request.js'

export function listStyles() {
  return request.get('/styles')
}

export function saveStyle(data) {
  if (data.id) {
    return request.put('/styles/' + data.id, data)
  }
  return request.post('/styles', data)
}

export function deleteStyle(id) {
  return request.delete('/styles/' + id)
}

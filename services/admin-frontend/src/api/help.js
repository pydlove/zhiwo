import request from './request.js'

export function listHelps() {
  return request.get('/helps')
}

export function getHelp(id) {
  return request.get('/helps/' + id)
}

export function saveHelp(data) {
  if (data.id) {
    return request.put('/helps/' + data.id, data)
  }
  return request.post('/helps', data)
}

export function deleteHelp(id) {
  return request.delete('/helps/' + id)
}

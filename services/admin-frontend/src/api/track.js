import request from './request.js'

export function listTracks() {
  return request.get('/tracks')
}

export function getTrack(id) {
  return request.get('/tracks/' + id)
}

export function saveTrack(data) {
  if (data.id) {
    return request.put('/tracks/' + data.id, data)
  }
  return request.post('/tracks', data)
}

export function deleteTrack(id) {
  return request.delete('/tracks/' + id)
}

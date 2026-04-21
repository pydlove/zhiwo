import request from './request.js'

export function listTracks() {
  return request.get('/tracks')
}

export function getTrack(id) {
  return request.get('/tracks/' + id)
}

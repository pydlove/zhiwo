import request from './request.js'

export function listUserTracks(userId) {
  return request.get('/users/' + userId + '/tracks')
}

export function addUserTrack(userId, trackId) {
  return request.post('/users/' + userId + '/tracks', { trackId })
}

export function removeUserTrack(userId, trackId) {
  return request.delete('/users/' + userId + '/tracks/' + trackId)
}

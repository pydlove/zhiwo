import request from './request.js'

export function listUsers() {
  return request.get('/users')
}

export function getUserTracks(userId) {
  return request.get('/users/' + userId + '/tracks')
}

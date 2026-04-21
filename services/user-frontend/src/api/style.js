import request from './request.js'

export function listStyles() {
  return request.get('/styles')
}

export function getStyle(id) {
  return request.get('/styles/' + id)
}

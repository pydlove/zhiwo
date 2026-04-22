import request from './request.js'

export function listGuides() {
  return request.get('/guides')
}

export function getGuide(id) {
  return request.get('/guides/' + id)
}

export function listRecommendedGuides() {
  return request.get('/guides/recommended')
}

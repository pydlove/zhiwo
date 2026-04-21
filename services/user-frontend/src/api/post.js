import request from './request.js'

export function listPosts(bloggerId) {
  return request.get('/posts' + (bloggerId ? '?bloggerId=' + bloggerId : ''))
}

export function searchPosts(params) {
  return request.get('/posts/search', { params })
}

export function listRecommendations(trackId) {
  return request.get('/posts/recommendations' + (trackId ? '?trackId=' + trackId : ''))
}

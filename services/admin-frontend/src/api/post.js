import request from './request.js'

export function listPosts(bloggerId) {
  return request.get('/posts' + (bloggerId ? '?bloggerId=' + bloggerId : ''))
}

export function searchPosts(params) {
  return request.get('/posts/search', { params })
}

export function savePost(data) {
  return request.post('/posts', data)
}

export function deletePost(id) {
  return request.delete('/posts/' + id)
}

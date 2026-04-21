import request from './request.js'

export function listSubscriptionPosts(params) {
  return request.get('/subscription-posts', { params })
}

export function listSubscriptionPostsByUser(userId) {
  return request.get('/subscription-posts/user/' + userId)
}

export function saveSubscriptionPost(data) {
  if (data.id) {
    return request.put('/subscription-posts/' + data.id, data)
  }
  return request.post('/subscription-posts', data)
}

export function deleteSubscriptionPost(id) {
  return request.delete('/subscription-posts/' + id)
}

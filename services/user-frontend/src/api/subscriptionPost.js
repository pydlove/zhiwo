import request from './request.js'

export function getLatestSubscriptionPost(userId, trackId) {
  return request.get('/subscription-posts/latest?userId=' + encodeURIComponent(userId) + '&trackId=' + encodeURIComponent(trackId))
}

export function markSubscriptionPostUsed(id) {
  return request.post('/subscription-posts/' + encodeURIComponent(id) + '/used')
}

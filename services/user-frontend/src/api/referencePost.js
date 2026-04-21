import request from './request.js'

export function listReferencePosts(trackId, platform) {
  return request.get('/reference-posts?trackId=' + trackId + '&platform=' + encodeURIComponent(platform))
}

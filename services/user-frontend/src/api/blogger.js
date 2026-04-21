import request from './request.js'

export function listBloggers(trackId) {
  return request.get('/bloggers' + (trackId ? '?trackId=' + trackId : ''))
}

export function getBlogger(id) {
  return request.get('/bloggers/' + id)
}

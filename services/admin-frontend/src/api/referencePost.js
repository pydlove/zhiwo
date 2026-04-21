import request from './request.js'

export function listReferencePosts() {
  return request.get('/reference-posts')
}

export function saveReferencePost(data) {
  return request.post('/reference-posts', data)
}

export function deleteReferencePost(id) {
  return request.delete('/reference-posts/' + id)
}

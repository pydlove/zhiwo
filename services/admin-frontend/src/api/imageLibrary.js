import request from './request.js'

export function listImages(params) {
  return request.get('/images', { params })
}

export function uploadImage(data) {
  return request.post('/images/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteImage(id) {
  return request.delete(`/images/${id}`)
}

export function updateImage(id, data) {
  return request.post(`/images/${id}/update`, data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function downloadImages(data) {
  return request.post('/images/download', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

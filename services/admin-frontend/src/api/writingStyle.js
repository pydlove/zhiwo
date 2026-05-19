import request from './request.js'

export function listWritingStyles(category) {
  const params = category ? { category } : {}
  return request.get('/writing-styles', { params })
}

export function saveWritingStyle(data) {
  return request.post('/writing-styles', data)
}

export function deleteWritingStyle(id) {
  return request.delete('/writing-styles/' + id)
}

export function listWritingStyleCategories() {
  return request.get('/writing-styles/categories')
}

import request from './request.js'

export function listHelpCategories() {
  return request.get('/help-categories')
}

export function saveHelpCategory(data) {
  if (data.id) {
    return request.put('/help-categories/' + data.id, data)
  }
  return request.post('/help-categories', data)
}

export function deleteHelpCategory(id) {
  return request.delete('/help-categories/' + id)
}

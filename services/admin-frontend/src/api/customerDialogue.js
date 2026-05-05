import request from './request.js'

export function listCustomerDialogues(category) {
  const params = {}
  if (category) params.category = category
  return request.get('/customer-dialogues', { params })
}

export function listCategories() {
  return request.get('/customer-dialogues/categories')
}

export function getCustomerDialogue(id) {
  return request.get('/customer-dialogues/' + id)
}

export function saveCustomerDialogue(data) {
  return request.post('/customer-dialogues', data)
}

export function deleteCustomerDialogue(id) {
  return request.delete('/customer-dialogues/' + id)
}

export function exportSelected(ids) {
  return request.post('/customer-dialogues/export-selected', ids, {
    responseType: 'blob',
  })
}

export function batchDeleteCustomerDialogues(ids) {
  return request.post('/customer-dialogues/batch-delete', ids)
}

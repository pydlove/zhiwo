import request from './request.js'

export function listCustomerDialogues(category) {
  const params = category ? { category } : {}
  return request.get('/customer-dialogues', { params })
}

export function listCategories() {
  return request.get('/customer-dialogues/categories')
}

export function saveCustomerDialogue(data) {
  return request.post('/customer-dialogues', data)
}

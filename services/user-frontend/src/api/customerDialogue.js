import request from './request.js'

export function listCustomerDialogues(category, adminId) {
  const params = {}
  if (category) params.category = category
  if (adminId) params.adminId = adminId
  return request.get('/customer-dialogues', { params })
}

export function listCategories() {
  return request.get('/customer-dialogues/categories')
}

export function saveCustomerDialogue(data) {
  return request.post('/customer-dialogues', data)
}

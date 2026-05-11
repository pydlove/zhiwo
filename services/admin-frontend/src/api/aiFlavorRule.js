import request from './request.js'

export function listAiFlavorRules() {
  return request.get('/ai-flavor-rules')
}

export function saveAiFlavorRule(data) {
  return request.post('/ai-flavor-rules', data)
}

export function deleteAiFlavorRule(id) {
  return request.delete('/ai-flavor-rules/' + id)
}
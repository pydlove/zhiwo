import request from './request.js'

export function listPromptTemplates(params) {
  return request.get('/prompt-templates', { params })
}

export function getDefaultPromptTemplate(type) {
  return request.get('/prompt-templates/default', { params: { type } })
}

export function savePromptTemplate(data) {
  return request.post('/prompt-templates', data)
}

export function deletePromptTemplate(id) {
  return request.delete(`/prompt-templates/${id}`)
}

import request from './request.js'

export function getLLMConfig() {
  return request.get('/llm-config')
}

export function saveLLMConfig(data) {
  return request.post('/llm-config', data)
}

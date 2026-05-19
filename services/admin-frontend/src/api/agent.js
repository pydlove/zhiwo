import request from './request.js'

export function getAgentConfig() {
  return request.get('/agent/config')
}

export function saveAgentConfig(data) {
  return request.post('/agent/config', data)
}

export function getAgentExecutions(limit = 20) {
  return request.get('/agent/executions', { params: { limit } })
}

export function getAgentExecution(id) {
  return request.get(`/agent/executions/${id}`)
}

export function triggerAgentRun() {
  return request.post('/agent/trigger')
}

import request from './request.js'

export function createTitleGenerateTask(data) {
  return request.post('/title-generate/tasks', data)
}

export function listTitleGenerateTasks(params) {
  return request.get('/title-generate/tasks', { params })
}

export function getTitleGenerateTask(id) {
  return request.get(`/title-generate/tasks/${id}`)
}

export function cancelTitleGenerateTask(id) {
  return request.post(`/title-generate/tasks/${id}/cancel`)
}

export function stopTitleGenerateTask(id) {
  return request.post(`/title-generate/tasks/${id}/stop`)
}

export function getTaskTitles(id) {
  return request.get(`/title-generate/tasks/${id}/titles`)
}

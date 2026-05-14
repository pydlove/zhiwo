import request from './request.js'

export function listTasks(params) {
  return request.get('/tasks', { params })
}

export function getTaskDetail(id) {
  return request.get(`/tasks/${id}`)
}

export function cancelTask(id) {
  return request.post(`/tasks/${id}/cancel`)
}

export function stopTask(id) {
  return request.post(`/tasks/${id}/stop`)
}

export function retryTask(id) {
  return request.post(`/tasks/${id}/retry`)
}

export function rerunBackend(id) {
  return request.post(`/tasks/${id}/rerun-backend`)
}

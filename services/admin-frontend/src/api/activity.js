import request from './request.js'

export function listActivities() {
  return request.get('/activities')
}

export function saveActivity(data) {
  if (data.id) {
    return request.put('/activities/' + data.id, data)
  }
  return request.post('/activities', data)
}

export function deleteActivity(id) {
  return request.delete('/activities/' + id)
}

export function sendActivityEmail(activityId, userIds) {
  return request.post('/activities/' + activityId + '/send-email', { userIds })
}

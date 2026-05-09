import request from './request.js'

export function listUsers(params = {}) {
  const query = new URLSearchParams()
  if (params.status !== undefined && params.status !== null) query.append('status', params.status)
  if (params.userType !== undefined && params.userType !== null) query.append('userType', params.userType)
  if (params.keyword) query.append('keyword', params.keyword)
  if (params.platform) query.append('platform', params.platform)
  if (params.trackId) query.append('trackId', params.trackId)
  if (params.adminId) query.append('adminId', params.adminId)
  const qs = query.toString()
  return request.get('/users' + (qs ? '?' + qs : ''))
}

export function getUserTracks(userId) {
  return request.get('/users/' + userId + '/tracks')
}

export function addUserTrack(userId, trackId) {
  return request.post('/users/' + userId + '/tracks', { trackId })
}

export function removeUserTrack(userId, trackId) {
  return request.delete('/users/' + userId + '/tracks/' + trackId)
}

export function exportUsers(params) {
  const query = new URLSearchParams()
  if (params.keyword) query.append('keyword', params.keyword)
  if (params.status !== undefined && params.status !== null) query.append('status', params.status)
  if (params.userType !== undefined && params.userType !== null) query.append('userType', params.userType)
  if (params.userIds && params.userIds.length > 0) {
    params.userIds.forEach(id => query.append('userIds', id))
  }
  return request.get('/users/export?' + query.toString(), {
    responseType: 'blob'
  })
}

export function importUsers(excelFile) {
  const formData = new FormData()
  formData.append('excel', excelFile)
  return request.post('/users/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function batchUpdateAdmin(userIds, adminId) {
  return request.post('/users/batch-update-admin', { userIds, adminId })
}

export function getExpiringUsers(days = 7) {
  return request.get('/users/expiring', { params: { days } })
}

export function sendExpireReminderEmails(userIds) {
  return request.post('/users/expiring/send-reminder', { userIds })
}

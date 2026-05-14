import request from './request.js'

export function listAnnouncements() {
  return request.get('/announcements')
}

export function getAnnouncement(id) {
  return request.get('/announcements/' + id)
}

export function saveAnnouncement(data) {
  if (data.id) {
    return request.put('/announcements/' + data.id, data)
  }
  return request.post('/announcements', data)
}

export function deleteAnnouncement(id) {
  return request.delete('/announcements/' + id)
}

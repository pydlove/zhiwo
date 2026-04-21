import request from './request.js'

export function listAdmins() {
  return request.get('/admins')
}

export function saveAdmin(data) {
  if (data.id) {
    return request.put('/admins/' + data.id, data)
  }
  return request.post('/admins', data)
}

export function deleteAdmin(id) {
  return request.delete('/admins/' + id)
}

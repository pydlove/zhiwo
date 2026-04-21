import request from './request.js'

export function listRoles() {
  return request.get('/roles')
}

export function getRoleAdminCounts() {
  return request.get('/roles/admin-counts')
}

export function saveRole(data) {
  if (data.id) {
    return request.put('/roles/' + data.id, data)
  }
  return request.post('/roles', data)
}

export function deleteRole(id) {
  return request.delete('/roles/' + id)
}

import request from './request.js'

export function updateAvatar(userId, avatar) {
  return request.put('/users/' + userId + '/avatar', { avatar })
}

export function updateUserTemplate(userId, template) {
  return request.put('/users/' + userId, { template })
}

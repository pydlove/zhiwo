import request from './request.js'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function changePassword(data) {
  return request.post('/auth/change-password', data)
}

import request from './request.js'

export function getOperatorInfo(username) {
  return request.get('/operators/' + encodeURIComponent(username))
}

export function getOperatorDialogues(username, category) {
  const params = {}
  if (category) params.category = category
  return request.get('/operators/' + encodeURIComponent(username) + '/dialogues', { params })
}

import request from './request.js'

// 标题审核相关
export function listTitleReviews(params) {
  return request.get('/title-review/list', { params })
}

export function approveTitleReview(id) {
  return request.post('/title-review/' + id + '/approve')
}

export function rejectTitleReview(id, data) {
  return request.post('/title-review/' + id + '/reject', data)
}

export function batchApproveTitleReviews(ids) {
  return request.post('/title-review/batch-approve', { ids })
}

export function batchRejectTitleReviews(ids, reason) {
  return request.post('/title-review/batch-reject', { ids, reason })
}

export function cancelTitleReview(id) {
  return request.post('/title-review/' + id + '/cancel')
}

export function batchCancelTitleReviews(ids) {
  return request.post('/title-review/batch-cancel', { ids })
}

export function pushTitleReview(id, data) {
  return request.post('/title-review/' + id + '/push', data)
}

export function batchPushTitleReviews(data) {
  return request.post('/title-review/batch-push', data)
}

export function rePushTitleReview(id, data) {
  return request.post('/title-review/' + id + '/re-push', data)
}

export function batchRePushTitleReviews(data) {
  return request.post('/title-review/batch-re-push', data)
}

export function getTitleReviewStats() {
  return request.get('/title-review/stats')
}

export function listPushedTitleReviews(params) {
  return request.get('/title-review/list-pushed', { params })
}

export function listPushLogs(params) {
  return request.get('/title-review/push-logs', { params })
}

export function listBySource(params) {
  return request.get('/title-review/list-by-source', { params })
}

// 服务器配置相关
export function listServerConfigs() {
  return request.get('/server-configs')
}

export function saveServerConfig(data) {
  return request.post('/server-configs', data)
}

export function deleteServerConfig(id) {
  return request.delete('/server-configs/' + id)
}

export function testServerConfig(id) {
  return request.post('/server-configs/' + id + '/test')
}

export function testServerConfigDirect(data) {
  return request.post('/server-configs/test-direct', data)
}

export function optimizeTitle(id, currentTitle, instruction = '') {
  return request.post('/title-review/' + id + '/optimize', { currentTitle, instruction })
}

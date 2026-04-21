import request from './request.js'

export function listDailyRecommends() {
  return request.get('/daily-recommends')
}

export function saveDailyRecommend(data) {
  if (data.id) {
    return request.put('/daily-recommends/' + data.id, data)
  }
  return request.post('/daily-recommends', data)
}

export function deleteDailyRecommend(id) {
  return request.delete('/daily-recommends/' + id)
}

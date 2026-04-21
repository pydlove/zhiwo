import request from './request.js'

export function listDailyRecommends(trackId, platform) {
  return request.get('/daily-recommends?trackId=' + encodeURIComponent(trackId) + '&platform=' + encodeURIComponent(platform))
}

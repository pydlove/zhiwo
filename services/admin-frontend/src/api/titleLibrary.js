import request from './request.js'

export function listTitles(params) {
  return request.get('/title-library', { params })
}

export function saveTitle(data) {
  if (data.id) {
    return request.put('/title-library/' + data.id, data)
  }
  return request.post('/title-library', data)
}

export function deleteTitle(id) {
  return request.delete('/title-library/' + id)
}

export function importTitles(excelFile) {
  const formData = new FormData()
  formData.append('excel', excelFile)
  return request.post('/title-library/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function matchCheck(date) {
  const params = {}
  if (date) params.date = date
  return request.get('/title-library/match-check', { params })
}

export function matchTodayTitles(date) {
  const params = {}
  if (date) params.date = date
  return request.post('/title-library/match-today', null, { params })
}

export function unbindRecommendation(titleId) {
  return request.delete('/title-library/' + titleId + '/recommendation')
}

export function batchUnbindRecommendations(titleIds) {
  return request.post('/title-library/batch-unbind', { titleIds })
}

export function generateTitles(data) {
  return request.post('/title-library/generate', data)
}

export function getGenerateStatus(taskId) {
  return request.get('/title-library/generate-status', { params: { taskId } })
}

export function cancelGenerate(taskId) {
  return request.post('/title-library/generate-cancel', null, { params: { taskId } })
}

export function generatePostsForToday(titleIds) {
  return request.post('/title-library/generate-posts-for-today', { titleIds })
}

export function getGeneratePostStatus(taskId) {
  return request.get('/title-library/generate-post-status', { params: { taskId } })
}

export function cancelGeneratePost(taskId) {
  return request.post('/title-library/generate-post-cancel', null, { params: { taskId } })
}

export function exportTitleLibrary(params) {
  return request.post('/title-library/export', null, {
    params,
    responseType: 'blob',
  })
}

export function exportTitleLibraryBatch(titleIds, baseName) {
  return request.post('/title-library/export', null, {
    params: { titleIds, baseName },
    responseType: 'blob',
    paramsSerializer: {
      indexes: null,
      serialize: (p) => {
        const parts = []
        if (p.titleIds) {
          for (const id of p.titleIds) {
            parts.push(`titleIds=${encodeURIComponent(id)}`)
          }
        }
        if (p.baseName) {
          parts.push(`baseName=${encodeURIComponent(p.baseName)}`)
        }
        return parts.join('&')
      }
    }
  })
}

export function exportTitleList(params) {
  return request.post('/title-library/export-titles', null, {
    params,
    responseType: 'blob',
  })
}

export function exportTitleListBatch(titleIds) {
  return request.post('/title-library/export-titles', null, {
    params: { titleIds },
    responseType: 'blob',
    paramsSerializer: {
      indexes: null,
      serialize: (p) => {
        const parts = []
        if (p.titleIds) {
          for (const id of p.titleIds) {
            parts.push(`titleIds=${encodeURIComponent(id)}`)
          }
        }
        return parts.join('&')
      }
    }
  })
}

export function importArticles(files) {
  const formData = new FormData()
  for (const file of files) {
    formData.append('files', file)
  }
  return request.post('/title-library/import-articles', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function sendTitleEmail(titleId) {
  return request.post('/title-library/' + titleId + '/send-email')
}

export function batchSendTitleEmail(titleIds) {
  return request.post('/title-library/batch-send-email', { titleIds })
}

export function getDefaultPromptTemplate(type) {
  return request.get('/prompt-templates/default', { params: { type } })
}

export function savePromptTemplate(data) {
  return request.post('/prompt-templates', data)
}

export function listUnrecommendedUsers(date, type) {
  const params = { date }
  if (type) params.type = type
  return request.get('/title-library/unrecommended-users', { params })
}

export function listUnpushedUsers(date) {
  return request.get('/title-library/unpushed-users', { params: { date } })
}

export function getPushOverview(params) {
  return request.get('/title-library/push-overview', { params })
}

export function batchPushEmail(data) {
  return request.post('/title-library/batch-push-email', data)
}

export function markTitleUsed(id) {
  return request.post('/title-library/' + id + '/used')
}

export function batchChangeTrack(titleIds, trackId) {
  return request.post('/title-library/batch-change-track', { titleIds, trackId })
}

export function clearRecommendationsByDate(date) {
  const params = {}
  if (date) params.date = date
  return request.post('/title-library/clear-recommendations', null, { params })
}

export function getUserHistory(userId) {
  return request.get('/title-library/user-history/' + userId)
}

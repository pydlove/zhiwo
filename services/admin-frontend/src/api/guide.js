import request from './request.js'

export function listGuides(params) {
  return request.get('/guides', { params })
}

export function getGuide(id) {
  return request.get('/guides/' + id)
}

export function saveGuide(data) {
  if (data.id) {
    return request.put('/guides/' + data.id, data)
  }
  return request.post('/guides', data)
}

export function deleteGuide(id) {
  return request.delete('/guides/' + id)
}

export function batchUpdateRecommended(ids, isRecommended) {
  return request.post('/guides/batch-recommended', { ids, isRecommended })
}

export function generateGuides(category, count) {
  return request.post('/guides/generate', { category, count })
}

export function exportGuides(guideIds) {
  return request.post('/guides/export', null, {
    params: { guideIds },
    responseType: 'blob',
    paramsSerializer: {
      indexes: null,
      serialize: (p) => {
        const parts = []
        if (p.guideIds) {
          for (const id of p.guideIds) {
            parts.push(`guideIds=${encodeURIComponent(id)}`)
          }
        }
        return parts.join('&')
      }
    }
  })
}

export function importGuides(excelFile) {
  const formData = new FormData()
  formData.append('excel', excelFile)
  return request.post('/guides/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

import request from './request.js'

export function listReferencePosts() {
  return request.get('/reference-posts')
}

export function saveReferencePost(data) {
  return request.post('/reference-posts', data)
}

export function deleteReferencePost(id) {
  return request.delete('/reference-posts/' + id)
}

export function exportReferencePosts(refIds) {
  return request.post('/reference-posts/export', null, {
    params: { refIds },
    responseType: 'blob',
    paramsSerializer: {
      indexes: null,
      serialize: (p) => {
        const parts = []
        if (p.refIds) {
          for (const id of p.refIds) {
            parts.push(`refIds=${encodeURIComponent(id)}`)
          }
        }
        return parts.join('&')
      }
    }
  })
}

export function importReferencePosts(excelFile) {
  const formData = new FormData()
  formData.append('excel', excelFile)
  return request.post('/reference-posts/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

import request from './request.js'

export function listPosts(bloggerId) {
  return request.get('/posts' + (bloggerId ? '?bloggerId=' + bloggerId : ''))
}

export function searchPosts(params) {
  return request.get('/posts/search', { params })
}

export function savePost(data) {
  return request.post('/posts', data)
}

export function deletePost(id) {
  return request.delete('/posts/' + id)
}

export function exportPosts(postIds) {
  return request.post('/posts/export', null, {
    params: { postIds },
    responseType: 'blob',
    paramsSerializer: {
      indexes: null,
      serialize: (p) => {
        const parts = []
        if (p.postIds) {
          for (const id of p.postIds) {
            parts.push(`postIds=${encodeURIComponent(id)}`)
          }
        }
        return parts.join('&')
      }
    }
  })
}

export function importPosts(excelFile) {
  const formData = new FormData()
  formData.append('excel', excelFile)
  return request.post('/posts/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

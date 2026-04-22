import request from './request.js'

export function listBloggers(trackId) {
  return request.get('/bloggers' + (trackId ? '?trackId=' + trackId : ''))
}

export function getBlogger(id) {
  return request.get('/bloggers/' + id)
}

export function saveBlogger(data) {
  if (data.id) {
    return request.put('/bloggers/' + data.id, data)
  }
  return request.post('/bloggers', data)
}

export function deleteBlogger(id) {
  return request.delete('/bloggers/' + id)
}

export function importBloggers(excelFile, zipFile) {
  const formData = new FormData()
  formData.append('excel', excelFile)
  if (zipFile) {
    formData.append('zip', zipFile)
  }
  return request.post('/bloggers/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

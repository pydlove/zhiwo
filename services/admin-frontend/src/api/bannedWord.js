import request from './request.js'

export function listBannedWords() {
  return request.get('/banned-words')
}

export function saveBannedWord(data) {
  return request.post('/banned-words', data)
}

export function deleteBannedWord(id) {
  return request.delete('/banned-words/' + id)
}

export function exportBannedWords() {
  return request.get('/banned-words/export', {
    responseType: 'blob',
  })
}

export function importBannedWords(excelFile) {
  const formData = new FormData()
  formData.append('excel', excelFile)
  return request.post('/banned-words/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

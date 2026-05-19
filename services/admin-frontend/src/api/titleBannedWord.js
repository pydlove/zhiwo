import request from './request.js'

export function listTitleBannedWords() {
  return request.get('/title-banned-words')
}

export function listActiveTitleBannedWords() {
  return request.get('/title-banned-words/active')
}

export function saveTitleBannedWord(data) {
  return request.post('/title-banned-words', data)
}

export function deleteTitleBannedWord(id) {
  return request.delete(`/title-banned-words/${id}`)
}

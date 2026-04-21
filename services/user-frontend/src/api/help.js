import request from './request.js'

export function listHelps() {
  return request.get('/helps')
}

export function getHelp(id) {
  return request.get('/helps/' + id)
}

export function listHelpCategories() {
  return request.get('/help-categories')
}

import request from './request.js'

export function detectArticleAi(content) {
  return request.post('/article-ai-detect', { content })
}

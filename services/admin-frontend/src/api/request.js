import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

instance.interceptors.request.use((config) => {
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

instance.interceptors.response.use(
  (res) => {
    // Skip JSON code check for blob/arraybuffer responses (file downloads)
    const contentType = res.headers['content-type'] || ''
    if (contentType.includes('application/vnd.openxmlformats') || contentType.includes('application/octet-stream') || res.config.responseType === 'blob') {
      return res.data
    }
    // 防御性检查：后端可能返回非标准格式（如 Spring Boot 默认错误页、空响应等）
    if (!res.data || typeof res.data !== 'object') {
      return Promise.reject(new Error('后端返回非标准响应'))
    }
    if (res.data.code !== 200) {
      const msg = res.data.msg || '请求失败'
      // message.error(msg) // 可在需要时引入 ant-design-vue 的 message
      return Promise.reject(new Error(msg))
    }
    return res.data.data
  },
  (err) => {
    return Promise.reject(err)
  }
)

export default instance

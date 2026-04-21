import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

instance.interceptors.response.use(
  (res) => {
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

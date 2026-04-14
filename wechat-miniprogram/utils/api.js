const BASE_URL = 'http://localhost:8080/api'

function request(url, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        if (res.data.code === 200) {
          resolve(res.data.data)
        } else {
          reject(res.data.msg)
        }
      },
      fail: reject
    })
  })
}

module.exports = {
  getTracks: () => request('/tracks'),
  getTrack: (id) => request('/tracks/' + id),
  getBloggersByTrack: (trackId) => request('/bloggers?trackId=' + trackId),
  getBlogger: (id) => request('/bloggers/' + id),
  getPostsByBlogger: (bloggerId) => request('/posts?bloggerId=' + bloggerId)
}

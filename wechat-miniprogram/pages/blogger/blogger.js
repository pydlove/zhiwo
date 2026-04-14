const api = require('../../utils/api.js')

Page({
  data: {
    blogger: {},
    trackName: '',
    posts: []
  },

  onLoad(options) {
    const bloggerId = options.id
    this.loadData(bloggerId)
  },

  loadData(bloggerId) {
    api.getBlogger(bloggerId).then(blogger => {
      this.setData({ blogger })
      wx.setNavigationBarTitle({ title: blogger.name })
      // Fetch track name for display
      if (blogger.trackId) {
        api.getTrack(blogger.trackId).then(track => {
          this.setData({ trackName: track.name })
        }).catch(() => {
          // silently ignore track fetch error
        })
      }
    }).catch(err => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })

    api.getPostsByBlogger(bloggerId).then(posts => {
      this.setData({ posts })
    }).catch(err => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  copyUrl(e) {
    const url = e.currentTarget.dataset.url
    wx.setClipboardData({
      data: url,
      success() {
        wx.showToast({ title: '已复制', icon: 'success', duration: 1500 })
      }
    })
  },

  copyTitle(e) {
    const title = e.currentTarget.dataset.title
    wx.setClipboardData({
      data: title,
      success() {
        wx.showToast({ title: '已复制', icon: 'success', duration: 1500 })
      }
    })
  }
})

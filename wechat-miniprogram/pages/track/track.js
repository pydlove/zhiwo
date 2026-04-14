const api = require('../../utils/api.js')

Page({
  data: {
    track: {},
    bloggers: []
  },

  onLoad(options) {
    const trackId = options.id
    this.loadData(trackId)
  },

  loadData(trackId) {
    api.getTrack(trackId).then(track => {
      this.setData({ track })
      wx.setNavigationBarTitle({
        title: `${track.icon} ${track.name}`
      })
    })

    api.getBloggersByTrack(trackId).then(bloggers => {
      this.setData({ bloggers })
    }).catch(err => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  goToBlogger(e) {
    const bloggerId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/blogger/blogger?id=${bloggerId}`
    })
  }
})

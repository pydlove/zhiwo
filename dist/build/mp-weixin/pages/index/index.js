const api = require('../../utils/api.js')

Page({
  data: {
    tracks: [],
    rankColors: ['#ff4d4f', '#ff7a45', '#ffa940']
  },

  onLoad() {
    this.loadTracks()
  },

  loadTracks() {
    api.getTracks().then(tracks => {
      this.setData({ tracks })
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  goToTrack(e) {
    const trackId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/track/track?id=${trackId}`
    })
  }
})

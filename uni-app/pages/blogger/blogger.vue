<template>
  <view class="container">
    <view class="blogger-header">
      <image class="blogger-avatar" :src="blogger.avatar" mode="aspectFill" v-if="blogger.avatar" />
      <view class="blogger-avatar placeholder" v-else></view>
      <view class="blogger-meta">
        <view class="blogger-name">{{ blogger.name }}</view>
        <view class="blogger-track">{{ trackName }}赛道 · Top {{ blogger.rankNum }}</view>
      </view>
    </view>

    <view class="section-title">🔥 代表作品</view>

    <view class="post-list">
      <view class="post-card" v-for="item in posts" :key="item.id">
        <view class="post-title">{{ item.title }}</view>
        <view class="post-actions">
          <view class="action-btn" @click="copyUrl(item.url)">🔗 复制链接</view>
          <view class="action-btn" @click="copyTitle(item.title)">📋 复制标题</view>
        </view>
      </view>
    </view>

    <view class="ad-placeholder">— 底部流量主广告位 —</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../utils/api.js'

const blogger = ref({})
const trackName = ref('')
const posts = ref([])

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const bloggerId = currentPage.options?.id || currentPage.$page?.options?.id
  if (bloggerId) {
    loadData(bloggerId)
  }
})

function loadData(bloggerId) {
  api.getBlogger(bloggerId).then(data => {
    blogger.value = data
    uni.setNavigationBarTitle({ title: data.name })
    if (data.trackId) {
      api.getTrack(data.trackId).then(track => {
        trackName.value = track.name
      }).catch(() => {
        // silently ignore
      })
    }
  }).catch(() => {
    uni.showToast({ title: '加载失败', icon: 'none' })
  })

  api.getPostsByBlogger(bloggerId).then(data => {
    posts.value = data
  }).catch(() => {
    uni.showToast({ title: '加载失败', icon: 'none' })
  })
}

function copyUrl(url) {
  uni.setClipboardData({
    data: url,
    success() {
      uni.showToast({ title: '已复制', icon: 'success', duration: 1500 })
    }
  })
}

function copyTitle(title) {
  uni.setClipboardData({
    data: title,
    success() {
      uni.showToast({ title: '已复制', icon: 'success', duration: 1500 })
    }
  })
}
</script>

<style lang="scss" scoped>
.container {
  background: #f5f5f5;
  padding: 16px;
  min-height: 100vh;
  box-sizing: border-box;
}

.blogger-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.blogger-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}

.blogger-avatar.placeholder {
  background: #e0e0e0;
}

.blogger-meta {
  flex: 1;
}

.blogger-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.blogger-track {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 10px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
}

.post-title {
  font-size: 13px;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1.5;
}

.post-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: #f0f0f0;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  color: #333;
  font-weight: 500;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
</style>

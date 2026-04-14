<template>
  <view class="container">
    <view class="page-title">🔥 热门赛道</view>

    <view class="track-list">
      <view
        class="track-card"
        v-for="(item, index) in tracks"
        :key="item.id"
        :data-id="item.id"
        @click="goToTrack(item.id)"
      >
        <view class="track-rank" :style="{ color: index < 3 ? rankColors[index] : '#666' }">{{ index + 1 }}</view>
        <view class="track-info">
          <view class="track-name">{{ item.icon }} {{ item.name }}</view>
          <view class="track-preview">{{ item.previewBloggers }}</view>
        </view>
        <view class="track-action">查看 →</view>
      </view>
    </view>

    <view class="ad-placeholder">— 底部流量主广告位 —</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../utils/api.js'

const tracks = ref([])
const rankColors = ['#ff4d4f', '#ff7a45', '#ffa940']

onMounted(() => {
  loadTracks()
})

function loadTracks() {
  api.getTracks().then(data => {
    tracks.value = data
  }).catch(() => {
    uni.showToast({ title: '加载失败', icon: 'none' })
  })
}

function goToTrack(trackId) {
  uni.navigateTo({
    url: `/pages/track/track?id=${trackId}`
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

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.track-card {
  background: #fff;
  padding: 14px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid transparent;
}

.track-card:nth-child(1) { border-left-color: #ff4d4f; }
.track-card:nth-child(2) { border-left-color: #ff7a45; }
.track-card:nth-child(3) { border-left-color: #ffa940; }
.track-card:nth-child(n+4) { border-left-color: #07c160; }

.track-rank {
  font-size: 24px;
  font-weight: 700;
  min-width: 28px;
  text-align: center;
}

.track-info {
  flex: 1;
}

.track-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.track-preview {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.track-action {
  font-size: 11px;
  color: #07c160;
  font-weight: 500;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
</style>

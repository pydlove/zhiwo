<template>
  <view class="container">
    <view class="page-title">{{ track.icon }} {{ track.name }}</view>

    <view class="blogger-list">
      <view
        class="blogger-card"
        v-for="(item, index) in bloggers"
        :key="item.id"
        @click="goToBlogger(item.id)"
      >
        <view class="blogger-rank" :style="{ background: index < 3 ? rankColors[index] : '#e0e0e0', color: index < 3 ? '#fff' : '#333' }">
          {{ item.rankNum }}
        </view>
        <view class="blogger-info">
          <view class="blogger-name">{{ item.name }}</view>
          <view class="blogger-tagline">{{ item.tagline }}</view>
        </view>
        <view class="blogger-action">查看作品 →</view>
      </view>
    </view>

    <view class="ad-placeholder">— 底部流量主广告位 —</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../utils/api.js'

const track = ref({})
const bloggers = ref([])
const rankColors = ['#ff4d4f', '#ff7a45', '#ffa940']

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const trackId = currentPage.options?.id || currentPage.$page?.options?.id
  if (trackId) {
    loadData(trackId)
  }
})

function loadData(trackId) {
  api.getTrack(trackId).then(data => {
    track.value = data
    uni.setNavigationBarTitle({
      title: `${data.icon} ${data.name}`
    })
  }).catch(() => {
    uni.showToast({ title: '加载失败', icon: 'none' })
  })

  api.getBloggersByTrack(trackId).then(data => {
    bloggers.value = data
  }).catch(() => {
    uni.showToast({ title: '加载失败', icon: 'none' })
  })
}

function goToBlogger(bloggerId) {
  uni.navigateTo({
    url: `/pages/blogger/blogger?id=${bloggerId}`
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
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.blogger-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.blogger-card {
  background: #fff;
  padding: 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.blogger-rank {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.blogger-info {
  flex: 1;
  min-width: 0;
}

.blogger-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
}

.blogger-tagline {
  font-size: 11px;
  color: #555;
  margin-top: 2px;
}

.blogger-action {
  font-size: 11px;
  color: #07c160;
  font-weight: 500;
  flex-shrink: 0;
}

.ad-placeholder {
  margin-top: 12px;
  font-size: 11px;
  color: #666;
  text-align: center;
}
</style>

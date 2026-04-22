<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'

const route = useRoute()
const router = useRouter()
const { isMobile } = useViewport()
const trackId = route.query.trackId || '1'
const bloggerId = route.query.bloggerId || '1'

const selectedTitle = ref('')
const confirmOpen = ref(false)

const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const isExpired = computed(() => {
  const expire = user.value.expireDate
  if (!expire) return false
  return new Date(expire + 'T23:59:59') < new Date()
})

function checkExpired() {
  if (isExpired.value) {
    message.warning('账号已到期，请联系管理员续费')
    return true
  }
  return false
}

const titles = ref([
  { text: '人到中年，这三件事越早明白越好', tag: '推荐' },
  { text: '35岁以后，真正的成熟是学会做减法', tag: '高潜力' },
  { text: '为什么越往后走，越觉得健康比钱重要？', tag: '热点结合' },
  { text: '中年人的社交，贵在质量而非数量', tag: '共鸣型' },
  { text: '心态好的人，大都做到了这三点', tag: '实用型' },
])

function selectTitle(t) {
  if (checkExpired()) return
  selectedTitle.value = t.text
  confirmOpen.value = true
}

function confirmCreate() {
  if (checkExpired()) return
  confirmOpen.value = false
  router.push(`/app/create?trackId=${trackId}&bloggerId=${bloggerId}&title=${encodeURIComponent(selectedTitle.value)}`)
}
</script>

<template>
  <div :style="{ maxWidth: '800px', margin: '0 auto', padding: isMobile ? '20px 12px 32px' : '32px 0 48px' }">
    <div v-if="isExpired" style="background: #fff7ed; border: 1px solid #fed7aa; border-radius: 10px; padding: 12px 16px; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; font-size: 13px; color: #c2410c;">
      <span style="font-size: 14px;">⚠️</span>
      <span>账号已到期，暂不能选择创作方向，请联系管理员续费</span>
    </div>

    <div @click="router.back()" style="font-size: 14px; color: #6b7280; margin-bottom: 20px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;">
      ← 返回赛道详情
    </div>

    <div style="text-align: center; margin-bottom: 32px;">
      <h1 style="font-size: 24px; font-weight: 700; margin-bottom: 8px; color: #111827;">今日推荐创作方向</h1>
      <p style="font-size: 14px; color: #6b7280;">基于 <span style="color: #2563eb; font-weight: 500;">情感故事</span> / <span style="color: #2563eb; font-weight: 500;">深夜情感电台</span> 的爆款特征生成</p>
    </div>

    <div style="display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px;">
      <div
        v-for="t in titles"
        :key="t.text"
        @click="selectTitle(t)"
        style="background: #fff; border: 2px solid #f1f5f9; border-radius: 12px; padding: 20px 24px; cursor: pointer; transition: all 0.15s; display: flex; align-items: center; justify-content: space-between;"
        @mouseenter="$event.currentTarget.style.borderColor = '#bfdbfe'; $event.currentTarget.style.boxShadow = '0 4px 12px rgba(37,99,235,0.06)'"
        @mouseleave="$event.currentTarget.style.borderColor = '#f1f5f9'; $event.currentTarget.style.boxShadow = 'none'"
      >
        <div style="font-size: 16px; font-weight: 500; color: #111827;">{{ t.text }}</div>
        <div style="font-size: 12px; padding: 4px 10px; background: #eff6ff; color: #2563eb; border-radius: 6px; font-weight: 500; white-space: nowrap;">{{ t.tag }}</div>
      </div>
    </div>

    <div style="text-align: center;">
      <button style="padding: 10px 20px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; color: #374151; font-weight: 500; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;">
        🔄 换一批推荐
      </button>
    </div>

    <div style="margin-top: 32px; padding: 16px 20px; background: #fff; border: 1px solid #f1f5f9; border-radius: 10px;">
      <div style="font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 8px;">💡 选题建议</div>
      <ul style="font-size: 13px; color: #6b7280; line-height: 1.8; padding-left: 16px; margin: 0;">
        <li>选择与你账号定位最契合的标题，保持内容一致性</li>
        <li>推荐标签的标题通常与参考文章风格最接近</li>
        <li>进入创作页后，可在半人工和零人工模式间切换</li>
      </ul>
    </div>
  </div>

  <!-- Confirm Modal -->
  <Modal v-model:open="confirmOpen" width="400" :footer="null" :closable="false">
    <div style="text-align: center; padding: 8px;">
      <div style="width: 48px; height: 48px; background: #eff6ff; color: #2563eb; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22px; margin: 0 auto 16px;">✏️</div>
      <div style="font-size: 17px; font-weight: 600; margin-bottom: 8px; color: #111827;">前往创作中心？</div>
      <div style="font-size: 14px; color: #6b7280; margin-bottom: 20px; line-height: 1.5;">已选择标题：<br/><strong style="color: #111827;">{{ selectedTitle }}</strong></div>
      <div style="display: flex; gap: 10px;">
        <button @click="confirmOpen = false" style="flex: 1; padding: 11px 16px; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; border: none; background: #f3f4f6; color: #374151;">取消</button>
        <button @click="confirmCreate" style="flex: 1; padding: 11px 16px; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; border: none; background: #2563eb; color: #fff;">去创作</button>
      </div>
    </div>
  </Modal>
</template>

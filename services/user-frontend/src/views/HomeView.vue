<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Card, message } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { usePlatformStore } from '../stores/platform.js'
import { listTracks } from '../api/track.js'
import { getStats } from '../api/stats.js'

const router = useRouter()
const { isMobile } = useViewport()
const platformStore = usePlatformStore()
const tracks = ref([])
const stats = ref([])

const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const isExpired = computed(() => {
  const expire = user.value.expireDate
  if (!expire) return false
  return new Date(expire + 'T23:59:59') < new Date()
})

function goToTrack(trackId) {
  if (isExpired.value) {
    message.warning('账号已到期，请联系管理员续费')
    return
  }
  router.push(`/app/track/${trackId}`)
}

const filteredTracks = computed(() => {
  return tracks.value.filter(t => platformStore.trackMatches(t.platforms))
})

function formatNumber(n) {
  if (n == null) return '0'
  return n.toLocaleString()
}

async function loadStats() {
  try {
    const data = await getStats()
    stats.value = [
      { label: '历史推荐文章', value: formatNumber(data.totalSubscriptionPosts), trend: '累计推荐文章数量' },
      { label: '累计赛道数', value: formatNumber(data.totalTracks), trend: `↑ +${formatNumber(data.weekNewTracks)} 本周新增` },
      { label: '头部博主', value: formatNumber(data.totalBloggers), trend: `↑ +${formatNumber(data.weekNewBloggers)} 本周新增` },
      { label: '爆款文章库', value: formatNumber(data.totalPosts), trend: `↑ +${formatNumber(data.weekNewPosts)} 本周新增` },
    ]
  } catch (e) {
    stats.value = [
      { label: '历史推荐文章', value: '0', trend: '-' },
      { label: '累计赛道数', value: '0', trend: '-' },
      { label: '头部博主', value: '0', trend: '-' },
      { label: '爆款文章库', value: '0', trend: '-' },
    ]
  }
}

const colorMap = [
  { keys: ['情感', '母婴', '恋爱', '婚姻'], gradient: 'linear-gradient(135deg, #f472b6 0%, #ec4899 100%)' },
  { keys: ['科技', '数码', '互联网', 'AI', '人工智能'], gradient: 'linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)' },
  { keys: ['职场', '成长', '励志', '心理'], gradient: 'linear-gradient(135deg, #fb923c 0%, #f97316 100%)' },
  { keys: ['健康', '养生', '医疗', '健身'], gradient: 'linear-gradient(135deg, #4ade80 0%, #22c55e 100%)' },
  { keys: ['汽车', '交通', '出行'], gradient: 'linear-gradient(135deg, #94a3b8 0%, #64748b 100%)' },
  { keys: ['房产', '家居', '装修', '生活'], gradient: 'linear-gradient(135deg, #fdba74 0%, #f59e0b 100%)' },
  { keys: ['美食', '烹饪', '饮食', '厨房'], gradient: 'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)' },
  { keys: ['旅游', '旅行', '户外', '风景'], gradient: 'linear-gradient(135deg, #22d3ee 0%, #06b6d4 100%)' },
  { keys: ['财经', '金融', '投资', '理财', '商业'], gradient: 'linear-gradient(135deg, #fcd34d 0%, #eab308 100%)' },
  { keys: ['教育', '学习', '考试', '知识'], gradient: 'linear-gradient(135deg, #818cf8 0%, #6366f1 100%)' },
  { keys: ['娱乐', '明星', '八卦', '影视', '综艺'], gradient: 'linear-gradient(135deg, #c084fc 0%, #a855f7 100%)' },
  { keys: ['军事', '历史', '文化', '国学'], gradient: 'linear-gradient(135deg, #a8a29e 0%, #78716c 100%)' },
  { keys: ['体育', '运动', '球类', '赛事'], gradient: 'linear-gradient(135deg, #f87171 0%, #ef4444 100%)' },
  { keys: ['时尚', '穿搭', '美妆', '护肤'], gradient: 'linear-gradient(135deg, #e879f9 0%, #d946ef 100%)' },
]

function getTrackColor(name) {
  if (!name) return 'linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)'
  for (const item of colorMap) {
    if (item.keys.some(k => name.includes(k))) return item.gradient
  }
  return 'linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)'
}

function parseCover(coverJson) {
  if (!coverJson) return null
  try {
    return JSON.parse(coverJson)
  } catch (e) {
    return null
  }
}

onMounted(async () => {
  loadStats()
  try {
    const data = await listTracks()
    tracks.value = data.map((t) => {
      const parsed = parseCover(t.coverJson)
      return {
        ...t,
        bloggerCount: t.bloggerCount || 0,
        postCount: t.postCount || 0,
        cover: parsed || { gradient: getTrackColor(t.name) },
        platforms: t.platforms || '公众号 · 今日头条',
      }
    })
  } catch (e) {
    // fallback mock
    tracks.value = [
      { id: 1, name: '情感故事', bloggerCount: 420, postCount: '3.2k', platforms: '公众号 · 今日头条', cover: { gradient: getTrackColor('情感故事') } },
      { id: 2, name: '科技数码', bloggerCount: 380, postCount: '2.8k', platforms: '公众号 · 今日头条', cover: { gradient: getTrackColor('科技数码') } },
      { id: 3, name: '职场成长', bloggerCount: 290, postCount: '1.9k', platforms: '公众号', cover: { gradient: getTrackColor('职场成长') } },
      { id: 4, name: '健康养生', bloggerCount: 150, postCount: '980', platforms: '今日头条', cover: { gradient: getTrackColor('健康养生') } },
    ]
  }
})
</script>

<template>
  <div :style="{ maxWidth: '1200px', margin: '0 auto', padding: isMobile ? '0 12px 32px' : '0 24px 48px' }">
    <div :style="{ padding: isMobile ? '24px 0 20px' : '48px 0 32px' }">
      <h1 :style="{ fontSize: isMobile ? '22px' : '32px', fontWeight: 700, marginBottom: '8px', color: '#111827' }">发现热门赛道，让 AI 帮你创作</h1>
      <p style="font-size: 14px; color: #6b7280;">汇聚公众号、今日头条头部博主数据，为你生成爆款选题和优质内容</p>
    </div>

    <div :style="{ display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(4, 1fr)', gap: isMobile ? '12px' : '20px', marginBottom: isMobile ? '32px' : '48px' }">
      <Card v-for="s in stats" :key="s.label" style="border-radius: 16px; border: 1px solid #f1f5f9; box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
        <div style="font-size: 13px; color: #6b7280; margin-bottom: 6px;">{{ s.label }}</div>
        <div :style="{ fontSize: isMobile ? '22px' : '32px', fontWeight: 700, color: '#111827', marginBottom: '4px' }">{{ s.value }}</div>
        <div style="font-size: 12px; color: #10b981; font-weight: 500;">{{ s.trend }}</div>
      </Card>
    </div>

    <h2 style="font-size: 18px; font-weight: 600; margin-bottom: 16px; color: #111827;">热门赛道</h2>

    <div v-if="filteredTracks.length === 0" style="text-align: center; padding: 60px 20px; color: #9ca3af; background: #fff; border-radius: 12px; border: 1px solid #f1f5f9;">
      当前平台暂无相关赛道数据
    </div>

    <div v-else :style="{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(4, 1fr)', gap: isMobile ? '12px' : '20px' }">
      <div
        v-for="t in filteredTracks"
        :key="t.id"
        @click="goToTrack(t.id)"
        style="background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #f1f5f9; box-shadow: 0 1px 2px rgba(0,0,0,0.04); cursor: pointer; transition: all 0.2s; display: flex; flex-direction: column; position: relative;"
        @mouseenter="$event.currentTarget.style.transform = 'translateY(-3px)'; $event.currentTarget.style.boxShadow = '0 12px 32px rgba(0,0,0,0.08)'; $event.currentTarget.style.borderColor = '#e2e8f0'"
        @mouseleave="$event.currentTarget.style.transform = 'none'; $event.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.04)'; $event.currentTarget.style.borderColor = '#f1f5f9'"
      >
        <div style="height: 4px; width: 100%;" :style="{ background: t.cover?.gradient || 'linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)' }"></div>
        <div style="padding: 24px 20px 20px; flex: 1; display: flex; flex-direction: column;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
            <div style="font-size: 17px; font-weight: 600; color: #111827;">{{ t.name }}</div>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
          </div>
          <div style="font-size: 13px; color: #6b7280; line-height: 1.6; margin-bottom: 16px; flex: 1;">
            <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 4px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <span>{{ t.bloggerCount }} 位头部博主</span>
            </div>
            <div style="display: flex; align-items: center; gap: 6px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
              <span>{{ t.postCount }} 篇爆款文章</span>
            </div>
          </div>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span style="display: inline-block; padding: 3px 10px; background: #f1f5f9; border-radius: 6px; font-size: 12px; color: #475569; font-weight: 500;">{{ t.platforms || '-' }}</span>
            <span style="font-size: 12px; color: #2563eb; font-weight: 500;">进入赛道 →</span>
          </div>
        </div>
      </div>
    </div>

    <div style="text-align: center; padding: 32px; font-size: 13px; color: #9ca3af; margin-top: 24px;">
      © 2026 Aicloud · 请联系管理员开通账号后使用完整功能
    </div>
  </div>
</template>

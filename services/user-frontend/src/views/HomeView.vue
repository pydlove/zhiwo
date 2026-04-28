<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Card, message, Modal } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { usePlatformStore } from '../stores/platform.js'
import { usePermissions } from '../composables/usePermissions.js'
import { listTracks } from '../api/track.js'
import { getStats } from '../api/stats.js'
import { listUserTracks, addUserTrack, removeUserTrack } from '../api/userTrack.js'

const router = useRouter()
const { isMobile } = useViewport()
const platformStore = usePlatformStore()
const { trackLimit } = usePermissions()
const tracks = ref([])
const stats = ref([])
const userTracks = ref([])

const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const isExpired = computed(() => {
  const expire = user.value.expireDate
  if (!expire) return false
  return new Date(expire + 'T23:59:59') < new Date()
})

const subscribedTrackIds = computed(() => new Set(userTracks.value.map(ut => ut.trackId)))

// 当前平台已订阅的赛道数量（按平台独立计算限制）
const platformSubscribedCount = computed(() => {
  return userTracks.value.filter(ut => {
    const track = tracks.value.find(t => t.id === ut.trackId)
    return track && platformStore.trackMatches(track.platforms)
  }).length
})

// 超出 trackLimit 的历史订阅自动冻结（按平台独立计算）
const frozenTrackIds = computed(() => {
  const limit = trackLimit.value
  if (limit <= 0) return new Set()
  const platformTracks = userTracks.value.filter(ut => {
    const track = tracks.value.find(t => t.id === ut.trackId)
    return track && platformStore.trackMatches(track.platforms)
  })
  const frozen = platformTracks.slice(limit)
  return new Set(frozen.map(ut => ut.trackId))
})

const hasFrozenTracks = computed(() => frozenTrackIds.value.size > 0)

const subscribedTracks = computed(() => {
  return tracks.value
    .filter(t => subscribedTrackIds.value.has(t.id))
    .filter(t => platformStore.trackMatches(t.platforms))
})

const unsubscribedTracks = computed(() => {
  return tracks.value
    .filter(t => !subscribedTrackIds.value.has(t.id))
    .filter(t => platformStore.trackMatches(t.platforms))
})

async function subscribeTrack(track) {
  if (!user.value.id) {
    message.error('用户未登录')
    return false
  }
  if (isExpired.value) {
    message.warning('账号已到期，请联系管理员续费')
    return false
  }
  const limit = trackLimit.value
  if (limit > 0 && platformSubscribedCount.value >= limit) {
    message.warning(`您当前的权益最多可选择 ${limit} 个赛道，如需更多请联系管理员`)
    return false
  }
  try {
    await addUserTrack(user.value.id, track.id)
    userTracks.value.push({ userId: user.value.id, trackId: track.id })
    message.success(`已订阅「${track.name}」赛道`)
    return true
  } catch (e) {
    message.error(e?.message || '订阅失败')
    return false
  }
}

function goToTrack(track) {
  if (isExpired.value) {
    message.warning('账号已到期，请联系管理员续费')
    return
  }
  const isSubscribed = subscribedTrackIds.value.has(track.id)
  if (isSubscribed) {
    if (frozenTrackIds.value.has(track.id)) {
      Modal.confirm({
        title: '赛道已冻结',
        content: `「${track.name}」赛道已超出您当前权益支持的赛道数量，暂时无法查看。您可以取消其他订阅释放额度，或联系管理员升级套餐。`,
        okText: '知道了',
        cancelButtonProps: { style: { display: 'none' } },
      })
      return
    }
    router.push(`/app/track/${track.id}`)
    return
  }
  // 未订阅：根据权益判断
  const limit = trackLimit.value
  if (limit > 0 && platformSubscribedCount.value >= limit) {
    Modal.confirm({
      title: '订阅额度已满',
      content: `您当前权益在${platformStore.current}平台最多支持 ${limit} 个赛道，已订阅 ${platformSubscribedCount.value} 个（含冻结）。如需订阅新赛道，请先取消部分现有订阅或升级套餐。`,
      okText: '知道了',
      cancelButtonProps: { style: { display: 'none' } },
    })
    return
  }
  if (limit > 0) {
    Modal.confirm({
      title: `订阅「${track.name}」赛道？`,
      content: `您当前权益在${platformStore.current}平台最多支持 ${limit} 个赛道。订阅后如需更换，请先取消现有订阅。`,
      okText: '确认订阅',
      cancelText: '取消',
      async onOk() {
        const ok = await subscribeTrack(track)
        if (ok) {
          router.push(`/app/track/${track.id}`)
        }
      }
    })
  } else {
    subscribeTrack(track).then(ok => {
      if (ok) router.push(`/app/track/${track.id}`)
    })
  }
}

async function unsubscribeTrack(track, event) {
  if (event) event.stopPropagation()
  if (!user.value.id) {
    message.error('用户未登录')
    return
  }
  try {
    await removeUserTrack(user.value.id, track.id)
    userTracks.value = userTracks.value.filter(ut => ut.trackId !== track.id)
    message.success(`已取消订阅「${track.name}」赛道`)
  } catch (e) {
    message.error(e?.message || '取消订阅失败')
  }
}

function onTrackMouseEnter(track, event) {
  if (frozenTrackIds.value.has(track.id)) return
  event.currentTarget.style.transform = 'translateY(-3px)'
  event.currentTarget.style.boxShadow = '0 12px 32px rgba(0,0,0,0.08)'
  event.currentTarget.style.borderColor = '#e2e8f0'
}

function onTrackMouseLeave(event) {
  event.currentTarget.style.transform = 'none'
  event.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.04)'
  event.currentTarget.style.borderColor = '#f1f5f9'
}

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
    const [data, utData] = await Promise.all([
      listTracks(),
      user.value.id ? listUserTracks(user.value.id).catch(() => []) : Promise.resolve([])
    ])
    userTracks.value = utData || []
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

    <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
      <h2 style="font-size: 18px; font-weight: 600; color: #111827; margin: 0;">我的订阅</h2>
      <span v-if="trackLimit > 0" style="font-size: 12px; color: #6b7280; background: #f1f5f9; padding: 2px 10px; border-radius: 4px;">
        {{ platformSubscribedCount }} / {{ trackLimit }} 个赛道
      </span>
      <span v-if="hasFrozenTracks" style="font-size: 12px; color: #b45309; background: #fef3c7; padding: 2px 10px; border-radius: 4px; display: flex; align-items: center; gap: 4px;">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#b45309" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        有 {{ frozenTrackIds.size }} 个赛道已冻结，请取消释放额度或升级套餐，如需调整订阅的赛道，请联系管理员
      </span>
    </div>

    <div v-if="subscribedTracks.length === 0" style="text-align: center; padding: 60px 20px; color: #9ca3af; background: #fff; border-radius: 12px; border: 1px solid #f1f5f9;">
      <div style="font-size: 15px; margin-bottom: 8px;">暂无订阅赛道</div>
      <div style="font-size: 13px;">点击下方「发现更多赛道」浏览并订阅感兴趣的赛道</div>
    </div>

    <div v-else :style="{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(4, 1fr)', gap: isMobile ? '12px' : '20px' }">
      <div
        v-for="t in subscribedTracks"
        :key="t.id"
        @click="goToTrack(t)"
        :style="{
          background: '#fff',
          borderRadius: '12px',
          overflow: 'hidden',
          border: '1px solid #f1f5f9',
          boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
          cursor: frozenTrackIds.has(t.id) ? 'not-allowed' : 'pointer',
          transition: 'all 0.2s',
          display: 'flex',
          flexDirection: 'column',
          position: 'relative',
          opacity: frozenTrackIds.has(t.id) ? 0.7 : 1,
        }"
        @mouseenter="onTrackMouseEnter(t, $event)"
        @mouseleave="onTrackMouseLeave($event)"
      >
        <div
          style="height: 4px; width: 100%;"
          :style="{ background: frozenTrackIds.has(t.id)
            ? 'linear-gradient(135deg, #94a3b8 0%, #64748b 100%)'
            : 'linear-gradient(135deg, #4ade80 0%, #22c55e 100%)'
          }"
        ></div>
        <div style="padding: 24px 20px 20px; flex: 1; display: flex; flex-direction: column;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
              <div style="font-size: 17px; font-weight: 600; color: #111827;">{{ t.name }}</div>
              <span
                v-if="frozenTrackIds.has(t.id)"
                style="display: inline-flex; align-items: center; gap: 2px; padding: 1px 6px; background: #f3f4f6; border: 1px solid #d1d5db; border-radius: 4px; font-size: 11px; color: #6b7280; font-weight: 600;"
              >
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#6b7280" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/></svg>
                已冻结
              </span>
              <span v-if="t.isHot" style="display: inline-flex; align-items: center; gap: 2px; padding: 1px 6px; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 4px; font-size: 11px; color: #f5222d; font-weight: 600;">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="#f5222d"><path d="M12 23c6.075 0 11-4.925 11-11S18.075 1 12 1 1 5.925 1 12s4.925 11 11 11zm0-2c-4.97 0-9-4.03-9-9 0-2.76 1.24-5.23 3.19-6.88.35 1.03.85 2.03 1.47 2.95.62.92 1.37 1.73 2.22 2.4.43.34.88.64 1.35.9.24-.56.37-1.18.37-1.82 0-1.38-.56-2.63-1.46-3.54C10.87 4.63 12.93 4 15 4c.34 0 .67.02 1 .06C16.97 5.6 18 7.68 18 10c0 1.1-.22 2.14-.62 3.1-.4.95-.98 1.8-1.7 2.5-.72.7-1.57 1.26-2.5 1.64-.93.38-1.94.58-2.97.61.17.28.36.54.58.78.22.24.46.46.72.65.26.19.54.35.84.48.3.13.61.23.93.29-.43.52-.95.97-1.55 1.33-.6.36-1.27.62-1.99.77-.72.15-1.47.2-2.23.13-.76-.07-1.49-.25-2.18-.53C5.56 19.3 6.5 18.2 7.72 17.36c.61-.42 1.3-.75 2.05-.97.75-.22 1.54-.33 2.33-.32-.08-.26-.14-.53-.18-.8-.04-.27-.06-.55-.06-.82 0-.55.09-1.09.26-1.6.17-.51.42-.98.73-1.4-.35.1-.69.23-1.01.4-.32.17-.62.37-.89.6-.27.23-.51.49-.72.78-.21.29-.39.6-.53.93-.14.33-.24.68-.3 1.04-.06.36-.08.73-.06 1.1.02.37.08.73.18 1.08.1.35.24.69.42 1 .18.31.4.6.65.85.25.25.53.47.84.65.31.18.65.32 1 .42.35.1.71.16 1.08.18.37.02.74 0 1.1-.06.36-.06.71-.16 1.04-.3.33-.14.64-.32.93-.53.29-.21.55-.45.78-.72.23-.27.43-.57.6-.89.17-.32.3-.66.4-1.01z"/></svg>
                热门
              </span>
            </div>
            <svg v-if="!frozenTrackIds.has(t.id)" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
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
            <span v-if="frozenTrackIds.has(t.id)" style="font-size: 12px; color: #9ca3af; font-weight: 500;">无法查看</span>
            <span v-else style="font-size: 12px; color: #2563eb; font-weight: 500;">进入赛道 →</span>
          </div>
          <div v-if="frozenTrackIds.has(t.id)" style="margin-top: 12px; display: flex; justify-content: flex-end;">
            <button
              @click.stop="unsubscribeTrack(t, $event)"
              style="padding: 4px 12px; font-size: 12px; border-radius: 6px; border: 1px solid #e5e7eb; background: #fff; color: #ef4444; cursor: pointer; font-weight: 500;"
            >取消订阅</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 发现更多赛道 -->
    <div style="margin-top: 48px;">
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
        <h2 style="font-size: 18px; font-weight: 600; color: #111827; margin: 0;">发现更多赛道</h2>
      </div>

      <div v-if="unsubscribedTracks.length === 0" style="text-align: center; padding: 40px 20px; color: #9ca3af; background: #fff; border-radius: 12px; border: 1px solid #f1f5f9;">
        暂无更多可订阅赛道
      </div>

      <div v-else :style="{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(4, 1fr)', gap: isMobile ? '12px' : '20px' }">
        <div
          v-for="t in unsubscribedTracks"
          :key="t.id"
          @click="goToTrack(t)"
          style="background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #f1f5f9; box-shadow: 0 1px 2px rgba(0,0,0,0.04); cursor: pointer; transition: all 0.2s; display: flex; flex-direction: column; position: relative; opacity: 0.85;"
          @mouseenter="$event.currentTarget.style.transform = 'translateY(-3px)'; $event.currentTarget.style.boxShadow = '0 12px 32px rgba(0,0,0,0.08)'; $event.currentTarget.style.borderColor = '#e2e8f0'; $event.currentTarget.style.opacity = '1'"
          @mouseleave="$event.currentTarget.style.transform = 'none'; $event.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.04)'; $event.currentTarget.style.borderColor = '#f1f5f9'; $event.currentTarget.style.opacity = '0.85'"
        >
          <div style="height: 4px; width: 100%;" :style="{ background: t.isHot ? 'linear-gradient(135deg, #f87171 0%, #ef4444 100%)' : 'linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)' }"></div>
          <div style="padding: 24px 20px 20px; flex: 1; display: flex; flex-direction: column;">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
              <div style="display: flex; align-items: center; gap: 8px;">
                <div style="font-size: 17px; font-weight: 600; color: #111827;">{{ t.name }}</div>
                <span v-if="t.isHot" style="display: inline-flex; align-items: center; gap: 2px; padding: 1px 6px; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 4px; font-size: 11px; color: #f5222d; font-weight: 600;">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="#f5222d"><path d="M12 23c6.075 0 11-4.925 11-11S18.075 1 12 1 1 5.925 1 12s4.925 11 11 11zm0-2c-4.97 0-9-4.03-9-9 0-2.76 1.24-5.23 3.19-6.88.35 1.03.85 2.03 1.47 2.95.62.92 1.37 1.73 2.22 2.4.43.34.88.64 1.35.9.24-.56.37-1.18.37-1.82 0-1.38-.56-2.63-1.46-3.54C10.87 4.63 12.93 4 15 4c.34 0 .67.02 1 .06C16.97 5.6 18 7.68 18 10c0 1.1-.22 2.14-.62 3.1-.4.95-.98 1.8-1.7 2.5-.72.7-1.57 1.26-2.5 1.64-.93.38-1.94.58-2.97.61.17.28.36.54.58.78.22.24.46.46.72.65.26.19.54.35.84.48.3.13.61.23.93.29-.43.52-.95.97-1.55 1.33-.6.36-1.27.62-1.99.77-.72.15-1.47.2-2.23.13-.76-.07-1.49-.25-2.18-.53C5.56 19.3 6.5 18.2 7.72 17.36c.61-.42 1.3-.75 2.05-.97.75-.22 1.54-.33 2.33-.32-.08-.26-.14-.53-.18-.8-.04-.27-.06-.55-.06-.82 0-.55.09-1.09.26-1.6.17-.51.42-.98.73-1.4-.35.1-.69.23-1.01.4-.32.17-.62.37-.89.6-.27.23-.51.49-.72.78-.21.29-.39.6-.53.93-.14.33-.24.68-.3 1.04-.06.36-.08.73-.06 1.1.02.37.08.73.18 1.08.1.35.24.69.42 1 .18.31.4.6.65.85.25.25.53.47.84.65.31.18.65.32 1 .42.35.1.71.16 1.08.18.37.02.74 0 1.1-.06.36-.06.71-.16 1.04-.3.33-.14.64-.32.93-.53.29-.21.55-.45.78-.72.23-.27.43-.57.6-.89.17-.32.3-.66.4-1.01z"/></svg>
                  热门
                </span>
              </div>
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
              <span style="font-size: 12px; color: #2563eb; font-weight: 500;">点击订阅 →</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div style="text-align: center; padding: 32px; font-size: 13px; color: #9ca3af; margin-top: 24px;">
      © 2026 知我公众号创作助手 · 让每个人都能轻松运营公众号
    </div>
  </div>
</template>

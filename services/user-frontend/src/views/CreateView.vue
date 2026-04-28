<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, Button, Tag, Switch } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { usePermissions } from '../composables/usePermissions.js'
import { renderAsync } from 'docx-preview'
import { usePlatformStore } from '../stores/platform.js'
import { listTracks } from '../api/track.js'
import { listUserTracks, addUserTrack, removeUserTrack } from '../api/userTrack.js'
import { getLatestSubscriptionPost, markSubscriptionPostUsed } from '../api/subscriptionPost.js'
import { listHelps } from '../api/help.js'
import { useEmailConfig } from '../composables/useEmailConfig.js'
import { listRecommendedGuides } from '../api/guide.js'

const router = useRouter()
const { isMobile } = useViewport()
const platformStore = usePlatformStore()
const { trackLimit, canEmailPush, canGuideAccess } = usePermissions()

const step = ref('select-track')
const tracks = ref([])
const userTracks = ref([])
const loadingTracks = ref(false)
const loadingUserTracks = ref(false)
const selectedTrack = ref(null)

const trackSelectModalOpen = ref(false)
const selectedTrackIds = ref([])

const subscriptionPost = ref(null)
const loadingPost = ref(false)

// Help docs sidebar
const helpDocs = ref([])
const helpPreviewOpen = ref(false)
const helpPreviewRecord = ref(null)

// Recommended guides sidebar
const recommendedGuides = ref([])
const guidePreviewOpen = ref(false)
const guidePreviewRecord = ref(null)

async function loadHelpDocs() {
  try {
    const list = await listHelps()
    helpDocs.value = (list || []).filter(h => h.category === '使用说明' && h.status === '已上架')
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (e) {
    console.error('loadHelpDocs error:', e)
  }
}

function openHelpPreview(record) {
  helpPreviewRecord.value = record
  helpPreviewOpen.value = true
}

async function loadRecommendedGuides() {
  try {
    const list = await listRecommendedGuides()
    recommendedGuides.value = (list || []).slice(0, 5)
  } catch (e) {
    console.error('loadRecommendedGuides error:', e)
  }
}

function openGuidePreview(record) {
  guidePreviewRecord.value = record
  guidePreviewOpen.value = true
}

function handleGuideClick(record) {
  if (!canGuideAccess.value) {
    message.warning('您当前的权益暂不支持访问创作技巧内容')
    return
  }
  openGuidePreview(record)
}

// Preview modal state
const previewModalOpen = ref(false)
const previewRecord = ref({})
const previewContent = ref('')
const previewLoading = ref(false)
const docxContainerRef = ref(null)

const previewFileType = computed(() => {
  const name = previewRecord.value.fileName || ''
  if (name.endsWith('.pdf')) return 'pdf'
  if (name.endsWith('.txt') || name.endsWith('.md')) return 'text'
  if (name.endsWith('.docx')) return 'docx'
  if (name.endsWith('.doc')) return 'doc'
  return 'other'
})

const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
const { emailConfig, loading: emailConfigLoading, loadEmailConfig, saveEmailConfig } = useEmailConfig()

async function toggleEmailReceive(val) {
  const uid = user.value.id
  if (!uid) return
  if (!canEmailPush.value) {
    message.warning('您当前的权益暂不支持邮件每日推送')
    return
  }
  if (!emailConfig.value.email) {
    message.warning('请先前往个人中心配置邮箱地址')
    return
  }
  try {
    const newVal = val ? 1 : 0
    await saveEmailConfig(uid, { email: emailConfig.value.email, emailReceive: newVal })
    message.success(newVal === 1 ? '已开启邮件订阅' : '已关闭邮件订阅')
  } catch (e) {
    message.error('设置失败')
  }
}
const isExpired = computed(() => {
  const expire = user.value.expireDate
  if (!expire) return false
  return new Date(expire + 'T23:59:59') < new Date()
})

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

function checkExpired() {
  if (isExpired.value) {
    message.warning('账号已到期，请联系管理员续费')
    return true
  }
  return false
}

async function selectTrack(t) {
  if (checkExpired()) return
  selectedTrack.value = t
  step.value = 'post-detail'
  loadingPost.value = true
  loadHelpDocs()
  try {
    const post = await getLatestSubscriptionPost(user.value.id || '', t.id)
    subscriptionPost.value = post || null
  } catch (e) {
    subscriptionPost.value = null
  } finally {
    loadingPost.value = false
  }
}

function backToSelectTrack() {
  step.value = 'select-track'
  selectedTrack.value = null
  subscriptionPost.value = null
}

async function openFile() {
  if (!subscriptionPost.value?.fileUrl) {
    message.warning('暂无文件')
    return
  }
  previewRecord.value = subscriptionPost.value
  previewModalOpen.value = true
  previewContent.value = ''

  const type = previewFileType.value
  if (type === 'pdf' || type === 'doc' || type === 'other') {
    return
  }
  if (type === 'text') {
    previewLoading.value = true
    try {
      const res = await fetch(subscriptionPost.value.fileUrl)
      const text = await res.text()
      previewContent.value = text
    } catch (e) {
      previewContent.value = '读取文件失败'
    } finally {
      previewLoading.value = false
    }
    return
  }
  if (type === 'docx') {
    previewLoading.value = true
    setTimeout(async () => {
      try {
        const res = await fetch(subscriptionPost.value.fileUrl)
        const blob = await res.blob()
        previewLoading.value = false
        await nextTick()
        if (!docxContainerRef.value) return
        docxContainerRef.value.innerHTML = ''
        await nextTick()
        await renderAsync(blob, docxContainerRef.value, null, {
          className: 'docx-preview',
          inWrapper: false,
        })
      } catch (e) {
        previewLoading.value = false
        await nextTick()
        if (docxContainerRef.value) {
          docxContainerRef.value.innerHTML = '<div style="color:#999;text-align:center;padding:40px;">解析文件失败</div>'
        }
      }
    }, 400)
  }
}

function downloadFile() {
  if (!subscriptionPost.value?.fileUrl) {
    message.warning('暂无文件可下载')
    return
  }
  const a = document.createElement('a')
  a.href = subscriptionPost.value.fileUrl
  a.download = subscriptionPost.value.fileName || 'article'
  a.target = '_blank'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

async function handleMarkUsed() {
  if (!subscriptionPost.value?.id) return
  if (subscriptionPost.value.used) {
    message.info('该文章已标记为使用过')
    return
  }
  try {
    await markSubscriptionPostUsed(subscriptionPost.value.id)
    subscriptionPost.value.used = 1
    message.success('已标记为使用')
  } catch (e) {
    message.error('标记失败')
  }
}

const myTracks = computed(() => {
  const ids = userTracks.value.map(ut => ut.trackId)
  return tracks.value.filter(t => ids.includes(t.id) && platformStore.trackMatches(t.platforms))
})

const availableTracks = computed(() => {
  return tracks.value.filter(t => platformStore.trackMatches(t.platforms))
})

const canAddTrack = computed(() => {
  if (trackLimit.value <= 0) return true
  return myTracks.value.length < trackLimit.value
})

function openTrackSelectModal() {
  if (checkExpired()) return
  trackSelectModalOpen.value = true
  const validTrackIds = availableTracks.value.map(t => t.id)
  selectedTrackIds.value = userTracks.value
    .map(ut => ut.trackId)
    .filter(id => validTrackIds.includes(id))
}

function toggleTrackSelection(tid) {
  const idx = selectedTrackIds.value.indexOf(tid)
  if (idx > -1) {
    selectedTrackIds.value.splice(idx, 1)
  } else {
    if (trackLimit.value > 0 && selectedTrackIds.value.length >= trackLimit.value) {
      message.warning(`您当前的权益最多可选择 ${trackLimit.value} 个赛道，如需更多请联系管理员`)
      return
    }
    selectedTrackIds.value.push(tid)
  }
}

function confirmTrackSelection() {
  const uid = user.value.id
  if (!uid) {
    message.error('用户未登录')
    return
  }
  const currentIds = userTracks.value.map(ut => ut.trackId)
  const validTrackIds = new Set(availableTracks.value.map(t => t.id))
  const toAddIds = selectedTrackIds.value.filter(id => !currentIds.includes(id))
  const toRemoveIds = currentIds.filter(id => !selectedTrackIds.value.includes(id) && validTrackIds.has(id))

  const trackMap = new Map(tracks.value.map(t => [t.id, t.name]))
  const addNames = toAddIds.map(id => trackMap.get(id) || id)
  const removeNames = toRemoveIds.map(id => trackMap.get(id) || id)

  let content = ''
  if (addNames.length && removeNames.length) {
    content = `赛道订阅提醒：${addNames.join('、')}\n取消赛道：${removeNames.join('、')}`
  } else if (addNames.length) {
    content = `赛道订阅提醒：${addNames.join('、')}`
  } else if (removeNames.length) {
    content = `赛道订阅提醒：${removeNames.join('、')}`
  } else {
    trackSelectModalOpen.value = false
    return
  }
  content += '\n\n订阅赛道后不可更改，后期如需更改请联系管理员！'

  Modal.confirm({
    title: '确认订阅此赛道吗？',
    content,
    async onOk() {
      try {
        for (const tid of toRemoveIds) {
          await removeUserTrack(uid, tid)
        }
        for (const tid of toAddIds) {
          await addUserTrack(uid, tid)
        }
        await loadUserTracks()
        trackSelectModalOpen.value = false
        message.success('保存成功')
      } catch (e) {
        message.error(e?.response?.data?.msg || e?.message || '保存失败')
      }
    },
  })
}

async function loadUserTracks() {
  loadingUserTracks.value = true
  try {
    const uid = user.value.id
    if (uid) {
      userTracks.value = await listUserTracks(uid)
    }
  } catch (e) {
    userTracks.value = []
  } finally {
    loadingUserTracks.value = false
  }
}

async function loadTracks() {
  loadingTracks.value = true
  try {
    const tData = await listTracks()
    tracks.value = tData.map((t) => {
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
    message.error('加载赛道失败')
  } finally {
    loadingTracks.value = false
  }
}

onMounted(() => {
  Promise.all([loadTracks(), loadUserTracks(), loadEmailConfig(user.value.id), loadRecommendedGuides()])
})
</script>

<template>
  <!-- Step 1: My Tracks -->
  <div v-if="step === 'select-track'" :style="{ maxWidth: '1200px', margin: '0 auto', padding: isMobile ? '0 0 32px' : '0 0 48px' }">
    <div :style="{ padding: isMobile ? '20px 0 16px' : '32px 0 24px', textAlign: 'center' }">
      <h1 style="font-size: 28px; font-weight: 700; margin-bottom: 8px; color: #111827;">订阅中心</h1>
      <p style="font-size: 15px; color: #6b7280;">
        选择您已订阅的赛道，查看每日推荐文章
        <span v-if="trackLimit > 0" style="margin-left: 8px; font-size: 13px; color: #2563eb; font-weight: 500;">（最多可选 {{ trackLimit }} 个赛道）</span>
      </p>
    </div>

    <div v-if="loadingTracks || loadingUserTracks" style="text-align: center; padding: 80px; color: #9ca3af;">
      <div style="font-size: 15px;">加载中...</div>
    </div>

    <div v-else-if="tracks.length === 0" style="text-align: center; padding: 60px; color: #9ca3af; background: #fff; border-radius: 16px; border: 1px solid #f1f5f9;">
      暂无赛道数据
    </div>

    <div v-else-if="myTracks.length === 0" style="text-align: center; padding: 60px 20px; color: #6b7280; background: #fff; border-radius: 16px; border: 1px solid #f1f5f9;">
      <div style="font-size: 16px; margin-bottom: 12px;">您还没有选择赛道</div>
      <div style="font-size: 13px; margin-bottom: 20px;">请先点击下方按钮选择您要订阅的赛道</div>
      <button @click="openTrackSelectModal" style="padding: 10px 24px; background: #2563eb; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer;">去选择赛道</button>
    </div>

    <div v-else :style="{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(4, 1fr)', gap: isMobile ? '12px' : '20px', marginBottom: '32px' }">
      <div
        v-for="t in myTracks"
        :key="t.id"
        style="background: #fff; border-radius: 12px; overflow: hidden; border: 1px solid #f1f5f9; box-shadow: 0 1px 2px rgba(0,0,0,0.04); transition: all 0.2s; cursor: pointer; display: flex; flex-direction: column;"
        @mouseenter="$event.currentTarget.style.transform = 'translateY(-3px)'; $event.currentTarget.style.boxShadow = '0 12px 32px rgba(0,0,0,0.08)'; $event.currentTarget.style.borderColor = '#e2e8f0'"
        @mouseleave="$event.currentTarget.style.transform = 'none'; $event.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.04)'; $event.currentTarget.style.borderColor = '#f1f5f9'"
        @click="selectTrack(t)"
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
            <span style="font-size: 12px; color: #2563eb; font-weight: 500;">查看推荐 →</span>
          </div>
        </div>
      </div>

      <!-- Add track card -->
      <div
        v-if="canAddTrack"
        @click="openTrackSelectModal"
        style="background: #fff; border-radius: 16px; overflow: hidden; border: 2px dashed #d1d5db; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 220px; cursor: pointer; transition: all 0.15s;"
        @mouseenter="$event.currentTarget.style.borderColor = '#2563eb'; $event.currentTarget.style.background = '#f8fafc'"
        @mouseleave="$event.currentTarget.style.borderColor = '#d1d5db'; $event.currentTarget.style.background = '#fff'"
      >
        <div style="font-size: 32px; color: #9ca3af; margin-bottom: 8px;">+</div>
        <div style="font-size: 14px; color: #6b7280; font-weight: 500;">添加赛道</div>
        <div v-if="trackLimit > 0" style="font-size: 12px; color: #9ca3af; margin-top: 4px;">还可添加 {{ trackLimit - myTracks.length }} 个</div>
      </div>
    </div>

    <!-- Track Select Modal -->
    <Modal v-model:open="trackSelectModalOpen" title="选择赛道" :mask-closable="false" @ok="confirmTrackSelection" :width="isMobile ? '90vw' : 640">
      <div style="margin-top: 12px;">
        <div v-if="trackLimit > 0" style="font-size: 13px; color: #6b7280; margin-bottom: 12px;">已选 {{ selectedTrackIds.length }} / {{ trackLimit }} 个赛道</div>
        <div :style="{ display: 'grid', gridTemplateColumns: isMobile ? 'repeat(2, 1fr)' : 'repeat(3, 1fr)', gap: '12px' }">
          <div
            v-for="t in availableTracks"
            :key="t.id"
            @click="toggleTrackSelection(t.id)"
            :style="{
              padding: '14px',
              borderRadius: '10px',
              border: selectedTrackIds.includes(t.id) ? '2px solid #2563eb' : '1px solid #e5e7eb',
              background: selectedTrackIds.includes(t.id) ? '#eff6ff' : '#fff',
              cursor: 'pointer',
              transition: 'all 0.15s',
              textAlign: 'center'
            }"
          >
            <div style="font-size: 14px; font-weight: 500; color: #111827;">{{ t.name }}</div>
            <div style="font-size: 12px; color: #9ca3af; margin-top: 4px;">{{ t.platforms || '-' }}</div>
          </div>
        </div>
      </div>
    </Modal>
  </div>

  <!-- Step 2: Post Detail -->
  <div v-else :style="{ maxWidth: '1200px', margin: '0 auto', padding: isMobile ? '0 0 32px' : '0 0 48px' }">
    <!-- Title Area -->
    <div :style="{ padding: isMobile ? '16px 0' : '24px 0' }">
      <div @click="backToSelectTrack" style="font-size: 14px; color: #6b7280; margin-bottom: 16px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; transition: color 0.15s;" @mouseenter="$event.currentTarget.style.color = '#2563eb'" @mouseleave="$event.currentTarget.style.color = '#6b7280'">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        返回我的赛道
      </div>
      <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
        <div style="width: 6px; height: 28px; border-radius: 3px;" :style="{ background: selectedTrack?.cover?.gradient || 'linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)' }"></div>
        <h1 :style="{ fontSize: isMobile ? '22px' : '26px', fontWeight: 700, color: '#111827', margin: 0 }">
          {{ selectedTrack?.name }} · 每日推荐
        </h1>
      </div>
      <p style="font-size: 14px; color: #6b7280; margin-left: 18px;">
        <span style="display: inline-flex; align-items: center; gap: 6px;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          为您推荐该赛道今日精选文章
        </span>
      </p>
    </div>

    <!-- Two Column Layout -->
    <div :style="{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '20px' }">
      <!-- Left: Main Content -->
      <div style="flex: 1; min-width: 0;">
        <div v-if="loadingPost" style="text-align: center; padding: 80px 20px; color: #9ca3af; background: #fff; border-radius: 16px; border: 1px solid #e2e8f0; box-shadow: 0 4px 20px rgba(0,0,0,0.04);">
          <div style="font-size: 40px; margin-bottom: 16px;">⏳</div>
          <div style="font-size: 15px; color: #6b7280;">加载文章中...</div>
        </div>

        <div v-else-if="!subscriptionPost" style="text-align: center; padding: 72px 20px; background: #fff; border-radius: 16px; border: 1px solid #e2e8f0; box-shadow: 0 4px 20px rgba(0,0,0,0.04);">
          <div style="font-size: 48px; margin-bottom: 16px;">
            <img src="../assets/images/empty.png" alt="Empty" style="width: 80px; height: 80px;">
          </div>
          <div style="font-size: 17px; font-weight: 600; color: #374151; margin-bottom: 8px;">暂无该赛道的推荐文章</div>
          <div style="font-size: 14px; color: #9ca3af;">管理员正在准备精彩内容，敬请期待</div>
        </div>

        <div v-else style="background: #fff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06);">
          <div style="height: 5px; width: 100%;" :style="{ background: selectedTrack?.cover?.gradient || 'linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%)' }"></div>
          <div style="padding: 28px 32px 32px;">
            <div style="font-size: 21px; font-weight: 700; color: #111827; margin-bottom: 16px; line-height: 1.5;">
              {{ subscriptionPost.title }}
            </div>

            <div v-if="subscriptionPost.description" style="font-size: 15px; color: #4b5563; line-height: 1.8; margin-bottom: 28px;">
              {{ subscriptionPost.description }}
            </div>

            <div :style="{ display: 'flex', gap: '12px', flexWrap: 'wrap', flexDirection: isMobile ? 'column' : 'row' }">
              <button
                v-if="subscriptionPost.fileUrl"
                @click="openFile"
                style="padding: 11px 26px; background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); color: #fff; border: none; border-radius: 10px; font-size: 14px; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 8px; box-shadow: 0 2px 8px rgba(37,99,235,0.25); transition: all 0.15s;"
                @mouseenter="$event.currentTarget.style.transform = 'translateY(-1px)'; $event.currentTarget.style.boxShadow = '0 4px 12px rgba(37,99,235,0.35)';"
                @mouseleave="$event.currentTarget.style.transform = 'none'; $event.currentTarget.style.boxShadow = '0 2px 8px rgba(37,99,235,0.25)';"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                预览文章
              </button>
              <button
                v-if="subscriptionPost.fileUrl"
                @click="downloadFile"
                style="padding: 11px 26px; background: #fff; color: #2563eb; border: 1.5px solid #2563eb; border-radius: 10px; font-size: 14px; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.15s;"
                @mouseenter="$event.currentTarget.style.background = '#eff6ff';"
                @mouseleave="$event.currentTarget.style.background = '#fff';"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                下载文章
              </button>
              <button
                v-if="subscriptionPost.id"
                @click="handleMarkUsed"
                :style="{
                  padding: '11px 26px',
                  borderRadius: '10px',
                  fontSize: '14px',
                  fontWeight: 500,
                  cursor: subscriptionPost.used ? 'default' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  background: subscriptionPost.used ? '#dcfce7' : '#fff',
                  color: subscriptionPost.used ? '#15803d' : '#059669',
                  border: subscriptionPost.used ? '1.5px solid #86efac' : '1.5px solid #059669',
                  transition: 'all 0.15s',
                }"
                @mouseenter="if (!subscriptionPost.used) { $event.currentTarget.style.background = '#d1fae5'; }"
                @mouseleave="if (!subscriptionPost.used) { $event.currentTarget.style.background = '#fff'; }"
              >
                <svg v-if="!subscriptionPost.used" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                {{ subscriptionPost.used ? '已使用' : '标记已使用' }}
              </button>
            </div>

            <div v-if="subscriptionPost.fileName" style="margin-top: 20px; padding: 10px 14px; background: #f8fafc; border-radius: 8px; display: inline-flex; align-items: center; gap: 8px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              <span style="font-size: 13px; color: #6b7280;">{{ subscriptionPost.fileName }}</span>
            </div>
          </div>
        </div>

        <!-- Recommended Guides -->
        <div v-if="recommendedGuides.length" style="margin-top: 20px; background: #fff; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.04);">
          <div style="padding: 16px 20px 12px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #f1f5f9;">
            <div style="width: 5px; height: 18px; border-radius: 3px; background: linear-gradient(135deg, #f59e0b 0%, #ea580c 100%);"></div>
            <div style="font-size: 15px; font-weight: 600; color: #111827; display: flex; align-items: center; gap: 6px;">
              创作技巧推荐
              <svg v-if="!canGuideAccess" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </div>
          </div>
          <div style="padding: 14px 20px 18px;">
            <div :style="{ display: 'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(2, 1fr)', gap: '10px' }">
              <div
                v-for="g in recommendedGuides"
                :key="g.id"
                @click="handleGuideClick(g)"
                :style="{ padding: '14px', borderRadius: '10px', cursor: 'pointer', fontSize: '13px', color: '#374151', background: 'linear-gradient(135deg, #fefce8 0%, #fffbeb 100%)', border: '1px solid #fef08a', transition: 'all 0.15s', opacity: canGuideAccess ? 1 : 0.6 }"
                @mouseenter="if (!canGuideAccess) { $event.currentTarget.style.opacity = '0.8'; } else { $event.currentTarget.style.background = 'linear-gradient(135deg, #fef9c3 0%, #fef3c7 100%)'; $event.currentTarget.style.borderColor = '#f59e0b'; $event.currentTarget.style.transform = 'translateY(-2px)'; $event.currentTarget.style.boxShadow = '0 4px 12px rgba(245,158,11,0.15)'; }"
                @mouseleave="if (!canGuideAccess) { $event.currentTarget.style.opacity = '0.6'; } else { $event.currentTarget.style.background = 'linear-gradient(135deg, #fefce8 0%, #fffbeb 100%)'; $event.currentTarget.style.borderColor = '#fef08a'; $event.currentTarget.style.transform = 'none'; $event.currentTarget.style.boxShadow = 'none'; }"
              >
                <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                  <span style="font-weight: 600; line-height: 1.5; color: #111827;">{{ g.title }}</span>
                </div>
                <div style="font-size: 12px; color: #6b7280; line-height: 1.5; margin-left: 20px;" v-if="g.description">
                  {{ g.description.slice(0, 50) + (g.description.length > 50 ? '...' : '') }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Sidebar -->
      <div :style="{ width: isMobile ? '100%' : '260px', flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '16px' }">
        <!-- Email Config -->
        <div style="background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.04);">
          <div style="padding: 14px 16px 10px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #f1f5f9;">
            <div style="width: 28px; height: 28px; border-radius: 8px; background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%); display: flex; align-items: center; justify-content: center;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
            </div>
            <span style="font-size: 14px; font-weight: 600; color: #111827;">邮件订阅</span>
          </div>
          <div style="padding: 12px 16px 16px;">
            <div v-if="!canEmailPush" style="font-size: 13px; color: #9ca3af; display: flex; align-items: center; gap: 6px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              您当前的权益暂不支持邮件每日推送
            </div>
            <div v-else-if="!emailConfig.email" style="font-size: 13px; color: #9ca3af; display: flex; align-items: center; gap: 6px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              未配置邮箱，请前往个人中心设置
            </div>
            <div v-else style="display: flex; flex-direction: column; gap: 10px;">
              <div style="font-size: 12px; color: #6b7280; word-break: break-all; display: flex; align-items: center; gap: 6px; padding: 6px 10px; background: #f8fafc; border-radius: 6px;">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                {{ emailConfig.email }}
              </div>
              <div style="display: flex; align-items: center; gap: 8px;">
                <Switch
                  :checked="emailConfig.emailReceive === 1"
                  :disabled="emailConfigLoading"
                  @update:checked="toggleEmailReceive"
                  size="small"
                />
                <span style="font-size: 13px; color: #374151; font-weight: 500;">
                  {{ emailConfig.emailReceive === 1 ? '✓ 已开启' : '○ 已关闭' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Help Docs -->
        <div style="background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.04);">
          <div style="padding: 14px 16px 10px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #f1f5f9;">
            <div style="width: 28px; height: 28px; border-radius: 8px; background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%); display: flex; align-items: center; justify-content: center;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            </div>
            <span style="font-size: 14px; font-weight: 600; color: #111827;">使用说明</span>
          </div>
          <div style="padding: 12px 16px 16px;">
            <div v-if="!helpDocs.length" style="color: #9ca3af; font-size: 13px; text-align: center; padding: 16px 0; display: flex; align-items: center; justify-content: center; gap: 6px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              暂无帮助文档
            </div>
            <div style="display: flex; flex-direction: column; gap: 8px;">
              <div
                v-for="h in helpDocs"
                :key="h.id"
                @click="openHelpPreview(h)"
                style="padding: 12px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; color: #374151; background: #f8fafc; border: 1px solid #e2e8f0; transition: all 0.15s;"
                @mouseenter="$event.currentTarget.style.background = '#ecfdf5'; $event.currentTarget.style.borderColor = '#6ee7b7'; $event.currentTarget.style.color = '#059669';"
                @mouseleave="$event.currentTarget.style.background = '#f8fafc'; $event.currentTarget.style.borderColor = '#e2e8f0'; $event.currentTarget.style.color = '#374151';"
              >
                <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 4px;">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                  <span style="font-weight: 600;">{{ h.title }}</span>
                </div>
                <div style="font-size: 12px; color: #6b7280; line-height: 1.5; margin-left: 18px;" v-if="h.summary || h.content">
                  {{ (h.summary || h.content?.replace(/<[^>]+>/g, '') || '').slice(0, 40) + '...' }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Preview Modal -->
  <Modal
    v-model:open="previewModalOpen"
    :title="previewRecord.fileName || '文件预览'"
    :footer="null"
    :width="isMobile ? '95vw' : 900"
    :mask-closable="false"
  >
    <div :style="{ marginTop: '12px', minHeight: '200px', maxHeight: isMobile ? 'calc(80vh - 60px)' : '600px', overflow: 'auto' }">
      <div v-if="previewLoading" style="text-align: center; padding: 80px; color: #999;">
        <div>加载中...</div>
      </div>
      <!-- PDF -->
      <iframe
        v-else-if="previewFileType === 'pdf'"
        :src="previewRecord.fileUrl"
        :style="{ width: '100%', height: isMobile ? 'calc(80vh - 80px)' : '550px', border: 0 }"
      />
      <!-- Text -->
      <pre v-else-if="previewFileType === 'text'" style="padding: 16px; margin: 0; font-family: monospace; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 4px;"
      >{{ previewContent }}</pre>
      <!-- DOCX -->
      <div
        v-else-if="previewFileType === 'docx'"
        ref="docxContainerRef"
        :style="{ padding: isMobile ? '12px' : '24px', background: '#fff', border: '1px solid #f0f0f0', borderRadius: '4px', lineHeight: 1.8, fontSize: '14px' }"
      />
      <!-- DOC / Other -->
      <div v-else style="padding: 80px 24px; text-align: center; color: #999;">
        <div style="font-size: 48px; margin-bottom: 16px;">📄</div>
        <div style="font-size: 14px; margin-bottom: 8px;">该文件格式暂不支持浏览器在线预览</div>
        <div style="font-size: 12px; color: #bbb; margin-bottom: 16px;">请下载后用相应软件打开查看</div>
        <Button type="primary" @click="() => { window.open(previewRecord.fileUrl, '_blank') }" >下载文件</Button>
      </div>
    </div>
  </Modal>

  <!-- Help Doc Preview Modal -->
  <Modal
    v-model:open="helpPreviewOpen"
    :title="helpPreviewRecord?.title || '帮助文档'"
    :footer="null"
    :width="isMobile ? '95vw' : 720"
  >
    <div v-if="helpPreviewRecord" style="padding: 8px 0;">
      <Tag color="blue" style="margin-bottom: 12px;">{{ helpPreviewRecord.category }}</Tag>
      <div class="help-preview-content" style="font-size: 15px; line-height: 1.8; color: #374151; overflow-wrap: break-word;" v-html="helpPreviewRecord.content"></div>
    </div>
  </Modal>

  <!-- Guide Preview Modal -->
  <Modal
    v-model:open="guidePreviewOpen"
    :title="guidePreviewRecord?.title || '创作技巧'"
    :footer="null"
    :width="isMobile ? '95vw' : 720"
  >
    <div v-if="guidePreviewRecord" style="padding: 8px 0;">
      <Tag color="orange" style="margin-bottom: 12px;">{{ guidePreviewRecord.category }}</Tag>
      <div v-if="guidePreviewRecord.link" style="margin-bottom: 12px;">
        <a :href="guidePreviewRecord.link" target="_blank" style="color: #2563eb;">打开外部链接 ↗</a>
      </div>
      <div class="help-preview-content" style="font-size: 15px; line-height: 1.8; color: #374151; overflow-wrap: break-word;" v-html="guidePreviewRecord.content"></div>
    </div>
  </Modal>
</template>

<style>
.help-preview-content img {
  max-width: 100%;
  height: auto;
  display: block;
}
</style>

<style scoped>
:deep(.docx-preview img) {
  max-width: 100%;
  height: auto;
}
:deep(.docx-preview table) {
  max-width: 100%;
  border-collapse: collapse;
}
</style>

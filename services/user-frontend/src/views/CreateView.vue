<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, Button, Tag, Switch } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { renderAsync } from 'docx-preview'
import { usePlatformStore } from '../stores/platform.js'
import { listTracks } from '../api/track.js'
import { listUserTracks, addUserTrack, removeUserTrack } from '../api/userTrack.js'
import { getLatestSubscriptionPost, markSubscriptionPostUsed } from '../api/subscriptionPost.js'
import { listHelps } from '../api/help.js'
import { getEmailConfig, updateEmailConfig } from '../api/user.js'

const router = useRouter()
const { isMobile } = useViewport()
const platformStore = usePlatformStore()

const step = ref('select-track')
const tracks = ref([])
const userTracks = ref([])
const loadingTracks = ref(false)
const loadingUserTracks = ref(false)
const selectedTrack = ref(null)

const trackSelectModalOpen = ref(false)
const selectedTrackIds = ref([])
const trackLimit = computed(() => {
  const u = JSON.parse(localStorage.getItem('user') || '{}')
  return u.trackLimit ?? 0
})

const subscriptionPost = ref(null)
const loadingPost = ref(false)

// Help docs sidebar
const helpDocs = ref([])
const helpPreviewOpen = ref(false)
const helpPreviewRecord = ref(null)

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
const emailConfig = ref({ email: '', emailReceive: 0, canSetEmail: 0 })
const emailConfigLoading = ref(false)

async function loadEmailConfig() {
  const uid = user.value.id
  if (!uid) return
  try {
    const config = await getEmailConfig(uid)
    emailConfig.value = {
      email: config.email || '',
      emailReceive: config.emailReceive || 0,
      canSetEmail: config.canSetEmail || 0,
    }
  } catch (e) {
    // silent fail
  }
}

async function toggleEmailReceive(val) {
  const uid = user.value.id
  if (!uid) return
  if (emailConfig.value.canSetEmail !== 1) {
    message.warning('您暂无权限设置邮箱接收，请联系管理员开通')
    return
  }
  if (!emailConfig.value.email) {
    message.warning('请先前往个人中心配置邮箱地址')
    return
  }
  emailConfigLoading.value = true
  try {
    await updateEmailConfig(uid, { email: emailConfig.value.email, emailReceive: val })
    emailConfig.value.emailReceive = val
    message.success(val === 1 ? '已开启邮件订阅' : '已关闭邮件订阅')
  } catch (e) {
    message.error('设置失败')
  } finally {
    emailConfigLoading.value = false
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
  const toAddIds = selectedTrackIds.value.filter(id => !currentIds.includes(id))
  const toRemoveIds = currentIds.filter(id => !selectedTrackIds.value.includes(id))

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
  Promise.all([loadTracks(), loadUserTracks(), loadEmailConfig()])
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
        <div v-if="trackLimit > 0" style="font-size: 12px; color: #9ca3af; margin-top: 4px;">还可添加 {{ trackLimit - userTracks.length }} 个</div>
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
      <div @click="backToSelectTrack" style="font-size: 14px; color: #6b7280; margin-bottom: 16px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px;">
        ← 返回我的赛道
      </div>
      <h1 :style="{ fontSize: isMobile ? '20px' : '24px', fontWeight: 700, marginBottom: '6px', color: '#111827' }">
        {{ selectedTrack?.name }} · 每日推荐
      </h1>
      <p style="font-size: 14px; color: #6b7280;">为您推荐该赛道今日精选文章</p>
    </div>

    <!-- Two Column Layout -->
    <div :style="{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '16px' }">
      <!-- Left: Main Content -->
      <div style="flex: 1; min-width: 0;">
        <div v-if="loadingPost" style="text-align: center; padding: 60px; color: #9ca3af;">
          <div style="font-size: 15px;">加载文章中...</div>
        </div>

        <div v-else-if="!subscriptionPost" style="text-align: center; padding: 60px 20px; color: #9ca3af; background: #fff; border-radius: 16px; border: 1px solid #f1f5f9;">
          <div style="font-size: 16px; margin-bottom: 8px;">暂无该赛道的推荐文章</div>
          <div style="font-size: 13px;">管理员正在准备精彩内容，敬请期待</div>
        </div>

        <div v-else style="background: #fff; border: 1px solid #f1f5f9; border-radius: 16px; padding: 32px; box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
          <div style="font-size: 20px; font-weight: 700; color: #111827; margin-bottom: 16px; line-height: 1.4;">
            {{ subscriptionPost.title }}
          </div>

          <div v-if="subscriptionPost.description" style="font-size: 15px; color: #4b5563; line-height: 1.8; margin-bottom: 24px;">
            {{ subscriptionPost.description }}
          </div>

          <div :style="{ display: 'flex', gap: '10px', flexWrap: 'wrap', flexDirection: isMobile ? 'column' : 'row' }">
            <button
              v-if="subscriptionPost.fileUrl"
              @click="openFile"
              style="padding: 10px 24px; background: #2563eb; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 6px;"
            >
              预览文章
            </button>
            <button
              v-if="subscriptionPost.fileUrl"
              @click="downloadFile"
              style="padding: 10px 24px; background: #fff; color: #2563eb; border: 1px solid #2563eb; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; display: flex; align-items: center; gap: 6px;"
            >
              下载文章
            </button>
            <button
              v-if="subscriptionPost.id"
              @click="handleMarkUsed"
              :style="{
                padding: '10px 24px',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: 500,
                cursor: subscriptionPost.used ? 'default' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                background: subscriptionPost.used ? '#dcfce7' : '#eff6ff',
                color: subscriptionPost.used ? '#15803d' : '#2563eb',
                border: subscriptionPost.used ? '1px solid #86efac' : '1px solid #2563eb',
              }"
            >
              {{ subscriptionPost.used ? '已使用' : '标记已使用' }}
            </button>
          </div>

          <div v-if="subscriptionPost.fileName" style="margin-top: 16px; font-size: 13px; color: #9ca3af;">
            文件：{{ subscriptionPost.fileName }}
          </div>
        </div>
      </div>

      <!-- Right: Sidebar -->
      <div :style="{ width: isMobile ? '100%' : '260px', flexShrink: 0, display: 'flex', flexDirection: 'column', gap: '16px' }">
        <!-- Email Config -->
        <div style="background: #fff; border: 1px solid #f1f5f9; border-radius: 12px; padding: 16px;">
          <div style="font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px;">
            邮件订阅
          </div>
          <div v-if="emailConfig.canSetEmail !== 1" style="font-size: 12px; color: #9ca3af;">
            您暂无权限设置邮箱接收
          </div>
          <div v-else-if="!emailConfig.email" style="font-size: 12px; color: #9ca3af;">
            未配置邮箱，请前往个人中心设置
          </div>
          <div v-else style="display: flex; flex-direction: column; gap: 8px;">
            <div style="font-size: 12px; color: #6b7280; word-break: break-all;">
              {{ emailConfig.email }}
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <Switch
                :checked="emailConfig.emailReceive === 1"
                :disabled="emailConfigLoading"
                @update:checked="toggleEmailReceive"
                size="small"
              />
              <span style="font-size: 12px; color: #374151;">
                {{ emailConfig.emailReceive === 1 ? '已开启' : '已关闭' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Help Docs -->
        <div style="background: #fff; border: 1px solid #f1f5f9; border-radius: 12px; padding: 16px;">
          <div style="font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px;">
            使用说明
          </div>
          <div v-if="!helpDocs.length" style="color: #999; font-size: 13px; text-align: center; padding: 16px 0;">
            暂无帮助文档
          </div>
          <div style="display: flex; flex-direction: column; gap: 8px;">
            <div
              v-for="h in helpDocs"
              :key="h.id"
              @click="openHelpPreview(h)"
              style="padding: 10px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; color: #595959; background: #fafafa; border: 1px solid #f0f0f0; transition: all 0.15s;"
              @mouseenter="$event.currentTarget.style.background = '#e6f7ff'; $event.currentTarget.style.borderColor = '#91d5ff'; $event.currentTarget.style.color = '#1890ff';"
              @mouseleave="$event.currentTarget.style.background = '#fafafa'; $event.currentTarget.style.borderColor = '#f0f0f0'; $event.currentTarget.style.color = '#595959';"
            >
              <div style="font-weight: 500; line-height: 1.5;">{{ h.title }}</div>
              <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px; line-height: 1.4;" v-if="h.summary || h.content">
                {{ (h.summary || h.content?.replace(/<[^>]+>/g, '') || '').slice(0, 40) + '...' }}
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

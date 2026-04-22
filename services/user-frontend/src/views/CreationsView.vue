<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, Tag } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { listCreations, deleteCreation } from '../api/creation.js'

const router = useRouter()
const { isMobile } = useViewport()

const creations = ref([])
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

async function loadData() {
  try {
    const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
    if (!currentUser.id) {
      message.error('用户未登录')
      return
    }
    const list = await listCreations({ userId: currentUser.id })
    creations.value = list.map(c => ({
      ...c,
      title: c.title || '（无标题）',
      date: c.createdAt ? c.createdAt.slice(0, 10) : '-',
      time: c.createdAt ? c.createdAt.slice(11, 16) : '-',
    }))
  } catch (e) {
    message.error('加载失败')
  }
}

function getLocalDateStr(date = new Date()) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const groups = computed(() => {
  const map = {}
  creations.value.forEach(c => {
    if (!map[c.date]) map[c.date] = []
    map[c.date].push(c)
  })
  const today = getLocalDateStr()
  const yest = getLocalDateStr(new Date(Date.now() - 86400000))
  return Object.keys(map).sort((a, b) => b.localeCompare(a)).map(date => {
    let header = date
    if (date === today) header = '今天 ' + date
    else if (date === yest) header = '昨天 ' + date
    return { header, items: map[date] }
  })
})

function editCreation(c) {
  if (checkExpired()) return
  router.push(`/app/create?draftId=${c.id}&title=${encodeURIComponent(c.title)}`)
}

function deleteCreationItem(c) {
  if (checkExpired()) return
  Modal.confirm({
    title: '删除创作',
    content: `确定删除创作「${c.title}」吗？`,
    async onOk() {
      try {
        await deleteCreation(c.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

const previewOpen = ref(false)
const previewItem = ref(null)

function openPreview(c) {
  previewItem.value = c
  previewOpen.value = true
}

function closePreview() {
  previewOpen.value = false
  previewItem.value = null
}

onMounted(loadData)
</script>

<template>
  <div :style="{ maxWidth: '960px', margin: '0 auto', padding: isMobile ? '0 12px 32px' : '0 24px 48px' }">
    <div style="padding: 32px 0 24px;">
      <h1 style="font-size: 26px; font-weight: 700; margin-bottom: 8px; color: #111827;">我的创作</h1>
      <p style="font-size: 14px; color: #6b7280;">在这里查看和管理您的所有创作记录</p>
    </div>

    <div v-if="isExpired" style="background: #fff7ed; border: 1px solid #fed7aa; border-radius: 10px; padding: 12px 16px; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; font-size: 13px; color: #c2410c;">
      <span style="font-size: 14px;">⚠️</span>
      <span>账号已到期，暂不能编辑和删除创作，请联系管理员续费</span>
    </div>

    <div v-if="creations.length === 0" style="text-align: center; padding: 80px 20px; color: #9ca3af; background: #fff; border-radius: 12px; border: 1px solid #f1f5f9;">
      <div style="font-size: 48px; margin-bottom: 16px;">✨</div>
      <div style="font-size: 16px; font-weight: 500; color: #374151; margin-bottom: 8px;">暂无创作记录</div>
      <div style="font-size: 14px;">快去创作中心开始您的第一篇创作吧</div>
    </div>

    <div v-else style="display: flex; flex-direction: column; gap: 24px;">
      <div v-for="g in groups" :key="g.header">
        <div style="font-size: 14px; font-weight: 600; color: #374151; margin-bottom: 12px; padding-left: 4px;">{{ g.header }}</div>
        <div style="display: flex; flex-direction: column; gap: 12px;">
          <div
            v-for="c in g.items"
            :key="c.id"
            :style="{ background: '#fff', border: '1px solid #f1f5f9', borderRadius: '12px', padding: '18px 20px', display: 'flex', justifyContent: 'space-between', alignItems: isMobile ? 'flex-start' : 'center', transition: 'box-shadow 0.15s', flexDirection: isMobile ? 'column' : 'row' }"
            @mouseenter="$event.currentTarget.style.boxShadow = '0 4px 16px rgba(0,0,0,0.06)'"
            @mouseleave="$event.currentTarget.style.boxShadow = 'none'"
          >
            <div style="flex: 1; min-width: 0;">
              <div :style="{ fontSize: '16px', fontWeight: 600, color: '#111827', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', flexDirection: isMobile ? 'column' : 'row' }">
                <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-block; max-width: 100%;">{{ c.title }}</span>
                <Tag v-if="c.trackName" color="blue" style="font-size: 12px; margin: 0;">{{ c.trackName }}</Tag>
                <Tag v-if="c.mode" style="font-size: 12px; margin: 0;">{{ c.mode }}</Tag>
                <Tag v-if="c.reviewed === 1" color="green" style="font-size: 12px; margin: 0;">已审阅</Tag>
                <Tag v-else color="orange" style="font-size: 12px; margin: 0;">待审阅</Tag>
              </div>
              <div style="font-size: 13px; color: #9ca3af;">创建于 {{ c.date }} {{ c.time }}</div>
            </div>
            <div :style="{ display: 'flex', gap: '8px', marginLeft: isMobile ? '0' : '12px', marginTop: isMobile ? '12px' : '0', flexWrap: 'wrap' }">
              <button
                @click="editCreation(c)"
                style="padding: 8px 16px; background: #2563eb; color: #fff; border: none; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer;"
              >继续编辑</button>
              <button
                @click="openPreview(c)"
                style="padding: 8px 16px; background: #fff; color: #374151; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer;"
              >预览</button>
              <button
                @click="deleteCreationItem(c)"
                style="padding: 8px 16px; background: #fff; color: #f5222d; border: 1px solid #ffccc7; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer;"
              >删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <Modal v-model:open="previewOpen" title="创作预览" width="720" :mask-closable="false" :footer="null" @cancel="closePreview">
    <div v-if="previewItem" style="padding-top: 8px;">
      <div style="display: flex; gap: 16px; font-size: 13px; color: #6b7280; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #f1f5f9; flex-wrap: wrap;">
        <span>赛道：{{ previewItem.trackName || '-' }}</span>
        <span>模式：{{ previewItem.mode || '-' }}</span>
        <span>创建时间：{{ previewItem.date }} {{ previewItem.time }}</span>
      </div>
      <div style="font-size: 15px; line-height: 1.8; color: #374151; padding: 20px; background: #f8fafc; border-radius: 8px; min-height: 200px;">
        <h1 style="font-size: 20px; font-weight: 700; margin: 0 0 16px; color: #111827;">{{ previewItem.title }}</h1>
        <div v-html="previewItem.content"></div>
      </div>
    </div>
  </Modal>
</template>

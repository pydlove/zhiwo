<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { useViewport } from '../composables/useViewport.js'
import { listCreations, deleteCreation } from '../api/creation.js'

const router = useRouter()
const { isMobile } = useViewport()

const drafts = ref([])
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
    const list = await listCreations({ userId: currentUser.id || '' })
    drafts.value = list.map(c => ({
      ...c,
      title: c.title || '（无标题草稿）',
      savedAt: c.createdAt ? c.createdAt.slice(0, 16).replace('T', ' ') : '-',
    }))
  } catch (e) {
    message.error('加载失败')
  }
}

function editDraft(d) {
  if (checkExpired()) return
  router.push(`/app/create?draftId=${d.id}&title=${encodeURIComponent(d.title)}`)
}

function deleteDraft(d) {
  if (checkExpired()) return
  Modal.confirm({
    title: '删除草稿',
    content: `确定删除草稿「${d.title}」吗？`,
    async onOk() {
      try {
        await deleteCreation(d.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

onMounted(loadData)
</script>

<template>
  <div :style="{ maxWidth: '960px', margin: '0 auto', padding: isMobile ? '0 12px 32px' : '0 24px 48px' }">
    <div style="padding: 32px 0 24px;">
      <h1 style="font-size: 26px; font-weight: 700; margin-bottom: 8px; color: #111827;">我的草稿</h1>
      <p style="font-size: 14px; color: #6b7280;">在这里管理你保存的草稿，随时继续编辑或导出</p>
    </div>

    <div v-if="isExpired" style="background: #fff7ed; border: 1px solid #fed7aa; border-radius: 10px; padding: 12px 16px; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; font-size: 13px; color: #c2410c;">
      <span style="font-size: 14px;">⚠️</span>
      <span>账号已到期，暂不能编辑和删除草稿，请联系管理员续费</span>
    </div>

    <div v-if="drafts.length === 0" style="text-align: center; padding: 80px 20px; color: #9ca3af; background: #fff; border-radius: 12px; border: 1px solid #f1f5f9;">
      <div style="font-size: 48px; margin-bottom: 16px;">📝</div>
      <div style="font-size: 16px; font-weight: 500; color: #374151; margin-bottom: 8px;">暂无草稿</div>
      <div style="font-size: 14px;">快去创作中心写点什么吧</div>
    </div>

    <div v-else style="display: flex; flex-direction: column; gap: 12px;">
      <div
        v-for="d in drafts"
        :key="d.id"
        :style="{ background: '#fff', border: '1px solid #f1f5f9', borderRadius: '12px', padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: isMobile ? 'flex-start' : 'center', transition: 'box-shadow 0.15s', flexDirection: isMobile ? 'column' : 'row' }"
      >
        <div>
          <div style="font-size: 16px; font-weight: 600; color: #111827; margin-bottom: 6px;">{{ d.title }}</div>
          <div style="font-size: 13px; color: #9ca3af;">保存于 {{ d.savedAt }}</div>
        </div>
        <div :style="{ display: 'flex', gap: '10px', marginTop: isMobile ? '12px' : '0' }">
          <button
            @click="editDraft(d)"
            style="padding: 8px 16px; background: #2563eb; color: #fff; border: none; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer;"
          >继续编辑</button>
          <button
            @click="deleteDraft(d)"
            style="padding: 8px 16px; background: #fff; color: #f5222d; border: 1px solid #ffccc7; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer;"
          >删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

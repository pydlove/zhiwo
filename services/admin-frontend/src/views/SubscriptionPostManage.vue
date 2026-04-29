<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message, Pagination, DatePicker } from 'ant-design-vue'
import { renderAsync } from 'docx-preview'

import { listSubscriptionPosts, saveSubscriptionPost, deleteSubscriptionPost } from '../api/subscriptionPost.js'
import { listUsers } from '../api/user.js'
import { listTracks } from '../api/track.js'

const search = ref('')
const userFilter = ref(undefined)
const trackFilter = ref(undefined)
const statusFilter = ref(undefined)
const startDate = ref(null)
const endDate = ref(null)

const rawData = ref([])
const users = ref([])
const tracks = ref([])

const columns = [
  { title: '文章标题', dataIndex: 'title', key: 'title', ellipsis: true, width: 200 },
  { title: '目标用户', key: 'userName', width: 110 },
  { title: '所属赛道', key: 'trackName', width: 110 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true, width: 200 },
  { title: '文件', key: 'file', width: 180 },
  { title: '使用状态', key: 'used', width: 90, align: 'center' },
  { title: '状态', key: 'status', width: 90, align: 'center' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 135 },
  { title: '操作', key: 'action', width: 150, align: 'center' },
]

const modalOpen = ref(false)
const modalTitle = ref('新增订阅文章')
const form = ref({
  userId: undefined,
  trackId: undefined,
  title: '',
  description: '',
  fileUrl: '',
  fileName: '',
  status: '已上架',
})
const editingId = ref(null)

const userTrackMap = ref(new Map())

const currentPage = ref(1)
const pageSize = ref(10)

const filteredTracksForUser = computed(() => {
  if (!form.value.userId) return []
  const trackIds = userTrackMap.value.get(form.value.userId) || []
  return tracks.value.filter(t => trackIds.includes(t.id))
})

const filteredData = computed(() => rawData.value)

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

watch(() => form.value.userId, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== oldVal) {
    form.value.trackId = undefined
  }
})

async function loadData() {
  try {
    const params = {}
    const kw = search.value.trim()
    if (kw) params.keyword = kw
    if (userFilter.value) params.userId = userFilter.value
    if (trackFilter.value) params.trackId = trackFilter.value
    if (statusFilter.value) params.status = statusFilter.value
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const [list, uList, tList] = await Promise.all([
      listSubscriptionPosts(params),
      listUsers(),
      listTracks(),
    ])
    users.value = uList || []
    tracks.value = tList || []
    const userMap = new Map((uList || []).map(u => [u.id, u.username || u.name || u.id]))
    const trackMap = new Map((tList || []).map(t => [t.id, t.name]))
    rawData.value = list.map(d => ({
      ...d,
      userName: userMap.get(d.userId) || d.userId || '-',
      trackName: trackMap.get(d.trackId) || d.trackId || '-',
      createdAt: d.createdAt ? d.createdAt.slice(0, 16).replace('T', ' ') : '-',
    }))
    currentPage.value = 1

    // Build user track map from user data
    userTrackMap.value = new Map()
    for (const u of uList || []) {
      if (u.trackIds && Array.isArray(u.trackIds)) {
        userTrackMap.value.set(u.id, u.trackIds)
      }
    }
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handleReset() {
  search.value = ''
  userFilter.value = undefined
  trackFilter.value = undefined
  statusFilter.value = undefined
  startDate.value = null
  endDate.value = null
  currentPage.value = 1
  loadData()
}

function handleAdd() {
  modalTitle.value = '新增订阅文章'
  editingId.value = null
  form.value = { userId: undefined, trackId: undefined, title: '', description: '', fileUrl: '', fileName: '', status: '已上架' }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑订阅文章'
  editingId.value = record.id
  form.value = {
    userId: record.userId,
    trackId: record.trackId,
    title: record.title,
    description: record.description || '',
    fileUrl: record.fileUrl || '',
    fileName: record.fileName || '',
    status: record.status || '已上架',
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.userId || !form.value.trackId || !form.value.title) {
    message.warning('请填写必填项')
    return
  }
  try {
    await saveSubscriptionPost({
      id: editingId.value || undefined,
      userId: form.value.userId,
      trackId: form.value.trackId,
      title: form.value.title,
      description: form.value.description || undefined,
      fileUrl: form.value.fileUrl || undefined,
      fileName: form.value.fileName || undefined,
      status: form.value.status,
    })
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function isToday(dateStr) {
  if (!dateStr || dateStr === '-') return false
  const d = new Date(dateStr)
  if (isNaN(d)) return false
  const now = new Date()
  return d.getFullYear() === now.getFullYear() &&
         d.getMonth() === now.getMonth() &&
         d.getDate() === now.getDate()
}

function handleCancelRecommend(record) {
  Modal.confirm({
    title: '确认取消推荐该文章？',
    content: '取消后该文章将从用户订阅列表中移除，不可恢复。',
    async onOk() {
      try {
        await deleteSubscriptionPost(record.id)
        message.success('已取消推荐')
        loadData()
      } catch (e) {
        message.error('取消推荐失败')
      }
    },
  })
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除该订阅文章？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteSubscriptionPost(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
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

async function handlePreviewFile(record) {
  if (!record.fileUrl) {
    message.warning('暂无文件')
    return
  }
  previewRecord.value = record
  previewContent.value = ''

  const type = previewFileType.value
  if (type === 'text') {
    previewModalOpen.value = true
    previewLoading.value = true
    try {
      const res = await fetch(record.fileUrl)
      const text = await res.text()
      previewContent.value = text
    } catch (e) {
      previewContent.value = '读取文件失败'
    } finally {
      previewLoading.value = false
    }
    return
  }
  // pdf / docx / doc / other
  previewModalOpen.value = true
  if (type === 'docx') {
    previewLoading.value = true
    setTimeout(async () => {
      try {
        const res = await fetch(record.fileUrl)
        const blob = await res.blob()
        if (blob.size === 0) {
          throw new Error('文件内容为空')
        }
        previewLoading.value = false
        await nextTick()
        if (!docxContainerRef.value) return
        docxContainerRef.value.innerHTML = ''
        await nextTick()
        try {
          await renderAsync(blob, docxContainerRef.value, null, {
            className: 'docx-preview',
            inWrapper: false,
          })
        } catch (renderErr) {
          console.error('docx render error:', renderErr)
          if (docxContainerRef.value) {
            docxContainerRef.value.innerHTML = '<div style="color:#999;text-align:center;padding:40px;">文件解析失败，该文件可能不是有效的 docx 格式</div>'
          }
        }
      } catch (e) {
        previewLoading.value = false
        await nextTick()
        if (docxContainerRef.value) {
          docxContainerRef.value.innerHTML = '<div style="color:#999;text-align:center;padding:40px;">文件读取失败：' + (e.message || '未知错误') + '</div>'
        }
      }
    }, 400)
  }
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 20px; align-items: center; flex-wrap: wrap;">
      <Input v-model:value="search" placeholder="搜索标题 / 描述 / 文件名" style="width: 240px;" @pressEnter="handleSearch" />
      <Select v-model:value="userFilter" placeholder="全部用户" style="min-width: 160px;" allow-clear>
        <Select.Option v-for="u in users" :key="u.id" :value="u.id">{{ u.username || u.name || u.id }}</Select.Option>
      </Select>
      <Select v-model:value="trackFilter" placeholder="全部赛道" style="min-width: 160px;" allow-clear>
        <Select.Option v-for="t in tracks" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
      </Select>
      <Select v-model:value="statusFilter" placeholder="全部状态" style="min-width: 120px;" allow-clear>
        <Select.Option value="已上架">已上架</Select.Option>
        <Select.Option value="已下架">已下架</Select.Option>
      </Select>
      <DatePicker v-model:value="startDate" placeholder="开始日期" style="width: 140px;" />
      <DatePicker v-model:value="endDate" placeholder="结束日期" style="width: 140px;" />
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <div style="margin-left: auto; color: #999; font-size: 12px;">共 {{ filteredData.length }} 条</div>
    </div>

    <Table :columns="columns" :data-source="paginatedData" :pagination="false" row-key="id" size="small">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userName'">
          <span>{{ record.userName }}</span>
        </template>
        <template v-if="column.key === 'trackName'">
          <span>{{ record.trackName }}</span>
        </template>
        <template v-if="column.key === 'description'">
          <span :title="record.description">{{ record.description || '-' }}</span>
        </template>
        <template v-if="column.key === 'file'">
          <div v-if="record.fileName || record.fileUrl">
            <a @click="handlePreviewFile(record)">{{ record.fileName || '查看文件' }}</a>
          </div>
          <span v-else style="color: #999;">-</span>
        </template>
        <template v-if="column.key === 'used'">
          <Tag :color="record.used === 1 ? 'green' : 'orange'">{{ record.used === 1 ? '已使用' : '未使用' }}</Tag>
        </template>
        <template v-if="column.key === 'status'">
          <Tag :color="record.status === '已上架' ? 'green' : 'default'">{{ record.status }}</Tag>
        </template>
        <template v-if="column.key === 'action'" >
          <a @click="handlePreviewFile(record)">查看</a>
          <a v-if="isToday(record.createdAt)" style="margin-left: 12px; color: #f5222d;" @click="handleCancelRecommend(record)">取消推荐</a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="filteredData.length"
        show-total
        :page-size-options="['10', '20', '50']"
        show-size-changer
      />
    </div>
  </Card>

  <!-- Preview Modal -->
  <Modal
    v-model:open="previewModalOpen"
    :title="previewRecord.fileName || '文件预览'"
    :footer="null"
    :width="900"
    :mask-closable="false"
  >
    <div style="margin-top: 12px; min-height: 400px; max-height: 600px; overflow: auto;">
      <div v-if="previewLoading" style="text-align: center; padding: 80px; color: #999;">
        <div>加载中...</div>
      </div>
      <!-- PDF -->
      <iframe
        v-else-if="previewFileType === 'pdf'"
        :src="previewRecord.fileUrl"
        style="width: 100%; height: 550px; border: 0;"
      />
      <!-- Text -->
      <pre v-else-if="previewFileType === 'text'" style="padding: 16px; margin: 0; font-family: monospace; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 4px;"
      >{{ previewContent }}</pre>
      <!-- DOCX -->
      <div
        v-else-if="previewFileType === 'docx'"
        ref="docxContainerRef"
        style="padding: 24px; background: #fff; border: 1px solid #f0f0f0; border-radius: 4px; line-height: 1.8; font-size: 14px;"
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

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="560">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="目标用户" required>
        <Select v-model:value="form.userId" placeholder="请选择用户">
          <Select.Option v-for="u in users" :key="u.id" :value="u.id">{{ u.username || u.name || u.id }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="所属赛道" required>
        <Select v-model:value="form.trackId" placeholder="请先选择用户，再选择赛道" :disabled="!form.userId">
          <Select.Option v-for="t in filteredTracksForUser" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="文章标题" required>
        <Input v-model:value="form.title" placeholder="请输入文章标题" />
      </Form.Item>
      <Form.Item label="文章描述">
        <Input.TextArea v-model:value="form.description" :rows="3" placeholder="请输入文章描述" />
      </Form.Item>
      <Form.Item label="文件链接">
        <Input v-model:value="form.fileUrl" placeholder="请输入文件URL或上传后的链接" />
      </Form.Item>
      <Form.Item label="文件名">
        <Input v-model:value="form.fileName" placeholder="请输入文件名（用于展示）" />
      </Form.Item>
      <Form.Item label="状态">
        <Select v-model:value="form.status">
          <Select.Option value="已上架">已上架</Select.Option>
          <Select.Option value="已下架">已下架</Select.Option>
        </Select>
      </Form.Item>
    </Form>
  </Modal>
</template>

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

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message } from 'ant-design-vue'
import { listReferencePosts, saveReferencePost, deleteReferencePost } from '../api/referencePost.js'
import { listTracks } from '../api/track.js'

const search = ref('')
const platformFilter = ref(undefined)
const trackFilter = ref(undefined)
const statusFilter = ref(undefined)

const rawData = ref([])
const tracks = ref([])

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

const columns = [
  { title: '文章标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '所属赛道', key: 'trackName', width: 140 },
  { title: '平台', key: 'platform', width: 120 },
  { title: '类型', key: 'type', width: 100 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 150 },
  { title: '操作', key: 'action', width: 220 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增参考文章')
const form = ref({
  trackId: undefined,
  platform: undefined,
  title: '',
  contentType: '原文内容',
  content: '',
  url: '',
  sortOrder: 0,
  status: '已上架',
})
const editingId = ref(null)

const filteredTracksForModal = computed(() => {
  if (!form.value.platform) return []
  return tracks.value.filter(t => {
    const ps = (t.platforms || '').split(/[·、,，\s]+/).filter(Boolean)
    return ps.includes(form.value.platform)
  })
})

watch(() => form.value.platform, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== oldVal) {
    const validTrackIds = new Set(filteredTracksForModal.value.map(t => t.id))
    if (form.value.trackId && !validTrackIds.has(form.value.trackId)) {
      form.value.trackId = undefined
    }
  }
})

async function loadData() {
  try {
    const [list, tList] = await Promise.all([listReferencePosts(), listTracks()])
    tracks.value = tList || []
    const trackMap = new Map((tList || []).map(t => [t.id, t.name]))
    rawData.value = list.map(d => ({
      ...d,
      trackName: trackMap.get(d.trackId) || d.trackId || '-',
      sortOrder: d.sortOrder || 0,
      updateTime: d.updatedAt ? d.updatedAt.slice(0, 16).replace('T', ' ') : '-',
    }))
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败')
  }
}

const filteredData = computed(() => {
  let list = rawData.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(d => (d.title || '').includes(keyword))
  }
  if (platformFilter.value) {
    list = list.filter(d => d.platform === platformFilter.value)
  }
  if (trackFilter.value) {
    list = list.filter(d => d.trackId === trackFilter.value)
  }
  if (statusFilter.value) {
    list = list.filter(d => d.status === statusFilter.value)
  }
  return list
})

function handleSearch() {
  // computed already reactive
}

function handleReset() {
  search.value = ''
  platformFilter.value = undefined
  trackFilter.value = undefined
  statusFilter.value = undefined
}

function handleAdd() {
  modalTitle.value = '新增参考文章'
  editingId.value = null
  form.value = { trackId: undefined, platform: undefined, title: '', contentType: '原文内容', content: '', url: '', sortOrder: 0, status: '已上架' }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑参考文章'
  editingId.value = record.id
  const isLink = record.url && record.url.trim()
  form.value = {
    trackId: record.trackId,
    platform: record.platform,
    title: record.title,
    contentType: isLink ? '外部链接' : '原文内容',
    content: record.content || '',
    url: record.url || '',
    sortOrder: record.sortOrder || 0,
    status: record.status || '已上架',
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.trackId || !form.value.platform || !form.value.title) {
    message.warning('请填写必填项')
    return
  }
  if (form.value.contentType === '外部链接' && !form.value.url) {
    message.warning('请填写外部链接')
    return
  }
  try {
    await saveReferencePost({
      id: editingId.value || undefined,
      trackId: form.value.trackId,
      platform: form.value.platform,
      title: form.value.title,
      content: form.value.contentType === '原文内容' ? form.value.content : undefined,
      url: form.value.contentType === '外部链接' ? form.value.url : undefined,
      sortOrder: parseInt(form.value.sortOrder, 10) || 0,
      status: form.value.status,
    })
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

async function toggleStatus(record) {
  const newStatus = record.status === '已上架' ? '已下架' : '已上架'
  try {
    await saveReferencePost({
      id: record.id,
      trackId: record.trackId,
      platform: record.platform,
      title: record.title,
      content: record.content,
      url: record.url,
      sortOrder: record.sortOrder,
      status: newStatus,
    })
    message.success(newStatus === '已上架' ? '已上架' : '已下架')
    loadData()
  } catch (e) {
    message.error('操作失败')
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除该参考文章？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteReferencePost(record.id)
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
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center; flex-wrap: wrap;">
      <Input v-model:value="search" placeholder="搜索文章标题" style="width: 240px;" />
      <Select v-model:value="platformFilter" placeholder="全部平台" style="min-width: 140px;" allow-clear>
        <Select.Option v-for="opt in platformOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
      </Select>
      <Select v-model:value="trackFilter" placeholder="全部赛道" style="min-width: 160px;" allow-clear>
        <Select.Option v-for="t in tracks" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
      </Select>
      <Select v-model:value="statusFilter" placeholder="全部状态" style="min-width: 140px;" allow-clear>
        <Select.Option value="已上架">已上架</Select.Option>
        <Select.Option value="已下架">已下架</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button type="primary" style="margin-left: auto;" @click="handleAdd">+ 新增参考文章</Button>
    </div>

    <Table :columns="columns" :data-source="filteredData" :pagination="false" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'trackName'">
          <span>{{ record.trackName }}</span>
        </template>
        <template v-if="column.key === 'platform'">
          <Tag>{{ record.platform }}</Tag>
        </template>
        <template v-if="column.key === 'type'">
          <Tag :color="record.url ? 'blue' : 'green'">{{ record.url ? '外部链接' : '原文内容' }}</Tag>
        </template>
        <template v-if="column.key === 'status'">
          <Tag :color="record.status === '已上架' ? 'green' : 'default'">{{ record.status }}</Tag>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a style="margin-right: 12px;" :style="{ color: record.status === '已上架' ? '#f5222d' : '#1890ff' }" @click="toggleStatus(record)">
            {{ record.status === '已上架' ? '下架' : '上架' }}
          </a>
          <a style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="640">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="平台" required>
          <Select v-model:value="form.platform" placeholder="请选择平台">
            <Select.Option v-for="opt in platformOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="所属赛道" required>
          <Select v-model:value="form.trackId" placeholder="请先选择平台，再选择赛道" :disabled="!form.platform">
            <Select.Option v-for="t in filteredTracksForModal" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
          </Select>
        </Form.Item>
      </div>
      <Form.Item label="文章标题" required>
        <Input v-model:value="form.title" placeholder="请输入文章标题" />
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="内容类型" required>
          <Select v-model:value="form.contentType">
            <Select.Option value="原文内容">原文内容</Select.Option>
            <Select.Option value="外部链接">外部链接</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="排序权重">
          <Input type="number" v-model:value="form.sortOrder" placeholder="数字越小越靠前" />
        </Form.Item>
      </div>
      <Form.Item label="状态">
        <Select v-model:value="form.status">
          <Select.Option value="已上架">已上架</Select.Option>
          <Select.Option value="已下架">已下架</Select.Option>
        </Select>
      </Form.Item>

      <template v-if="form.contentType === '外部链接'">
        <Form.Item label="外部链接" required>
          <Input v-model:value="form.url" placeholder="请输入原文链接，如：https://mp.weixin.qq.com/..." />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">用户端将在新窗口打开此链接</div>
        </Form.Item>
      </template>

      <template v-if="form.contentType === '原文内容'">
        <Form.Item label="原文内容" required>
          <Input.TextArea v-model:value="form.content" :rows="8" placeholder="请输入参考文章原文，支持 HTML" />
        </Form.Item>
      </template>
    </Form>
  </Modal>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message } from 'ant-design-vue'
import { listDailyRecommends, saveDailyRecommend, deleteDailyRecommend } from '../api/dailyRecommend.js'
import { searchPosts } from '../api/post.js'
import { listTracks } from '../api/track.js'
import { listHelps } from '../api/help.js'

const search = ref('')
const platformFilter = ref(undefined)
const trackFilter = ref(undefined)
const statusFilter = ref(undefined)

const rawData = ref([])
const tracks = ref([])
const postOptions = ref([])
const postSearchKeyword = ref('')

// Help docs sidebar
const helpDocs = ref([])
const helpPreviewOpen = ref(false)
const helpPreviewRecord = ref(null)

async function loadHelpDocs() {
  try {
    const list = await listHelps()
    helpDocs.value = (list || []).filter(h => h.category === '使用问题' && h.status === '已上架')
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  } catch (e) {
    console.error('loadHelpDocs error:', e)
  }
}

function openHelpPreview(record) {
  helpPreviewRecord.value = record
  helpPreviewOpen.value = true
}

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

const columns = [
  { title: '推荐主题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '所属赛道', key: 'trackName', width: 140 },
  { title: '平台', key: 'platform', width: 120 },
  { title: '参考文章', key: 'ref', width: 120 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 150 },
  { title: '操作', key: 'action', width: 220 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增每日推荐')
const form = ref({
  trackId: undefined,
  platform: undefined,
  title: '',
  summary: '',
  refPostId: undefined,
  refUrl: '',
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

async function loadPostOptions(keyword = '') {
  if (!form.value.platform || !form.value.trackId) {
    postOptions.value = []
    return
  }
  try {
    const list = await searchPosts({
      platform: form.value.platform,
      trackId: form.value.trackId,
      keyword: keyword,
    })
    postOptions.value = list || []
  } catch (e) {
    postOptions.value = []
  }
}

function handlePostSearch(val) {
  postSearchKeyword.value = val
  loadPostOptions(val)
}

function handlePostDropdownOpen(open) {
  if (open) {
    loadPostOptions(postSearchKeyword.value)
  }
}

watch(() => form.value.platform, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== oldVal) {
    const validTrackIds = new Set(filteredTracksForModal.value.map(t => t.id))
    if (form.value.trackId && !validTrackIds.has(form.value.trackId)) {
      form.value.trackId = undefined
    }
    form.value.refPostId = undefined
    form.value.refUrl = ''
    postOptions.value = []
    postSearchKeyword.value = ''
  }
})

watch(() => form.value.trackId, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== oldVal) {
    form.value.refPostId = undefined
    form.value.refUrl = ''
    postOptions.value = []
    postSearchKeyword.value = ''
  }
})

watch(() => form.value.refPostId, (newVal) => {
  if (!newVal) return
  const post = postOptions.value.find(r => r.id === newVal)
  if (post && post.url) {
    form.value.refUrl = post.url
  }
})

async function loadData() {
  try {
    const [list, tList] = await Promise.all([listDailyRecommends(), listTracks()])
    tracks.value = tList || []
    const trackMap = new Map((tList || []).map(t => [t.id, t.name]))
    rawData.value = list.map(d => ({
      ...d,
      trackName: trackMap.get(d.trackId) || d.trackId || '-',
      sortOrder: d.sortOrder || 0,
      updateTime: d.updatedAt ? d.updatedAt.slice(0, 16).replace('T', ' ') : '-',
    }))
    await loadHelpDocs()
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败')
  }
}

const filteredData = computed(() => {
  let list = rawData.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(d => (d.title || '').includes(keyword) || (d.summary || '').includes(keyword))
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
  modalTitle.value = '新增每日推荐'
  editingId.value = null
  form.value = { trackId: undefined, platform: undefined, title: '', summary: '', refPostId: undefined, refUrl: '', sortOrder: 0, status: '已上架' }
  postOptions.value = []
  postSearchKeyword.value = ''
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑每日推荐'
  editingId.value = record.id
  form.value = {
    trackId: record.trackId,
    platform: record.platform,
    title: record.title,
    summary: record.summary || '',
    refPostId: record.refPostId || undefined,
    refUrl: record.refUrl || '',
    sortOrder: record.sortOrder || 0,
    status: record.status || '已上架',
  }
  postOptions.value = []
  postSearchKeyword.value = ''
  modalOpen.value = true
  loadPostOptions()
}

async function handleSave() {
  if (!form.value.trackId || !form.value.platform || !form.value.title) {
    message.warning('请填写必填项')
    return
  }
  try {
    await saveDailyRecommend({
      id: editingId.value || undefined,
      trackId: form.value.trackId,
      platform: form.value.platform,
      title: form.value.title,
      summary: form.value.summary || undefined,
      refPostId: form.value.refPostId || undefined,
      refUrl: form.value.refUrl || undefined,
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
    await saveDailyRecommend({
      id: record.id,
      trackId: record.trackId,
      platform: record.platform,
      title: record.title,
      summary: record.summary,
      refPostId: record.refPostId,
      refUrl: record.refUrl,
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
    title: '确认删除该每日推荐？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteDailyRecommend(record.id)
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
  <div style="display: flex; gap: 16px;">
    <!-- Main Content -->
    <div style="flex: 1; min-width: 0;">
      <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
        <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center; flex-wrap: wrap;">
          <Input v-model:value="search" placeholder="搜索推荐主题" style="width: 240px;" />
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
          <Button type="primary" style="margin-left: auto;" @click="handleAdd">+ 新增推荐</Button>
        </div>

        <Table :columns="columns" :data-source="filteredData" :pagination="false" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'trackName'">
              <span>{{ record.trackName }}</span>
            </template>
            <template v-if="column.key === 'platform'">
              <Tag>{{ record.platform }}</Tag>
            </template>
            <template v-if="column.key === 'ref'">
              <Tag v-if="record.refUrl" color="blue">链接</Tag>
              <Tag v-else-if="record.refPostId" color="green">关联</Tag>
              <span v-else>-</span>
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
    </div>

    <!-- Right Sidebar: Help Docs -->
    <Card :body-style="{ padding: '16px' }" style="width: 280px; flex-shrink: 0; border-radius: 2px;">
      <div style="font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px;">
        帮助文档 - 使用问题
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
    </Card>
  </div>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="560">
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
      <Form.Item label="推荐主题" required>
        <Input v-model:value="form.title" placeholder="请输入推荐主题" />
      </Form.Item>
      <Form.Item label="推荐摘要">
        <Input.TextArea v-model:value="form.summary" :rows="3" placeholder="请输入推荐摘要，用于用户端展示" />
      </Form.Item>
      <Form.Item label="关联文章">
        <Select
          v-model:value="form.refPostId"
          placeholder="请选择关联的文章"
          allow-clear
          show-search
          :filter-option="false"
          :disabled="!form.platform || !form.trackId"
          @search="handlePostSearch"
          @dropdownVisibleChange="handlePostDropdownOpen"
        >
          <Select.Option v-for="p in postOptions" :key="p.id" :value="p.id">{{ p.title }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="参考链接">
        <Input v-model:value="form.refUrl" placeholder="可直接填写参考文章链接" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">选择关联文章后若有链接会自动填充，也可手动填写外部链接</div>
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="排序权重">
          <Input type="number" v-model:value="form.sortOrder" placeholder="数字越小越靠前" />
        </Form.Item>
        <Form.Item label="状态">
          <Select v-model:value="form.status">
            <Select.Option value="已上架">已上架</Select.Option>
            <Select.Option value="已下架">已下架</Select.Option>
          </Select>
        </Form.Item>
      </div>
    </Form>
  </Modal>

  <!-- Help Doc Preview Modal -->
  <Modal
    v-model:open="helpPreviewOpen"
    :title="helpPreviewRecord?.title || '帮助文档'"
    :footer="null"
    :width="720"
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

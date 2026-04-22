<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'

function parseReads(val) {
  if (!val) return 0
  const str = String(val).replace(/,/g, '').trim()
  if (str.endsWith('w') || str.endsWith('W')) {
    const num = parseFloat(str.slice(0, -1))
    return isNaN(num) ? 0 : num * 10000
  }
  if (str.endsWith('k') || str.endsWith('K')) {
    const num = parseFloat(str.slice(0, -1))
    return isNaN(num) ? 0 : num * 1000
  }
  const num = parseFloat(str)
  return isNaN(num) ? 0 : num
}

import { Card, Input, Select, Button, Table, Tag, Modal, Form, message, Pagination } from 'ant-design-vue'
import { listPosts, savePost, deletePost } from '../api/post.js'
import { listBloggers } from '../api/blogger.js'
import { listTracks } from '../api/track.js'

const route = useRoute()

const searchTitle = ref('')
const trackFilter = ref(undefined)
const bloggerFilter = ref(undefined)
const page = ref(1)
const pageSize = ref(10)

const rawList = ref([])
const bloggers = ref([])
const bloggerMap = ref(new Map())
const tracks = ref([])
const trackMap = ref(new Map())

const trackColorMap = {
  '情感故事': 'blue',
  '科技数码': 'green',
  '健康养生': 'green',
  '职场成长': 'blue',
}

const columns = [
  { title: '文章标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '博主', dataIndex: 'blogger', key: 'blogger' },
  { title: '赛道', key: 'track', width: 120 },
  { title: '平台', dataIndex: 'platform', key: 'platform', width: 100 },
  { title: '数据指标', dataIndex: 'metrics', key: 'metrics', width: 180 },
  { title: '预估收益', key: 'revenue', width: 100 },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 120 },
  { title: '操作', key: 'action', width: 150 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增文章')
const form = ref({
  title: '', blogger: undefined, track: undefined, platform: undefined, publishTime: '',
  readCount: '', likeCount: '', shareCount: '', recommendCount: '',
  showCount: '', commentCount: '', collectCount: '',
  url: ''
})
const editingId = ref(null)
const selectedRowKeys = ref([])

const rowSelection = {
  onChange: (keys) => {
    selectedRowKeys.value = keys
  },
}

async function loadData() {
  try {
    const [postList, bloggerList, trackList] = await Promise.all([listPosts(), listBloggers(), listTracks()])
    bloggers.value = bloggerList
    bloggerMap.value = new Map(bloggerList.map(b => [b.id, b]))
    tracks.value = trackList
    trackMap.value = new Map(trackList.map(t => [t.id, t.name]))
    rawList.value = postList.map(p => {
      const blogger = bloggerMap.value.get(p.bloggerId) || {}
      const trackName = trackMap.value.get(blogger.trackId) || '-'
      const readsNum = parseReads(p.reads)
      const revenue = readsNum > 0 ? ((readsNum * 5) / 1000).toFixed(2) : '0.00'
      return {
        ...p,
        blogger: blogger.name || p.bloggerId || '-',
        track: trackName,
        metrics: `阅读 ${p.reads || 0} · 点赞 ${p.likes || 0}${p.comments ? ' · 评论 ' + p.comments : ''}`,
        revenue,
        publishTime: p.createdAt ? p.createdAt.slice(0, 10) : '-',
        content: p.content || '<p>暂无内容</p>',
      }
    })

    const qBloggerId = route.query.bloggerId
    if (qBloggerId) {
      const bloggerName = bloggerMap.value.get(qBloggerId)?.name
      if (bloggerName) bloggerFilter.value = bloggerName
    }
  } catch (e) {
    message.error('加载失败')
  }
}

const filteredList = computed(() => {
  let list = rawList.value
  const keyword = searchTitle.value.trim()
  if (keyword) {
    list = list.filter(p => (p.title || '').includes(keyword))
  }
  if (trackFilter.value) {
    list = list.filter(p => p.track === trackFilter.value)
  }
  if (bloggerFilter.value) {
    list = list.filter(p => p.blogger === bloggerFilter.value)
  }
  return list
})

const tableData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

function handleSearch() {
  page.value = 1
}

function handleReset() {
  searchTitle.value = ''
  trackFilter.value = undefined
  bloggerFilter.value = undefined
  page.value = 1
}

function handlePageChange(p) {
  page.value = p
}

function handleAdd() {
  modalTitle.value = '新增文章'
  editingId.value = null
  form.value = {
    title: '', blogger: undefined, track: undefined, platform: undefined, publishTime: '',
    readCount: '', likeCount: '', shareCount: '', recommendCount: '',
    showCount: '', commentCount: '', collectCount: '',
    url: ''
  }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑文章'
  editingId.value = record.id
  const bloggerId = record.bloggerId || bloggers.value.find(b => b.name === record.blogger)?.id
  let metrics = {}
  try {
    if (record.metricsJson) metrics = JSON.parse(record.metricsJson)
  } catch (e) {}
  form.value = {
    title: record.title, blogger: bloggerId, track: undefined, platform: record.platform, publishTime: record.publishTime,
    readCount: String(record.reads || ''), likeCount: String(record.likes || ''),
    shareCount: metrics.shareCount || '', recommendCount: metrics.recommendCount || '',
    showCount: metrics.showCount || '', commentCount: String(record.comments || ''), collectCount: metrics.collectCount || '',
    url: record.url || ''
  }
  modalOpen.value = true
}

const revenue = computed(() => {
  if (form.value.platform !== '公众号') return null
  const reads = parseReads(form.value.readCount)
  if (reads <= 0) return 0
  return ((reads * 5) / 1000).toFixed(2)
})

async function handleSave() {
  if (!form.value.title || !form.value.blogger || !form.value.platform || !form.value.url) {
    message.warning('请填写必填项')
    return
  }
  try {
    const metricsObj = {}
    if (form.value.shareCount) metricsObj.shareCount = form.value.shareCount
    if (form.value.recommendCount) metricsObj.recommendCount = form.value.recommendCount
    if (form.value.showCount) metricsObj.showCount = form.value.showCount
    if (form.value.collectCount) metricsObj.collectCount = form.value.collectCount
    await savePost({
      id: editingId.value || undefined,
      title: form.value.title,
      bloggerId: form.value.blogger,
      platform: form.value.platform,
      url: form.value.url,
      reads: parseInt(form.value.readCount || '0', 10) || 0,
      likes: parseInt(form.value.likeCount || '0', 10) || 0,
      comments: parseInt(form.value.commentCount || '0', 10) || 0,
      metricsJson: Object.keys(metricsObj).length ? JSON.stringify(metricsObj) : undefined,
    })
    message.success(modalTitle.value + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除该文章？',
    content: '删除后数据将不在列表中显示，但可在数据库中保留。',
    async onOk() {
      try {
        await deletePost(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

function handleBatchDelete() {
  const ids = selectedRowKeys.value
  if (!ids.length) return
  Modal.confirm({
    title: `确认批量删除 ${ids.length} 篇文章？`,
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await Promise.all(ids.map(id => deletePost(id)))
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch (e) {
        message.error('批量删除失败')
      }
    },
  })
}

function openPreview(record) {
  if (record.url) {
    window.open(record.url, '_blank')
  } else {
    message.warning('暂无原文链接')
  }
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="searchTitle" placeholder="搜索文章标题" style="width: 240px;" />
      <Select v-model:value="trackFilter" placeholder="全部赛道" style="min-width: 140px;" allow-clear>
        <Select.Option v-for="t in tracks" :key="t.id" :value="t.name">{{ t.name }}</Select.Option>
      </Select>
      <Select v-model:value="bloggerFilter" placeholder="全部博主" style="min-width: 140px;" allow-clear>
        <Select.Option v-for="b in bloggers" :key="b.id" :value="b.name">{{ b.name }}</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button v-if="selectedRowKeys.length" danger style="margin-left: auto;" @click="handleBatchDelete">批量删除 ({{ selectedRowKeys.length }})</Button>
      <Button type="primary" :style="selectedRowKeys.length ? { marginLeft: '12px' } : { marginLeft: 'auto' }" @click="handleAdd">+ 新增文章</Button>
    </div>

    <Table :columns="columns" :data-source="tableData" :pagination="false" row-key="id" :row-selection="rowSelection">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'track'">
          <Tag :color="trackColorMap[record.track] || 'default'">{{ record.track }}</Tag>
        </template>
        <template v-if="column.key === 'metrics'">
          <div style="font-size: 12px; line-height: 1.6; color: #595959; white-space: pre-line;">{{ record.metrics }}</div>
        </template>
        <template v-if="column.key === 'revenue'">
          <span style="color: #f59e0b; font-weight: 600;">¥ {{ record.revenue }}</span>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a style="margin-right: 12px;" @click="openPreview(record)">查看</a>
          <a style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination :current="page" :total="filteredList.length" :page-size="pageSize" @change="handlePageChange" />
    </div>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="800">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="文章标题" required>
        <Input v-model:value="form.title" placeholder="请输入文章标题" />
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="所属博主" required>
          <Select v-model:value="form.blogger" placeholder="请选择博主">
            <Select.Option v-for="b in bloggers" :key="b.id" :value="b.id">{{ b.name }}</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="所属平台" required>
          <Select v-model:value="form.platform" placeholder="请选择平台">
            <Select.Option value="公众号">公众号</Select.Option>
            <Select.Option value="今日头条">今日头条</Select.Option>
            <Select.Option value="百家号">百家号</Select.Option>
          </Select>
        </Form.Item>
      </div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="发布时间">
          <Input type="date" v-model:value="form.publishTime" />
        </Form.Item>
      </div>
      <p style="font-size: 13px; color: #8c8c8c; margin: 4px 0 12px;">💡 切换平台后，下方仅展示对应平台的指标字段</p>

      <div style="font-size: 13px; font-weight: 500; color: #262626; margin-bottom: 8px;">通用指标</div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="阅读量">
          <Input v-model:value="form.readCount" placeholder="如：12.5w" />
        </Form.Item>
        <Form.Item label="点赞量">
          <Input v-model:value="form.likeCount" placeholder="如：1.2w" />
        </Form.Item>
      </div>

      <template v-if="form.platform === '公众号'">
        <div style="font-size: 13px; font-weight: 500; color: #262626; margin: 12px 0 8px;">公众号指标</div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
          <Form.Item label="分享量">
            <Input v-model:value="form.shareCount" placeholder="如：3.4k" />
          </Form.Item>
          <Form.Item label="推荐量">
            <Input v-model:value="form.recommendCount" placeholder="如：8.9k" />
          </Form.Item>
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
          <Form.Item label="评论量">
            <Input v-model:value="form.commentCount" placeholder="如：1.8k" />
          </Form.Item>
          <Form.Item label="预估收益">
            <div style="font-size: 16px; font-weight: 600; color: #f59e0b;">{{ revenue !== null ? '¥ ' + revenue : '—' }}</div>
            <div style="font-size: 12px; color: #999;">按 1000 阅读 = 5 元 自动计算</div>
          </Form.Item>
        </div>
      </template>

      <template v-if="form.platform === '今日头条'">
        <div style="font-size: 13px; font-weight: 500; color: #262626; margin: 12px 0 8px;">今日头条指标</div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
          <Form.Item label="展现量">
            <Input v-model:value="form.showCount" placeholder="如：45w" />
          </Form.Item>
          <Form.Item label="评论量">
            <Input v-model:value="form.commentCount" placeholder="如：1.8k" />
          </Form.Item>
        </div>
      </template>

      <template v-if="form.platform === '百家号'">
        <div style="font-size: 13px; font-weight: 500; color: #262626; margin: 12px 0 8px;">百家号指标</div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
          <Form.Item label="收藏量">
            <Input v-model:value="form.collectCount" placeholder="如：1.5k" />
          </Form.Item>
          <Form.Item label="评论量">
            <Input v-model:value="form.commentCount" placeholder="如：456" />
          </Form.Item>
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
          <Form.Item label="分享量">
            <Input v-model:value="form.shareCount" placeholder="如：890" />
          </Form.Item>
        </div>
      </template>

      <Form.Item label="原文链接" required>
        <Input v-model:value="form.url" placeholder="请输入微信公众号或今日头条原文链接" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">用户端点击文章标题将直接跳转至该链接查看原文</div>
      </Form.Item>
    </Form>
  </Modal>

</template>

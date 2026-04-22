<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, Avatar, message, Pagination } from 'ant-design-vue'
import { listBloggers, saveBlogger, deleteBlogger, importBloggers } from '../api/blogger.js'
import { listTracks } from '../api/track.js'

const route = useRoute()
const router = useRouter()

const searchName = ref('')
const trackFilter = ref(undefined)
const platformFilter = ref(undefined)
const page = ref(1)
const pageSize = ref(10)

const rawList = ref([])
const tracks = ref([])
const trackMap = ref(new Map())

const trackColorMap = {
  '情感故事': 'blue',
  '科技数码': 'green',
  '职场成长': 'blue',
  '健康养生': 'green',
  '家庭教育': 'blue',
}

const columns = [
  { title: '头像', key: 'avatar', width: 80 },
  { title: '博主名称', dataIndex: 'name', key: 'name' },
  { title: '简介', dataIndex: 'intro', key: 'intro', ellipsis: true },
  { title: '所属赛道', key: 'track', width: 120 },
  { title: '平台', dataIndex: 'platform', key: 'platform', width: 100 },
  { title: '文章数', dataIndex: 'articleCount', key: 'articleCount', width: 90 },
  { title: '主页链接', key: 'link', width: 180 },
  { title: '添加时间', dataIndex: 'addTime', key: 'addTime', width: 120 },
  { title: '操作', key: 'action', width: 180 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增博主')
const form = ref({ name: '', intro: '', track: undefined, platform: undefined, link: '' })
const avatarImage = ref('')
const editingId = ref(null)
const selectedRowKeys = ref([])

// Import
const importModalOpen = ref(false)
const importExcelFile = ref(null)
const importZipFile = ref(null)
const importLoading = ref(false)

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

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
    if (form.value.track && !validTrackIds.has(form.value.track)) {
      form.value.track = undefined
    }
  }
})

const rowSelection = {
  onChange: (keys) => {
    selectedRowKeys.value = keys
  },
}

async function loadData() {
  try {
    const [bloggerList, trackList] = await Promise.all([listBloggers(), listTracks()])
    tracks.value = trackList
    trackMap.value = new Map(trackList.map(t => [t.id, t.name]))
    rawList.value = bloggerList.map(b => ({
      ...b,
      avatarText: b.name ? b.name.slice(0, 1) : '?',
      intro: b.tagline || '-',
      track: trackMap.value.get(b.trackId) || b.trackId || '-',
      articleCount: b.articleCount || 0,
      addTime: b.createdAt ? b.createdAt.slice(0, 10) : '-',
    }))

    // handle trackId query param from track manage
    const qTrackId = route.query.trackId
    if (qTrackId) {
      const trackName = trackMap.value.get(qTrackId)
      if (trackName) trackFilter.value = trackName
    }
  } catch (e) {
    message.error('加载失败')
  }
}

const filteredList = computed(() => {
  let list = rawList.value
  const keyword = searchName.value.trim()
  if (keyword) {
    list = list.filter(b => (b.name || '').includes(keyword))
  }
  if (trackFilter.value) {
    list = list.filter(b => b.track === trackFilter.value)
  }
  if (platformFilter.value) {
    list = list.filter(b => (b.platform || '').includes(platformFilter.value))
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
  searchName.value = ''
  trackFilter.value = undefined
  platformFilter.value = undefined
  page.value = 1
}

function handlePageChange(p) {
  page.value = p
}

function handleAdd() {
  modalTitle.value = '新增博主'
  editingId.value = null
  form.value = { name: '', intro: '', track: undefined, platform: undefined, link: '' }
  avatarImage.value = ''
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑博主'
  editingId.value = record.id
  const trackId = tracks.value.find(t => t.name === record.track)?.id
  form.value = { name: record.name, intro: record.intro, track: trackId, platform: record.platform, link: record.link }
  avatarImage.value = record.avatar || ''
  modalOpen.value = true
}

function handleAvatarUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    avatarImage.value = reader.result
  }
  reader.readAsDataURL(file)
}

async function handleSave() {
  if (!form.value.name || !form.value.track || !form.value.platform) {
    message.warning('请填写必填项')
    return
  }
  try {
    await saveBlogger({
      id: editingId.value || undefined,
      name: form.value.name,
      tagline: form.value.intro,
      trackId: form.value.track,
      platform: form.value.platform,
      link: form.value.link,
      avatar: avatarImage.value || undefined,
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
    title: '确认删除该博主？',
    content: '删除后数据将不在列表中显示，但可在数据库中保留。',
    async onOk() {
      try {
        await deleteBlogger(record.id)
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
    title: `确认批量删除 ${ids.length} 条博主？`,
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await Promise.all(ids.map(id => deleteBlogger(id)))
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch (e) {
        message.error('批量删除失败')
      }
    },
  })
}

function goToPosts(record) {
  router.push('/posts?bloggerId=' + record.id)
}

function downloadTemplate() {
  const headers = ['name', 'tagline', 'platform', 'track', 'link', 'avatarFileName']
  const example1 = ['罗大伦频道', '传播中医健康知识', '公众号', '健康养生', 'https://mp.weixin.qq.com/xxx', 'https://example.com/avatar1.png']
  const example2 = ['医路向前巍子', '每天科普疾病和急救知识', '公众号', '健康养生', '', 'weizi.png']
  const csv = [headers.join(','), example1.join(','), example2.join(',')].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '博主导入模板.csv'
  a.click()
  URL.revokeObjectURL(url)
}

function openImportModal() {
  importModalOpen.value = true
  importExcelFile.value = null
  importZipFile.value = null
}

async function handleImport() {
  if (!importExcelFile.value) {
    message.warning('请选择 Excel 文件')
    return
  }
  importLoading.value = true
  try {
    const result = await importBloggers(importExcelFile.value, importZipFile.value)
    message.success(`导入完成：成功 ${result.success} 条，跳过 ${result.skip} 条`)
    if (result.errors && result.errors.length) {
      console.warn('导入错误：', result.errors)
    }
    importModalOpen.value = false
    loadData()
  } catch (e) {
    message.error('导入失败：' + (e.response?.data?.msg || e.message))
  } finally {
    importLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="searchName" placeholder="搜索博主名称" style="width: 240px;" />
      <Select v-model:value="trackFilter" placeholder="全部赛道" style="min-width: 140px;" allow-clear>
        <Select.Option v-for="t in tracks" :key="t.id" :value="t.name">{{ t.name }}</Select.Option>
      </Select>
      <Select v-model:value="platformFilter" placeholder="全部平台" style="min-width: 140px;" allow-clear>
        <Select.Option value="公众号">公众号</Select.Option>
        <Select.Option value="今日头条">今日头条</Select.Option>
        <Select.Option value="百家号">百家号</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button v-if="selectedRowKeys.length" danger style="margin-left: auto;" @click="handleBatchDelete">批量删除 ({{ selectedRowKeys.length }})</Button>
      <Button :style="selectedRowKeys.length ? { marginLeft: '12px' } : { marginLeft: 'auto' }" @click="openImportModal">📥 批量导入</Button>
      <Button type="primary" style="margin-left: 12px;" @click="handleAdd">+ 新增博主</Button>
    </div>

    <Table :columns="columns" :data-source="tableData" :pagination="false" row-key="id" :row-selection="rowSelection">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'avatar'">
          <Avatar v-if="record.avatar" :src="record.avatar" :size="40" style="border-radius: 50%;" />
          <Avatar v-else :size="40" style="background: #f0f0f0; color: #999;">{{ record.avatarText }}</Avatar>
        </template>
        <template v-if="column.key === 'track'">
          <Tag :color="trackColorMap[record.track] || 'default'">{{ record.track }}</Tag>
        </template>
        <template v-if="column.key === 'link'">
          <span v-if="(record.platform || '').includes('公众号')" style="color: #999;">-</span>
          <a v-else :href="record.link" target="_blank" style="color: #1890ff;">查看主页 ↗</a>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a style="margin-right: 12px;" @click="goToPosts(record)">查看文章</a>
          <a style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination :current="page" :total="filteredList.length" :page-size="pageSize" @change="handlePageChange" />
    </div>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="博主头像">
        <div style="display: flex; gap: 12px; align-items: flex-start;">
          <label style="width: 100px; height: 100px; border: 1px dashed #d9d9d9; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #8c8c8c; font-size: 13px; overflow: hidden; position: relative;">
            <input type="file" accept="image/*" style="position: absolute; inset: 0; opacity: 0; cursor: pointer;" @change="handleAvatarUpload">
            <img v-if="avatarImage" :src="avatarImage" style="width: 100%; height: 100%; object-fit: cover;">
            <template v-else>
              <div style="font-size: 24px; margin-bottom: 4px;">+</div>
              <div>上传头像</div>
            </template>
          </label>
        </div>
        <div style="font-size: 12px; color: #999; margin-top: 4px;">建议尺寸 200×200px，圆形展示</div>
      </Form.Item>
      <Form.Item label="博主名称" required>
        <Input v-model:value="form.name" placeholder="请输入博主名称" />
      </Form.Item>
      <Form.Item label="博主简介">
        <Input.TextArea v-model:value="form.intro" placeholder="请输入博主简介，帮助用户快速了解该博主定位" :rows="3" />
      </Form.Item>
      <Form.Item label="所属平台" required>
        <Select v-model:value="form.platform" placeholder="请选择平台">
          <Select.Option v-for="opt in platformOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="所属赛道" required>
        <Select v-model:value="form.track" placeholder="请先选择平台，再选择赛道" :disabled="!form.platform">
          <Select.Option v-for="t in filteredTracksForModal" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="主页链接">
        <Input v-model:value="form.link" placeholder="请输入博主主页或公众号链接" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">用于前端跳转查看原文博主主页</div>
      </Form.Item>
    </Form>
  </Modal>

  <!-- Import Modal -->
  <Modal v-model:open="importModalOpen" title="批量导入博主" :mask-closable="false" :confirm-loading="importLoading" @ok="handleImport">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
        <div style="font-size: 13px; color: #389e0d; margin-bottom: 8px;">
          <strong>导入说明</strong>
        </div>
        <div style="font-size: 12px; color: #595959; line-height: 1.8;">
          <div>1. 下载模板，按格式填写博主信息</div>
          <div>2. Excel 列顺序：name | tagline | platform | track | link | avatarFileName</div>
          <div>3. 头像支持两种方式：① 填图片 URL（如 https://xxx.com/avatar.png）② 填文件名，同时上传头像 ZIP 包</div>
          <div>4. 平台支持：公众号 / 今日头条 / 百家号</div>
          <div>5. track 填赛道名称，需与系统中赛道名称完全一致</div>
        </div>
      </div>

      <Form.Item>
        <Button size="small" @click="downloadTemplate">下载导入模板</Button>
      </Form.Item>

      <Form.Item label="Excel 文件" required>
        <input type="file" accept=".xlsx,.xls,.csv" @change="e => importExcelFile = e.target.files[0]">
        <div style="font-size: 12px; color: #999; margin-top: 4px;">支持 .xlsx / .xls / .csv</div>
      </Form.Item>

      <Form.Item label="头像 ZIP 包">
        <input type="file" accept=".zip" @change="e => importZipFile = e.target.files[0]">
        <div style="font-size: 12px; color: #999; margin-top: 4px;">将所有头像图片打包成 ZIP 上传，文件名需与 Excel 中 avatarFileName 一致</div>
      </Form.Item>
    </Form>
  </Modal>
</template>

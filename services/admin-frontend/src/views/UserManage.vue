<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message, Pagination, Checkbox, Upload } from 'ant-design-vue'
import { listUsers, getUserTracks, removeUserTrack } from '../api/user.js'
import { listSubscriptionPosts, saveSubscriptionPost } from '../api/subscriptionPost.js'
import { listTracks } from '../api/track.js'
import { listCreations } from '../api/creation.js'
import { listStyles } from '../api/style.js'
import { uploadFile } from '../api/upload.js'
import request from '../api/request.js'

const search = ref('')
const statusFilter = ref(undefined)

const data = ref([])
const allSubscriptionPosts = ref([])
const allTracks = ref([])
const allStyles = ref([])

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '联系方式', dataIndex: 'contact', key: 'contact', width: 150 },
  { title: 'AI 日限额', dataIndex: 'aiUsageText', key: 'aiUsageText', width: 95 },
  { title: '可选赛道', dataIndex: 'trackLimitText', key: 'trackLimitText', width: 90 },
  { title: '可访问平台', dataIndex: 'platformLimitText', key: 'platformLimitText', width: 120 },
  { title: '状态', key: 'status', width: 75 },
  { title: '注册时间', dataIndex: 'registerTime', key: 'registerTime', width: 105 },
  { title: '到期时间', dataIndex: 'expireDate', key: 'expireDate', width: 105 },
  { title: '最近登录', dataIndex: 'lastLogin', key: 'lastLogin', width: 145 },
  { title: '操作', key: 'action', width: 320 },
]

const addModalOpen = ref(false)
const editModalOpen = ref(false)
const creationModalOpen = ref(false)
const currentUser = ref({})
const creationRecords = ref([])
const creationColumns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '赛道', dataIndex: 'trackName', key: 'trackName', width: 100 },
  { title: '模式', dataIndex: 'mode', key: 'mode', width: 80 },
  { title: '状态', key: 'reviewed', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 130 },
]
const platformOptions = ['公众号', '今日头条', '百家号']

const addForm = ref({ username: '', contactType: '手机号', contact: '', password: 'Abc123456', aiLimit: 50, trackLimit: 0, platformLimit: [], expireDate: '2026-12-31', remark: '' })
const editForm = ref({ id: null, aiLimit: 50, trackLimit: 0, platformLimit: [], expireDate: '2026-12-31', status: 1, remark: '' })

const currentPage = ref(1)
const pageSize = ref(10)

const filteredData = computed(() => {
  let list = data.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(u => (u.username || '').includes(keyword) || (u.contact || '').includes(keyword))
  }
  if (statusFilter.value) {
    const target = statusFilter.value === '正常' ? 1 : 0
    list = list.filter(u => u.status === target)
  }
  return list
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

// Recommend modal state
const recommendModalOpen = ref(false)
const recommendUser = ref({})
const recommendUserTracks = ref([])
const recommendUserPosts = ref([])
const selectedRecommendTrackId = ref(null)
const recommendForm = ref({ title: '', description: '', fileUrl: '', fileName: '' })
const recommendUploading = ref(false)
const previewMode = ref(false)
const previewContent = ref('')
const previewLoading = ref(false)

const selectedRecommendTrack = computed(() => {
  return recommendUserTracks.value.find(t => t.id === selectedRecommendTrackId.value) || null
})

const userSelectedStyle = computed(() => {
  const name = recommendUser.value?.template
  if (!name) return null
  return allStyles.value.find(s => s.name === name) || null
})

const trackHasPost = computed(() => {
  if (!selectedRecommendTrackId.value) return false
  return recommendUserPosts.value.some(p => p.trackId === selectedRecommendTrackId.value)
})

const previewFileType = computed(() => {
  const name = recommendForm.value.fileName || ''
  if (name.endsWith('.pdf')) return 'pdf'
  if (name.endsWith('.txt') || name.endsWith('.md')) return 'text'
  if (name.endsWith('.doc') || name.endsWith('.docx')) return 'word'
  return 'other'
})

function needsRecommend(user) {
  const userTracks = user.trackIds || []
  if (userTracks.length === 0) return false
  const userPosts = allSubscriptionPosts.value.filter(p => p.userId === user.id)
  const postedTrackIds = new Set(userPosts.map(p => p.trackId))
  return userTracks.some(tid => !postedTrackIds.has(tid))
}

async function openRecommendModal(record) {
  recommendUser.value = record
  recommendModalOpen.value = true
  selectedRecommendTrackId.value = null
  recommendForm.value = { title: '', description: '', fileUrl: '', fileName: '' }
  recommendUserPosts.value = []

  try {
    const [tracks, posts, styles] = await Promise.all([
      getUserTracks(record.id),
      listSubscriptionPosts(),
      listStyles(),
    ])
    allStyles.value = styles || []
    const trackIds = (tracks || []).map(t => t.trackId)
    recommendUserTracks.value = allTracks.value.filter(t => trackIds.includes(t.id))
    recommendUserPosts.value = (posts || []).filter(p => p.userId === record.id)

    // Auto-select first track that needs a post
    const postedTrackIds = new Set(recommendUserPosts.value.map(p => p.trackId))
    const firstMissing = recommendUserTracks.value.find(t => !postedTrackIds.has(t.id))
    if (firstMissing) {
      selectedRecommendTrackId.value = firstMissing.id
    } else if (recommendUserTracks.value.length > 0) {
      selectedRecommendTrackId.value = recommendUserTracks.value[0].id
    }
  } catch (e) {
    message.error('加载用户赛道失败')
  }
}

function selectRecommendTrack(trackId) {
  selectedRecommendTrackId.value = trackId
  recommendForm.value = { title: '', description: '', fileUrl: '', fileName: '' }
  previewMode.value = false
  previewContent.value = ''
}

async function handleRecommendUpload({ file }) {
  recommendUploading.value = true
  try {
    const url = await uploadFile(file)
    recommendForm.value.fileUrl = url
    recommendForm.value.fileName = file.name
    if (!recommendForm.value.title) {
      const nameWithoutExt = file.name.replace(/\.[^/.]+$/, '')
      recommendForm.value.title = nameWithoutExt
    }
    message.success('上传成功')
  } catch (e) {
    message.error('上传失败')
  } finally {
    recommendUploading.value = false
  }
}

async function handleRecommendPreview() {
  if (!recommendForm.value.fileUrl) return
  const type = previewFileType.value
  if (type === 'pdf') {
    previewMode.value = true
    previewContent.value = ''
    return
  }
  if (type === 'text') {
    previewLoading.value = true
    previewMode.value = true
    try {
      const res = await fetch(recommendForm.value.fileUrl)
      const text = await res.text()
      previewContent.value = text
    } catch (e) {
      previewContent.value = '读取文件失败'
    } finally {
      previewLoading.value = false
    }
    return
  }
  previewMode.value = true
  previewContent.value = ''
}

function backToEdit() {
  previewMode.value = false
  previewContent.value = ''
}

async function saveRecommend() {
  if (!selectedRecommendTrackId.value) {
    message.warning('请选择赛道')
    return
  }
  if (!recommendForm.value.title) {
    message.warning('请输入文章标题')
    return
  }
  try {
    await saveSubscriptionPost({
      userId: recommendUser.value.id,
      trackId: selectedRecommendTrackId.value,
      title: recommendForm.value.title,
      description: recommendForm.value.description || undefined,
      fileUrl: recommendForm.value.fileUrl || undefined,
      fileName: recommendForm.value.fileName || undefined,
      status: '已上架',
    })
    message.success('保存成功')
    // Refresh posts for this user
    const posts = await listSubscriptionPosts()
    recommendUserPosts.value = (posts || []).filter(p => p.userId === recommendUser.value.id)
    allSubscriptionPosts.value = posts || []
    recommendForm.value = { title: '', description: '', fileUrl: '', fileName: '' }
  } catch (e) {
    message.error('保存失败')
  }
}

async function loadData() {
  try {
    const [uList, cList, sList, tList] = await Promise.all([listUsers(), listCreations(), listSubscriptionPosts(), listTracks()])
    allSubscriptionPosts.value = sList || []
    allTracks.value = tList || []
    const usageMap = {}
    ;(cList || []).forEach(c => {
      usageMap[c.userId] = (usageMap[c.userId] || 0) + 1
    })
    data.value = uList.map(u => ({
      ...u,
      contact: u.phone || u.email || u.wxId || '-',
      statusText: u.status === 1 ? '正常' : '已禁用',
      registerTime: u.createdAt ? u.createdAt.slice(0, 10) : '-',
      expireDate: u.expireDate || '-',
      lastLogin: u.lastLogin ? u.lastLogin.slice(0, 16).replace('T', ' ') : '-',
      aiUsageText: `${usageMap[u.id] || 0}/${u.aiLimit || 0}`,
      trackLimitText: `${u.trackLimit || 0}`,
      platformLimitText: (u.platformLimit || '').split(/[,，]/).filter(Boolean).join('、') || '全部平台',
      trackIds: u.trackIds || [],
    }))
  } catch (e) {
    message.error('加载失败')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handleReset() {
  search.value = ''
  statusFilter.value = undefined
  currentPage.value = 1
  loadData()
}

function handleAdd() {
  addModalOpen.value = true
  addForm.value = { username: '', contactType: '手机号', contact: '', password: 'Abc123456', aiLimit: 50, trackLimit: 0, platformLimit: [...platformOptions], expireDate: '2026-12-31', remark: '' }
}

function handleEdit(record) {
  editModalOpen.value = true
  const rawPlatforms = record.platformLimit || ''
  editForm.value = { id: record.id, aiLimit: record.aiLimit || 50, trackLimit: record.trackLimit || 0, platformLimit: rawPlatforms ? rawPlatforms.split(/[,，]/).map(s => s.trim()).filter(Boolean) : [...platformOptions], expireDate: record.expireDate || '2026-12-31', status: record.status === 1 ? 1 : 0, remark: record.remark || '' }
}

async function saveAdd() {
  if (!addForm.value.username || !addForm.value.contact) {
    message.warning('请填写必填项')
    return
  }
  try {
    const payload = {
      username: addForm.value.username,
      password: addForm.value.password,
      status: 1,
      aiLimit: parseInt(addForm.value.aiLimit, 10) || 0,
      trackLimit: parseInt(addForm.value.trackLimit, 10) || 0,
      platformLimit: (addForm.value.platformLimit || []).join(','),
      expireDate: addForm.value.expireDate,
      remark: addForm.value.remark,
    }
    if (addForm.value.contactType === '手机号') payload.phone = addForm.value.contact
    else if (addForm.value.contactType === '邮箱') payload.email = addForm.value.contact
    else if (addForm.value.contactType === '微信号') payload.wxId = addForm.value.contact
    await request.post('/users', payload)
    message.success('开户成功')
    addModalOpen.value = false
    loadData()
  } catch (e) {
    message.error('开户失败')
  }
}

async function saveEdit() {
  try {
    await request.put('/users/' + editForm.value.id, {
      aiLimit: parseInt(editForm.value.aiLimit, 10) || 0,
      trackLimit: parseInt(editForm.value.trackLimit, 10) || 0,
      platformLimit: (editForm.value.platformLimit || []).join(','),
      expireDate: editForm.value.expireDate,
      status: editForm.value.status,
      remark: editForm.value.remark,
    })
    message.success('保存修改成功')
    editModalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

async function toggleStatus(record) {
  try {
    const newStatus = record.status === 1 ? 0 : 1
    await request.put('/users/' + record.id, { status: newStatus })
    message.success(newStatus === 1 ? '已启用' : '已禁用')
    loadData()
  } catch (e) {
    message.error('操作失败')
  }
}

async function resetPassword(record) {
  try {
    await request.put('/users/' + record.id, { password: 'Abc123456' })
    message.success('密码已重置为 Abc123456')
  } catch (e) {
    message.error('重置失败')
  }
}

async function viewCreations(record) {
  currentUser.value = record
  try {
    const list = await listCreations({ userId: record.id })
    creationRecords.value = (list || []).map(c => ({
      ...c,
      reviewedText: c.reviewed === 1 ? '已审阅' : '未审阅',
      createdAt: c.createdAt ? c.createdAt.slice(0, 16).replace('T', ' ') : '-',
    }))
    creationModalOpen.value = true
  } catch (e) {
    message.error('加载创作记录失败')
  }
}

// Track info modal state
const trackInfoModalOpen = ref(false)
const trackInfoUser = ref({})
const trackInfoList = ref([])
const trackInfoLoading = ref(false)

async function openTrackInfoModal(record) {
  trackInfoUser.value = record
  trackInfoModalOpen.value = true
  trackInfoLoading.value = true
  try {
    const userTracks = await getUserTracks(record.id)
    const trackIds = (userTracks || []).map(ut => ut.trackId)
    trackInfoList.value = allTracks.value.filter(t => trackIds.includes(t.id)).map(t => ({
      ...t,
      platformDisplay: (t.platforms || '').split(/[·、,，\s]+/).filter(Boolean).join('、') || '-',
    }))
  } catch (e) {
    message.error('加载用户赛道失败')
    trackInfoList.value = []
  } finally {
    trackInfoLoading.value = false
  }
}

function handleCancelUserTrack(track) {
  Modal.confirm({
    title: '确认取消订阅该赛道？',
    content: `将取消用户「${trackInfoUser.value.username || ''}」对「${track.name}」赛道的订阅。`,
    async onOk() {
      try {
        await removeUserTrack(trackInfoUser.value.id, track.id)
        message.success('已取消订阅')
        trackInfoList.value = trackInfoList.value.filter(t => t.id !== track.id)
        // Also refresh main list to update trackIds
        loadData()
      } catch (e) {
        message.error('取消订阅失败')
      }
    },
  })
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="search" placeholder="搜索用户名/联系方式" style="width: 240px;" />
      <Select v-model:value="statusFilter" placeholder="全部状态" style="min-width: 140px;" allow-clear>
        <Select.Option value="正常">正常</Select.Option>
        <Select.Option value="已禁用">已禁用</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button type="primary" style="margin-left: auto;" @click="handleAdd">+ 新增用户</Button>
    </div>

    <Table :columns="columns" :data-source="paginatedData" :pagination="false" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <Tag :color="record.status === 1 ? 'green' : 'red'">{{ record.statusText }}</Tag>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a style="margin-right: 12px;" @click="resetPassword(record)">重置密码</a>
          <a style="margin-right: 12px;" @click="openTrackInfoModal(record)">赛道信息</a>
          <a v-if="needsRecommend(record)" style="margin-right: 12px; color: #fa8c16;" @click="openRecommendModal(record)">推荐</a>
          <a :style="{ color: record.status === 1 ? '#f5222d' : '#1890ff' }" @click="toggleStatus(record)">
            {{ record.status === 1 ? '禁用' : '启用' }}
          </a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination v-model:current="currentPage" v-model:page-size="pageSize" :total="filteredData.length" show-total />
    </div>
  </Card>

  <Modal v-model:open="addModalOpen" title="新增用户（管理员开户）" :mask-closable="false" :width="700" @ok="saveAdd">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="用户名" required>
        <Input v-model:value="addForm.username" placeholder="请输入用户名" />
      </Form.Item>
      <Form.Item label="联系方式" required>
        <div style="display: grid; grid-template-columns: 120px 1fr; gap: 12px; align-items: flex-end;">
          <Select v-model:value="addForm.contactType">
            <Select.Option value="手机号">手机号</Select.Option>
            <Select.Option value="微信号">微信号</Select.Option>
            <Select.Option value="邮箱">邮箱</Select.Option>
          </Select>
          <Input v-model:value="addForm.contact" placeholder="请输入联系方式" />
        </div>
      </Form.Item>
      <Form.Item label="初始密码" required>
        <Input v-model:value="addForm.password" readonly />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">系统将自动生成初始密码，用户首次登录后建议修改</div>
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px;">
        <Form.Item label="每日 AI 生成限额" required>
          <Input type="number" v-model:value="addForm.aiLimit" min="0" />
        </Form.Item>
        <Form.Item label="可选赛道数" required>
          <Input type="number" v-model:value="addForm.trackLimit" min="0" />
        </Form.Item>
        <Form.Item label="授权到期时间" required>
          <Input type="date" v-model:value="addForm.expireDate" />
        </Form.Item>
      </div>
      <div style="font-size: 12px; color: #999; margin-top: -8px; margin-bottom: 16px;">每日限额包含标题、大纲、全文等所有 AI 生成调用；0 表示不限额。可选赛道数 0 表示不限制</div>
      <Form.Item label="可访问平台" required>
        <Checkbox.Group v-model:value="addForm.platformLimit" :options="platformOptions" />
      </Form.Item>
      <Form.Item label="备注">
        <Input.TextArea v-model:value="addForm.remark" placeholder="可选填，如：客户来源、特殊说明等" :rows="3" />
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="editModalOpen" title="编辑用户" :mask-closable="false" :width="700" @ok="saveEdit">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="用户名">
        <Input :value="data.find(d => d.id === editForm.id)?.username" disabled />
      </Form.Item>
      <Form.Item label="联系方式">
        <Input :value="data.find(d => d.id === editForm.id)?.contact" disabled />
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px;">
        <Form.Item label="每日 AI 生成限额" required>
          <Input type="number" v-model:value="editForm.aiLimit" min="0" />
        </Form.Item>
        <Form.Item label="可选赛道数" required>
          <Input type="number" v-model:value="editForm.trackLimit" min="0" />
        </Form.Item>
        <Form.Item label="授权到期时间" required>
          <Input type="date" v-model:value="editForm.expireDate" />
        </Form.Item>
      </div>
      <div style="font-size: 12px; color: #999; margin-top: -8px; margin-bottom: 16px;">0 表示不限额或不限制赛道数</div>
      <Form.Item label="可访问平台" required>
        <Checkbox.Group v-model:value="editForm.platformLimit" :options="platformOptions" />
      </Form.Item>
      <Form.Item label="账号状态">
        <Select v-model:value="editForm.status">
          <Select.Option :value="1">正常</Select.Option>
          <Select.Option :value="0">已禁用</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="备注">
        <Input.TextArea v-model:value="editForm.remark" placeholder="可选填" :rows="3" />
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="creationModalOpen" :title="`${currentUser.username || ''} 的创作记录`" :footer="null" :width="1000">
    <Table :columns="creationColumns" :data-source="creationRecords" :pagination="false" row-key="id" size="small" style="margin-top: 12px;">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'reviewed'">
          <Tag :color="record.reviewed === 1 ? 'green' : 'orange'">{{ record.reviewedText }}</Tag>
        </template>
      </template>
    </Table>
    <div v-if="!creationRecords.length" style="text-align: center; color: #999; padding: 24px;">暂无创作记录</div>
  </Modal>

  <!-- Recommend Modal -->
  <Modal
    v-model:open="recommendModalOpen"
    :title="`为 ${recommendUser.username || ''} 推荐文章`"
    :mask-closable="false"
    :width="900"
    :footer="null"
  >
    <div style="display: flex; gap: 0; margin-top: 12px; min-height: 480px;">
      <!-- Left: Track List -->
      <div style="width: 200px; border-right: 1px solid #f0f0f0; padding-right: 16px; flex-shrink: 0;">
        <div style="font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #262626;">订阅赛道</div>
        <div
          v-for="t in recommendUserTracks"
          :key="t.id"
          @click="selectRecommendTrack(t.id)"
          :style="{
            padding: '10px 12px',
            borderRadius: '6px',
            cursor: 'pointer',
            marginBottom: '8px',
            fontSize: '13px',
            background: selectedRecommendTrackId === t.id ? '#e6f7ff' : 'transparent',
            border: selectedRecommendTrackId === t.id ? '1px solid #1890ff' : '1px solid transparent',
            color: selectedRecommendTrackId === t.id ? '#1890ff' : '#595959',
          }"
        >
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px;">
            <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ t.name }}</span>
            <Tag v-if="recommendUserPosts.some(p => p.trackId === t.id)" size="small" color="green" style="flex-shrink: 0; margin-left: 6px;">已推荐</Tag>
            <Tag v-else size="small" color="orange" style="flex-shrink: 0; margin-left: 6px;">待推荐</Tag>
          </div>
          <div style="font-size: 11px; color: selectedRecommendTrackId === t.id ? '#69c0ff' : '#bfbfbf';">{{ t.platforms || '-' }}</div>
        </div>
        <div v-if="!recommendUserTracks.length" style="color: #999; font-size: 13px; padding: 12px 0;">该用户未订阅任何赛道</div>
      </div>

      <!-- Right: Form / Preview -->
      <div style="flex: 1; padding-left: 24px;">
        <div v-if="selectedRecommendTrack" style="max-width: 560px;">
          <!-- Template Info -->
          <div style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 6px; padding: 12px 16px; margin-bottom: 20px;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 13px; color: #52c41a; flex-shrink: 0;">用户默认样式：</span>
              <template v-if="userSelectedStyle">
                <span style="font-size: 14px; font-weight: 600; color: #262626;">{{ userSelectedStyle.name }}</span>
                <Tag v-if="userSelectedStyle.scene" size="small" color="blue">{{ userSelectedStyle.scene.split(',')[0] }}</Tag>
              </template>
              <span v-else style="font-size: 14px; font-weight: 500; color: #8c8c8c;">{{ recommendUser.template || '未设置' }}</span>
            </div>
            <div v-if="userSelectedStyle?.desc" style="font-size: 12px; color: #8c8c8c; margin-top: 4px; padding-left: 86px;">
              {{ userSelectedStyle.desc }}
            </div>
          </div>

          <!-- Edit Mode -->
          <div v-if="!previewMode">
            <Form layout="vertical">
              <Form.Item label="文章标题" required>
                <Input v-model:value="recommendForm.title" placeholder="请输入文章标题" />
              </Form.Item>

              <Form.Item label="订阅文章文件">
                <div style="display: flex; gap: 12px; align-items: center;">
                  <Upload
                    :customRequest="handleRecommendUpload"
                    :showUploadList="false"
                    accept=".doc,.docx,.pdf,.txt,.md"
                  >
                    <Button :loading="recommendUploading">
                      {{ recommendUploading ? '上传中...' : '点击上传' }}
                    </Button>
                  </Upload>
                  <Button v-if="recommendForm.fileUrl" type="link" @click="handleRecommendPreview">
                    预览文件
                  </Button>
                </div>
                <div v-if="recommendForm.fileName" style="margin-top: 8px; font-size: 12px; color: #666;">
                  已上传：{{ recommendForm.fileName }}
                </div>
              </Form.Item>

              <Form.Item label="文章描述">
                <Input.TextArea
                  v-model:value="recommendForm.description"
                  :rows="5"
                  placeholder="请输入文章描述"
                />
              </Form.Item>

              <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px;">
                <Button @click="recommendModalOpen = false">取消</Button>
                <Button type="primary" @click="saveRecommend">保存</Button>
              </div>
            </Form>
          </div>

          <!-- Preview Mode -->
          <div v-else>
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
              <div style="font-size: 16px; font-weight: 600;">
                {{ recommendForm.fileName || '文件预览' }}
              </div>
              <Button size="small" @click="backToEdit">返回编辑</Button>
            </div>

            <div style="border: 1px solid #f0f0f0; border-radius: 6px; background: #fafafa; min-height: 360px; max-height: 480px; overflow: auto;">
              <!-- PDF -->
              <iframe
                v-if="previewFileType === 'pdf'"
                :src="recommendForm.fileUrl"
                style="width: 100%; height: 480px; border: 0;"
              />
              <!-- Text -->
              <pre v-else-if="previewFileType === 'text'" style="padding: 16px; margin: 0; font-family: monospace; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;">{{ previewLoading ? '加载中...' : previewContent }}</pre>
              <!-- Word / Other -->
              <div v-else style="padding: 48px 24px; text-align: center; color: #999;">
                <div style="font-size: 40px; margin-bottom: 16px;">📄</div>
                <div style="font-size: 14px; margin-bottom: 8px;">该文件格式暂不支持浏览器在线预览</div>
                <div style="font-size: 12px; color: #bbb; margin-bottom: 16px;">请下载后用相应软件打开查看</div>
                <Button type="primary" @click="() => { window.open(recommendForm.fileUrl, '_blank') }">下载文件</Button>
              </div>
            </div>
          </div>
        </div>
        <div v-else style="display: flex; align-items: center; justify-content: center; height: 100%; color: #999;">
          请选择左侧赛道
        </div>
      </div>
    </div>
  </Modal>

  <!-- Track Info Modal -->
  <Modal
    v-model:open="trackInfoModalOpen"
    :title="`${trackInfoUser.username || ''} 的赛道订阅`"
    :footer="null"
    :width="560"
  >
    <div style="margin-top: 12px;">
      <div v-if="trackInfoLoading" style="text-align: center; padding: 40px; color: #999;">加载中...</div>
      <div v-else-if="!trackInfoList.length" style="text-align: center; padding: 40px; color: #999;">该用户未订阅任何赛道</div>
      <div v-else>
        <div
          v-for="t in trackInfoList"
          :key="t.id"
          style="display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border: 1px solid #f0f0f0; border-radius: 6px; margin-bottom: 8px; background: #fafafa;"
        >
          <div>
            <div style="font-size: 14px; font-weight: 500; color: #262626;">{{ t.name }}</div>
            <div style="font-size: 12px; color: #8c8c8c; margin-top: 2px;">{{ t.platformDisplay }}</div>
          </div>
          <a style="color: #f5222d; font-size: 13px;" @click="handleCancelUserTrack(t)">取消订阅</a>
        </div>
      </div>
    </div>
  </Modal>
</template>

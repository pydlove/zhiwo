<script setup>
import { ref, onMounted, computed, h } from 'vue'
import dayjs from 'dayjs'
import {
  Card, Input, Select, Button, Table, Tag, Modal, Form,
  message, Pagination, Tabs, Badge, Empty, Row, Col,
  Divider, Descriptions, Drawer, Radio, Alert, Space,
  DatePicker
} from 'ant-design-vue'
import {
  listTitleReviews, approveTitleReview, rejectTitleReview,
  batchApproveTitleReviews, batchRejectTitleReviews,
  cancelTitleReview, batchCancelTitleReviews,
  pushTitleReview, batchPushTitleReviews,
  rePushTitleReview, batchRePushTitleReviews,
  getTitleReviewStats, listPushLogs, listPushedTitleReviews, listBySource,
  listServerConfigs, saveServerConfig, deleteServerConfig,
  testServerConfig, testServerConfigDirect
} from '../api/process.js'
import {
  getProcessAutoStatus, getProcessAutoConfig, saveProcessAutoConfig,
  getTodayLog, getRecentLogs, triggerCheck
} from '../api/processAuto.js'
import { listTracks } from '../api/track.js'
import { generateTitles, getGenerateStatus, cancelGenerate, saveTitle } from '../api/titleLibrary.js'

// 流程类型 Tabs
const processType = ref('title-review')

// 数据
const tableData = ref([])
const tracks = ref([])
const serverConfigs = ref([])
const pushLogData = ref([])
const stats = ref({ pending: 0, approved: 0, rejected: 0, pushed: 0 })
const loading = ref(false)

// 分页
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 从 localStorage 读取保存的搜索条件和 tab
const PROCESS_SEARCH_KEY = 'process_search'
const PROCESS_TAB_KEY = 'process_tab'
const savedSearch = JSON.parse(localStorage.getItem(PROCESS_SEARCH_KEY) || '{}')
const savedTab = localStorage.getItem(PROCESS_TAB_KEY) || 'pending'

// 筛选
const searchKeyword = ref('')
const searchPlatform = ref(savedSearch.platform || '')
const searchTrack = ref(savedSearch.trackId || '')

// 审核状态子 Tabs（从 localStorage 恢复）
const reviewStatus = ref(savedTab)

// 选择
const selectedRowKeys = ref([])
const selectedRows = ref([])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys, rows) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  },
}))

// 弹窗状态
const rejectModalOpen = ref(false)
const rejectForm = ref({ id: '', reason: '' })
const serverConfigDrawerOpen = ref(false)
const serverConfigForm = ref({
  id: '', name: '', host: '', port: 8080,
  isActive: 1, isDefault: 0
})
const serverConfigModalOpen = ref(false)
const saveLoading = ref(false)
const serverConfigTestLoading = ref(false)

// 编辑标题
const editModalOpen = ref(false)
const editForm = ref({ id: '', title: '', description: '' })
const editLoading = ref(false)

// 改赛道
const changeTrackModalOpen = ref(false)
const changeTrackForm = ref({ id: '', trackId: '' })
const changeTrackLoading = ref(false)

// 自动化配置
const autoConfig = ref({
  id: '1',
  checkTime: '03:00',
  checkPlatforms: '',
  checkAllTracks: 1,
  autoNotifyLocal: 1,
  titlesPerTrack: 3,
  autoPushAfterApprove: 1,
  autoMatchAfterPush: 1,
  isEnabled: 1,
})
const autoConfigLoading = ref(false)
const autoConfigSaving = ref(false)
const todayLog = ref(null)
const recentLogs = ref([])
const autoStatusLoading = ref(false)
const triggerCheckLoading = ref(false)
const targetDate = ref(dayjs().add(1, 'day'))

const statusLabelMap = {
  waiting: '等待执行',
  checking: '检查中',
  need_titles: '需要生成标题',
  generating: '生成中',
  reviewing: '待审核',
  pushing: '推送中',
  matching: '匹配中',
  need_articles: '需要生成文章',
  article_review: '文章待审核',
  scheduled: '已排期',
  completed: '已完成',
  failed: '失败',
}

const statusColorMap = {
  waiting: 'default',
  checking: 'processing',
  need_titles: 'warning',
  generating: 'processing',
  reviewing: 'warning',
  pushing: 'processing',
  matching: 'processing',
  need_articles: 'warning',
  article_review: 'warning',
  scheduled: 'success',
  completed: 'success',
  failed: 'error',
}

// 流程阶段列表（用于步骤条）
const flowStages = [
  { key: 'checking', label: '标题检查', env: '线上' },
  { key: 'need_titles', label: '生成标题', env: '本地' },
  { key: 'reviewing', label: '审核标题', env: '线上' },
  { key: 'pushing', label: '推送到线上', env: '线上' },
  { key: 'matching', label: '匹配用户', env: '线上' },
  { key: 'need_articles', label: '生成文章', env: '本地' },
  { key: 'article_review', label: '审核文章', env: '线上' },
  { key: 'scheduled', label: '推送排期', env: '线上' },
]

function getStageIndex(status) {
  if (!status) return -1
  return flowStages.findIndex(s => s.key === status)
}

// 平台选项
const platforms = ['公众号', '今日头条', '百家号']

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

// 生成标题
const GENERATE_CONFIG_KEY = 'process_generateConfig'
const savedGenerateConfig = JSON.parse(localStorage.getItem(GENERATE_CONFIG_KEY) || 'null')

const generateModalOpen = ref(false)
const generateCount = ref(savedGenerateConfig?.count || 3)
const generateOutputPath = ref(savedGenerateConfig?.outputPath || '')
const generateTrackIds = ref(savedGenerateConfig?.trackIds || [])
const generating = ref(false)
const generateProgress = ref(0)
const generateTaskId = ref(null)
const generateStatusMsg = ref('')
let generatePollTimer = null

// 生成弹窗中赛道选项：按平台排序，显示为"平台-赛道"
const sortedTrackOptions = computed(() => {
  const platformOrder = platformOptions.map(p => p.value)
  const list = [...tracks.value].filter(t => t.name)
  list.sort((a, b) => {
    const pa = a.platforms ? a.platforms.split(/[,，\s]+/).filter(Boolean)[0] : ''
    const pb = b.platforms ? b.platforms.split(/[,，\s]+/).filter(Boolean)[0] : ''
    const ia = platformOrder.indexOf(pa)
    const ib = platformOrder.indexOf(pb)
    if (ia !== ib) return (ia === -1 ? 999 : ia) - (ib === -1 ? 999 : ib)
    return a.name.localeCompare(b.name)
  })
  return list.map(t => {
    const firstPlatform = t.platforms ? t.platforms.split(/[,，\s]+/).filter(Boolean)[0] : ''
    return { ...t, displayLabel: firstPlatform ? `${firstPlatform} - ${t.name}` : t.name }
  })
})

const filteredTracksForSearch = computed(() => {
  if (!searchPlatform.value) {
    return tracks.value
  }
  return tracks.value.filter(t => {
    if (!t.platforms) return false
    const trackPlatforms = t.platforms.split(/[·、,，\s]+/).filter(Boolean)
    return trackPlatforms.includes(searchPlatform.value)
  })
})

function onSearchTrackChange() {
  saveSearchConfig()
  handleSearch()
}

function saveSearchConfig() {
  localStorage.setItem(PROCESS_SEARCH_KEY, JSON.stringify({
    platform: searchPlatform.value,
    trackId: searchTrack.value,
  }))
}

function onSearchPlatformChange() {
  const validTrackIds = new Set(filteredTracksForSearch.value.map(t => t.id))
  if (searchTrack.value && !validTrackIds.has(searchTrack.value)) {
    searchTrack.value = ''
  }
  saveSearchConfig()
  handleSearch()
}

function saveGenerateConfig() {
  localStorage.setItem(GENERATE_CONFIG_KEY, JSON.stringify({
    count: generateCount.value,
    outputPath: generateOutputPath.value,
    trackIds: generateTrackIds.value,
  }))
}

function openGenerateModal() {
  generateModalOpen.value = true
}

function derivePlatformsFromTracks(trackIds) {
  const platforms = new Set()
  for (const trackId of trackIds) {
    const track = tracks.value.find(t => t.id === trackId)
    if (track && track.platforms) {
      const ps = track.platforms.split(/[,，\s]+/).filter(Boolean)
      ps.forEach(p => platforms.add(p))
    }
  }
  return Array.from(platforms)
}

function stopGeneratePoll() {
  if (generatePollTimer) {
    clearInterval(generatePollTimer)
    generatePollTimer = null
  }
}

async function pollGenerateStatus(taskId) {
  try {
    const status = await getGenerateStatus(taskId)
    generateProgress.value = status.progress || 0
    generateStatusMsg.value = status.message || ''
    if (status.status === 'completed') {
      stopGeneratePoll()
      generating.value = false
      generateTaskId.value = null
      message.success(`生成完成：共 ${status.total} 条标题，已保存到 ${status.path}`)
      loadData()
      loadStats()
    } else if (status.status === 'failed') {
      stopGeneratePoll()
      generating.value = false
      generateTaskId.value = null
      message.error('生成失败: ' + (status.message || '未知错误'))
    }
  } catch (e) {
    console.error('poll error:', e)
  }
}

async function handleGenerate() {
  if (!generateCount.value || generateCount.value < 1) {
    message.warning('请输入有效的生成数量')
    return
  }
  generating.value = true
  generateProgress.value = 0
  generateStatusMsg.value = '提交中...'
  try {
    const result = await generateTitles({
      countPerCombo: generateCount.value,
      outputPath: generateOutputPath.value.trim(),
      platforms: derivePlatformsFromTracks(generateTrackIds.value),
      trackIds: generateTrackIds.value,
    })
    generateTaskId.value = result.taskId
    generateModalOpen.value = false
    saveGenerateConfig()
    generateStatusMsg.value = '任务已提交，开始生成...'
    generatePollTimer = setInterval(() => {
      if (generateTaskId.value) {
        pollGenerateStatus(generateTaskId.value)
      }
    }, 3000)
  } catch (e) {
    generating.value = false
    message.error('提交失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  }
}

async function handleCancelGenerate() {
  if (!generateTaskId.value) {
    generating.value = false
    stopGeneratePoll()
    return
  }
  try {
    await cancelGenerate(generateTaskId.value)
    stopGeneratePoll()
    generating.value = false
    generateTaskId.value = null
    generateProgress.value = 0
    generateStatusMsg.value = ''
    message.info('已取消生成')
  } catch (e) {
    stopGeneratePoll()
    generating.value = false
    generateTaskId.value = null
    message.info('已取消生成')
  }
}

async function loadData() {
  if (reviewStatus.value === 'push-logs') {
    await loadPushLogs()
    return
  }
  if (reviewStatus.value === 'pushed') {
    loading.value = true
    try {
      const res = await listPushedTitleReviews({
        platform: searchPlatform.value,
        trackId: searchTrack.value,
        keyword: searchKeyword.value,
        page: page.value,
        pageSize: pageSize.value
      })
      tableData.value = res.list || []
      total.value = res.total || 0
    } catch (e) {
      message.error('加载数据失败')
    } finally {
      loading.value = false
    }
    return
  }
  if (reviewStatus.value === 'pushed-up') {
    loading.value = true
    try {
      const res = await listBySource({
        source: 'pushed_up',
        platform: searchPlatform.value,
        trackId: searchTrack.value,
        keyword: searchKeyword.value,
        page: page.value,
        pageSize: pageSize.value
      })
      tableData.value = res.list || []
      total.value = res.total || 0
    } catch (e) {
      message.error('加载数据失败')
    } finally {
      loading.value = false
    }
    return
  }
  loading.value = true
  try {
    const res = await listTitleReviews({
      reviewStatus: reviewStatus.value,
      platform: searchPlatform.value,
      trackId: searchTrack.value,
      keyword: searchKeyword.value,
      page: page.value,
      pageSize: pageSize.value
    })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function loadPushLogs() {
  loading.value = true
  try {
    const res = await listPushLogs({
      page: page.value,
      pageSize: pageSize.value
    })
    pushLogData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    message.error('加载推送日志失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getTitleReviewStats()
    stats.value = res || { pending: 0, approved: 0, rejected: 0, pushed: 0 }
  } catch (e) {
    // ignore
  }
}

async function loadTracks() {
  try {
    const res = await listTracks()
    tracks.value = res || []
  } catch (e) {
    // ignore
  }
}

async function loadServerConfigs() {
  try {
    const res = await listServerConfigs()
    serverConfigs.value = res || []
  } catch (e) {
    // ignore
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  searchKeyword.value = ''
  searchPlatform.value = ''
  searchTrack.value = ''
  page.value = 1
  localStorage.removeItem(PROCESS_SEARCH_KEY)
  loadData()
}

function handlePageChange(p, ps) {
  page.value = p
  pageSize.value = ps
  loadData()
}

function handleTabChange(key) {
  reviewStatus.value = key
  page.value = 1
  selectedRowKeys.value = []
  selectedRows.value = []
  localStorage.setItem(PROCESS_TAB_KEY, key)
  loadData()
}

async function handleApprove(record) {
  try {
    await approveTitleReview(record.id)
    message.success('审核通过')
    loadData()
    loadStats()
  } catch (e) {
    message.error('操作失败：' + (e?.response?.data?.msg || e.message))
  }
}

async function handleBatchApprove() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要审核的标题')
    return
  }
  try {
    await batchApproveTitleReviews(selectedRowKeys.value)
    message.success('批量审核通过成功')
    selectedRowKeys.value = []
    selectedRows.value = []
    loadData()
    loadStats()
  } catch (e) {
    message.error('操作失败')
  }
}

function handleReject(record) {
  rejectForm.value = { id: record.id, reason: '' }
  rejectModalOpen.value = true
}

async function handleRejectConfirm() {
  try {
    await rejectTitleReview(rejectForm.value.id, { reason: rejectForm.value.reason })
    message.success('已拒绝')
    rejectModalOpen.value = false
    loadData()
    loadStats()
  } catch (e) {
    message.error('操作失败')
  }
}

function handleEdit(record) {
  editForm.value = {
    id: record.titleLibraryId,
    title: record.title || '',
    description: record.description || '',
    platform: record.platform || '',
    trackId: record.trackId || ''
  }
  editModalOpen.value = true
}

async function handleEditConfirm() {
  if (!editForm.value.title || editForm.value.title.trim() === '') {
    message.warning('标题不能为空')
    return
  }
  editLoading.value = true
  try {
    await saveTitle({
      id: editForm.value.id,
      title: editForm.value.title.trim(),
      description: editForm.value.description.trim(),
      platform: editForm.value.platform,
      trackId: editForm.value.trackId
    })
    message.success('保存成功')
    editModalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  } finally {
    editLoading.value = false
  }
}

function handleChangeTrack(record) {
  changeTrackForm.value = {
    id: record.titleLibraryId,
    trackId: record.trackId || ''
  }
  changeTrackModalOpen.value = true
}

async function handleChangeTrackConfirm() {
  if (!changeTrackForm.value.trackId) {
    message.warning('请选择赛道')
    return
  }
  changeTrackLoading.value = true
  try {
    await saveTitle({
      id: changeTrackForm.value.id,
      trackId: changeTrackForm.value.trackId
    })
    message.success('赛道修改成功')
    changeTrackModalOpen.value = false
    loadData()
  } catch (e) {
    message.error('赛道修改失败')
  } finally {
    changeTrackLoading.value = false
  }
}

async function handleBatchReject() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要拒绝的标题')
    return
  }
  rejectForm.value = { ids: selectedRowKeys.value, reason: '' }
  rejectModalOpen.value = true
}

async function handleBatchRejectConfirm() {
  try {
    await batchRejectTitleReviews(rejectForm.value.ids, rejectForm.value.reason)
    message.success('批量拒绝成功')
    rejectModalOpen.value = false
    selectedRowKeys.value = []
    selectedRows.value = []
    loadData()
    loadStats()
  } catch (e) {
    message.error('操作失败')
  }
}

async function handleCancel(record) {
  try {
    await cancelTitleReview(record.id)
    message.success('已取消审核')
    loadData()
    loadStats()
  } catch (e) {
    message.error('操作失败：' + (e?.response?.data?.msg || e.message))
  }
}

async function handleBatchCancel() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要取消审核的标题')
    return
  }
  try {
    await batchCancelTitleReviews(selectedRowKeys.value)
    message.success('批量取消审核成功')
    selectedRowKeys.value = []
    selectedRows.value = []
    loadData()
    loadStats()
  } catch (e) {
    message.error('操作失败')
  }
}

function handlePush(record) {
  doPush([record.id])
}

function handleBatchPush() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要推送的标题')
    return
  }
  doPush(selectedRowKeys.value)
}

async function doPush(ids) {
  try {
    const res = await batchPushTitleReviews({ ids })
    const result = res
    if (result.failed > 0) {
      message.warning(`推送完成：成功 ${result.success} 条，失败 ${result.failed} 条`)
    } else {
      message.success(`成功推送 ${result.success} 条标题`)
    }
    selectedRowKeys.value = []
    selectedRows.value = []
    loadData()
    loadStats()
  } catch (e) {
    message.error('推送失败：' + (e?.response?.data?.msg || e.message))
  }
}

function handleRePush(record) {
  doRePush([record.id])
}

function handleBatchRePush() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要重新推送的标题')
    return
  }
  doRePush(selectedRowKeys.value)
}

async function doRePush(ids) {
  try {
    const res = await batchRePushTitleReviews({ ids })
    const result = res
    if (result.failed > 0) {
      message.warning(`重新推送完成：成功 ${result.success} 条，失败 ${result.failed} 条`)
    } else {
      message.success(`成功重新推送 ${result.success} 条标题`)
    }
    selectedRowKeys.value = []
    selectedRows.value = []
    loadData()
    loadStats()
  } catch (e) {
    message.error('重新推送失败：' + (e?.response?.data?.msg || e.message))
  }
}

// 服务器配置管理
function openServerConfigDrawer() {
  serverConfigDrawerOpen.value = true
  loadServerConfigs()
}

function openServerConfigModal(config) {
  if (config) {
    serverConfigForm.value = { ...config }
  } else {
    serverConfigForm.value = {
      id: '', name: '', host: '', port: 8080,
      isActive: 1, isDefault: 0
    }
  }
  serverConfigModalOpen.value = true
}

async function handleSaveServerConfig() {
  saveLoading.value = true
  try {
    await saveServerConfig(serverConfigForm.value)
    message.success('保存成功')
    serverConfigModalOpen.value = false
    await loadServerConfigs()
  } catch (e) {
    message.error('保存失败')
  } finally {
    saveLoading.value = false
  }
}

async function handleDeleteServerConfig(id) {
  Modal.confirm({
    title: '确认删除',
    content: '确定删除该服务器配置吗？',
    async onOk() {
      try {
        await deleteServerConfig(id)
        message.success('删除成功')
        loadServerConfigs()
      } catch (e) {
        message.error('删除失败')
      }
    }
  })
}

async function handleTestConnection(config, isDirect) {
  serverConfigTestLoading.value = true
  try {
    let res
    if (isDirect) {
      res = await testServerConfigDirect(config)
    } else {
      res = await testServerConfig(config.id)
    }
    if (res.success) {
      message.success(res.message)
    } else {
      message.error(res.message)
    }
  } catch (e) {
    message.error('测试连接失败')
  } finally {
    serverConfigTestLoading.value = false
  }
}

// 表格列定义
const columns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true, width: 300 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true, width: 200 },
  { title: '平台', dataIndex: 'platform', key: 'platform', width: 100 },
  { title: '赛道', dataIndex: 'trackName', key: 'trackName', width: 120 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 100,
    customRender: ({ text }) => {
      const map = { ai_generated: 'AI生成', manual: '手动录入', imported: '导入', pushed_up: '推送上来' }
      return h(Tag, { size: 'small', color: text === 'pushed_up' ? 'blue' : 'default' }, () => map[text] || text)
    }
  },
  { title: '生成时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  {
    title: '操作',
    key: 'action',
    width: 240,
    fixed: 'right',
    customRender: ({ record }) => {
      const buttons = []
      if (reviewStatus.value === 'pending') {
        buttons.push(
          h(Button, { type: 'link', size: 'small', onClick: () => handleEdit(record) }, () => '编辑'),
          h(Button, { type: 'link', size: 'small', onClick: () => handleChangeTrack(record) }, () => '改赛道'),
          h(Button, { type: 'link', size: 'small', onClick: () => handleApprove(record) }, () => '通过'),
          h(Button, { type: 'link', size: 'small', danger: true, onClick: () => handleReject(record) }, () => '拒绝')
        )
      } else if (reviewStatus.value === 'approved') {
        buttons.push(
          h(Button, { type: 'primary', size: 'small', onClick: () => handlePush(record) }, () => '推送'),
          h(Button, { type: 'link', size: 'small', onClick: () => handleChangeTrack(record) }, () => '改赛道'),
          h(Button, { type: 'link', size: 'small', onClick: () => handleCancel(record) }, () => '取消审核'),
          h(Button, { type: 'link', size: 'small', danger: true, onClick: () => handleReject(record) }, () => '拒绝')
        )
      } else if (reviewStatus.value === 'pushed') {
        buttons.push(
          h(Button, { type: 'primary', size: 'small', ghost: true, onClick: () => handleRePush(record) }, () => '重新推送'),
          h(Button, { type: 'link', size: 'small', onClick: () => handleChangeTrack(record) }, () => '改赛道')
        )
      } else if (reviewStatus.value === 'pushed-up') {
        // 推送上来的标题根据实际审核状态显示操作
        const rs = record.reviewStatus
        if (rs === 'pending') {
          buttons.push(
            h(Button, { type: 'link', size: 'small', onClick: () => handleEdit(record) }, () => '编辑'),
            h(Button, { type: 'link', size: 'small', onClick: () => handleApprove(record) }, () => '通过'),
            h(Button, { type: 'link', size: 'small', danger: true, onClick: () => handleReject(record) }, () => '拒绝')
          )
        } else if (rs === 'approved') {
          const ps = record.pushStatus
          if (ps === 'pushed') {
            buttons.push(h(Tag, { color: 'green', size: 'small' }, () => '已推送'))
          } else {
            buttons.push(
              h(Button, { type: 'primary', size: 'small', onClick: () => handlePush(record) }, () => '推送'),
              h(Button, { type: 'link', size: 'small', onClick: () => handleCancel(record) }, () => '取消审核'),
              h(Button, { type: 'link', size: 'small', danger: true, onClick: () => handleReject(record) }, () => '拒绝')
            )
          }
        } else if (rs === 'rejected') {
          buttons.push(h(Tag, { color: 'red', size: 'small' }, () => '已拒绝'))
        }
      }
      return h(Space, { size: 'small' }, () => buttons)
    }
  }
]

const pushLogColumns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '目标服务器', dataIndex: 'serverConfigName', key: 'serverConfigName', width: 150 },
  { title: '平台', dataIndex: 'platform', key: 'platform', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100,
    customRender: ({ text }) => {
      return h(Tag, { color: text === 'success' ? 'green' : 'red' }, () => text === 'success' ? '成功' : '失败')
    }
  },
  { title: '失败原因', dataIndex: 'errorMsg', key: 'errorMsg', ellipsis: true },
  { title: '推送时间', dataIndex: 'pushedAt', key: 'pushedAt', width: 160 }
]

// 自动化配置方法
async function loadAutoStatus() {
  autoStatusLoading.value = true
  try {
    const dateStr = targetDate.value ? targetDate.value.format('YYYY-MM-DD') : null
    const res = await getProcessAutoStatus(dateStr)
    autoConfig.value = res.config || autoConfig.value
    todayLog.value = res.todayLog || null
    recentLogs.value = res.recentLogs || []
  } catch (e) {
    message.error('加载自动化状态失败')
  } finally {
    autoStatusLoading.value = false
  }
}

async function handleSaveAutoConfig() {
  autoConfigSaving.value = true
  try {
    await saveProcessAutoConfig(autoConfig.value)
    message.success('配置保存成功')
  } catch (e) {
    message.error('保存失败')
  } finally {
    autoConfigSaving.value = false
  }
}

async function handleTriggerCheck() {
  triggerCheckLoading.value = true
  try {
    const dateStr = targetDate.value ? targetDate.value.format('YYYY-MM-DD') : null
    const res = await triggerCheck(dateStr)
    todayLog.value = res
    message.success('已触发标题检查')
    loadAutoStatus()
  } catch (e) {
    message.error('触发失败')
  } finally {
    triggerCheckLoading.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('MM-DD HH:mm')
}

onMounted(() => {
  loadData()
  loadStats()
  loadTracks()
  loadServerConfigs()
  loadAutoStatus()
})
</script>

<template>
  <div>
    <!-- 生成进度条 -->
    <div v-if="generating" style="background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <div style="font-size: 13px; color: #096dd9;">
          <strong>正在生成标题</strong> — {{ generateStatusMsg }}
        </div>
        <Button type="link" danger size="small" style="padding: 0;" @click="handleCancelGenerate">取消生成</Button>
      </div>
      <div style="width: 100%; background: #d9d9d9; border-radius: 4px; height: 8px;">
        <div :style="{ width: generateProgress + '%', background: '#1890ff', height: '8px', borderRadius: '4px', transition: 'width 0.5s' }" />
      </div>
      <div style="font-size: 12px; color: #096dd9; margin-top: 4px; text-align: right;">{{ generateProgress }}%</div>
    </div>

    <!-- 流程类型 Tabs -->
    <Tabs v-model:activeKey="processType" style="margin-bottom: 16px;">
      <Tabs.TabPane key="title-review" tab="标题审核流程" />
      <Tabs.TabPane key="auto-config" tab="自动化配置" />
    </Tabs>

    <!-- ==================== 自动化配置 Tab ==================== -->
    <div v-if="processType === 'auto-config'">
      <!-- 状态卡片 -->
      <Card :loading="autoStatusLoading" style="margin-bottom: 16px;">
        <div style="display: flex; align-items: center; gap: 24px; flex-wrap: wrap;">
          <div>
            <div style="font-size: 12px; color: #999; margin-bottom: 4px;">当前状态</div>
            <Tag :color="statusColorMap[todayLog?.status] || 'default'" style="font-size: 14px; padding: 4px 12px;">
              {{ statusLabelMap[todayLog?.status] || '等待执行' }}
            </Tag>
          </div>
          <div>
            <div style="font-size: 12px; color: #999; margin-bottom: 4px;">目标推荐日期</div>
            <DatePicker v-model:value="targetDate" size="small" @change="loadAutoStatus" />
          </div>
          <div>
            <div style="font-size: 12px; color: #999; margin-bottom: 4px;">下次检查</div>
            <div style="font-size: 14px; font-weight: 500;">{{ autoConfig?.checkTime || '03:00' }}</div>
          </div>
          <div style="margin-left: auto;">
            <Button type="primary" :loading="triggerCheckLoading" @click="handleTriggerCheck">立即触发检查</Button>
          </div>
        </div>
      </Card>

      <!-- 流程阶段条 -->
      <Card title="流程阶段" style="margin-bottom: 16px;">
        <div style="display: flex; gap: 8px; overflow-x: auto; padding: 8px 0;">
          <div v-for="(stage, idx) in flowStages" :key="stage.key"
               style="display: flex; align-items: center; gap: 8px; flex-shrink: 0;">
            <div style="padding: 6px 14px; border-radius: 16px; font-size: 13px; text-align: center;"
                 :style="getStageIndex(todayLog?.status) >= idx
                   ? { background: '#e6f7ff', color: '#096dd9', border: '1px solid #91d5ff', fontWeight: 500 }
                   : { background: '#f5f5f5', color: '#999', border: '1px solid #d9d9d9' }">
              <div>{{ stage.label }}</div>
              <div style="font-size: 11px; margin-top: 2px; opacity: 0.85;"
                   :style="stage.env === '线上'
                     ? { color: getStageIndex(todayLog?.status) >= idx ? '#1890ff' : '#8c8c8c' }
                     : { color: getStageIndex(todayLog?.status) >= idx ? '#52c41a' : '#8c8c8c' }">
                [{{ stage.env }}]
              </div>
            </div>
            <div v-if="idx < flowStages.length - 1" style="color: #d9d9d9;"
                 :style="getStageIndex(todayLog?.status) > idx ? { color: '#1890ff' } : {}">→</div>
          </div>
        </div>
      </Card>

      <!-- 配置表单 -->
      <Card title="自动化配置" style="margin-bottom: 16px;">
        <Form layout="vertical" :model="autoConfig">
          <Row :gutter="24">
            <Col :span="8">
              <Form.Item label="每日检查时间">
                <Input v-model:value="autoConfig.checkTime" placeholder="如 03:00" />
              </Form.Item>
            </Col>
            <Col :span="8">
              <Form.Item label="每个赛道生成数量">
                <Input v-model:value="autoConfig.titlesPerTrack" type="number" min="1" max="20" />
              </Form.Item>
            </Col>
            <Col :span="8">
              <Form.Item label="是否启用">
                <Select v-model:value="autoConfig.isEnabled">
                  <Select.Option :value="1">启用</Select.Option>
                  <Select.Option :value="0">禁用</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row :gutter="24">
            <Col :span="8">
              <Form.Item label="检查范围">
                <Select v-model:value="autoConfig.checkAllTracks">
                  <Select.Option :value="1">全部赛道</Select.Option>
                  <Select.Option :value="0">仅已订阅赛道</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col :span="8">
              <Form.Item label="不足时通知本地生成">
                <Select v-model:value="autoConfig.autoNotifyLocal">
                  <Select.Option :value="1">是</Select.Option>
                  <Select.Option :value="0">否</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col :span="8">
              <Form.Item label="审核后自动推送">
                <Select v-model:value="autoConfig.autoPushAfterApprove">
                  <Select.Option :value="1">是</Select.Option>
                  <Select.Option :value="0">否</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row :gutter="24">
            <Col :span="8">
              <Form.Item label="推送后自动匹配">
                <Select v-model:value="autoConfig.autoMatchAfterPush">
                  <Select.Option :value="1">是</Select.Option>
                  <Select.Option :value="0">否</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <div style="text-align: right;">
            <Button type="primary" :loading="autoConfigSaving" @click="handleSaveAutoConfig">保存配置</Button>
          </div>
        </Form>
      </Card>

      <!-- 执行历史 -->
      <Card title="执行历史（最近7天）">
        <Table
          :columns="[
            { title: '目标日期', dataIndex: 'targetDate', width: 110 },
            { title: '触发时间', dataIndex: 'checkTime', width: 140, customRender: ({ text }) => formatDate(text) },
            { title: '状态', dataIndex: 'status', width: 120,
              customRender: ({ text }) => h(Tag, { color: statusColorMap[text] || 'default' }, () => statusLabelMap[text] || text) },
            { title: '标题', children: [
              { title: '需生成', dataIndex: 'titlesNeeded', width: 80, align: 'center' },
              { title: '已生成', dataIndex: 'titlesGenerated', width: 80, align: 'center' },
              { title: '已审核', dataIndex: 'titlesApproved', width: 80, align: 'center' },
              { title: '已推送', dataIndex: 'titlesPushed', width: 80, align: 'center' },
            ]},
            { title: '文章', children: [
              { title: '需生成', dataIndex: 'articlesNeeded', width: 80, align: 'center' },
              { title: '已上传', dataIndex: 'articlesUploaded', width: 80, align: 'center' },
            ]},
            { title: '推送', children: [
              { title: '成功', dataIndex: 'pushSuccess', width: 70, align: 'center' },
              { title: '失败', dataIndex: 'pushFailed', width: 70, align: 'center' },
            ]},
            { title: '错误信息', dataIndex: 'errorMsg', ellipsis: true },
          ]"
          :data-source="recentLogs"
          row-key="id"
          size="small"
          :pagination="false"
        />
      </Card>
    </div>

    <!-- ==================== 标题审核流程 Tab ==================== -->
    <div v-if="processType === 'title-review'">

    <!-- 审核状态子 Tabs -->
    <Tabs v-model:activeKey="reviewStatus" @change="handleTabChange">
      <Tabs.TabPane key="pending">
        <template #tab>
          <span>
            待审核
            <Badge :count="stats.pending" :overflow-count="999" :offset="[10, -2]"
                   :show-zero="false" style="margin-left: 4px;" />
          </span>
        </template>
      </Tabs.TabPane>
      <Tabs.TabPane key="approved">
        <template #tab>
          <span>
            已通过
            <Badge :count="stats.approved" :overflow-count="999" :offset="[10, -2]"
                   :show-zero="false" style="margin-left: 4px;" />
          </span>
        </template>
      </Tabs.TabPane>
      <Tabs.TabPane key="pushed">
        <template #tab>
          <span>
            已推送
            <Badge :count="stats.pushed" :overflow-count="999" :offset="[10, -2]"
                   :show-zero="false" style="margin-left: 4px;" />
          </span>
        </template>
      </Tabs.TabPane>
      <Tabs.TabPane key="rejected">
        <template #tab>
          <span>
            已拒绝
            <Badge :count="stats.rejected" :overflow-count="999" :offset="[10, -2]"
                   :show-zero="false" style="margin-left: 4px;" />
          </span>
        </template>
      </Tabs.TabPane>
      <Tabs.TabPane key="pushed-up" tab="推送上来" />
      <Tabs.TabPane key="push-logs" tab="推送日志" />
    </Tabs>

    <!-- 筛选区 -->
    <Card style="margin-bottom: 16px;">
      <Row :gutter="16" align="middle">
        <Col :span="4">
          <Select v-model:value="searchPlatform" placeholder="平台" allow-clear style="width: 100%;"
                  @change="onSearchPlatformChange">
            <Select.Option v-for="p in platforms" :key="p" :value="p">{{ p }}</Select.Option>
          </Select>
        </Col>
        <Col :span="4">
          <Select v-model:value="searchTrack" placeholder="赛道" allow-clear style="width: 100%;"
                  @change="onSearchTrackChange">
            <Select.Option v-for="t in filteredTracksForSearch" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
          </Select>
        </Col>
        <Col :span="6">
          <Input v-model:value="searchKeyword" placeholder="搜索标题关键词" allow-clear
                 @pressEnter="handleSearch" />
        </Col>
        <Col :span="6">
          <Button type="primary" @click="handleSearch">筛选</Button>
          <Button style="margin-left: 8px;" @click="handleReset">重置</Button>
        </Col>
        <Col :span="4" style="text-align: right;">
          <Button type="default" @click="openServerConfigDrawer">
            服务器配置
          </Button>
          <Button type="primary" style="margin-left: 8px;" @click="openGenerateModal">
            生成标题
          </Button>
        </Col>
      </Row>
    </Card>

    <!-- 批量操作区 -->
    <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
      <Space>
        <span v-if="selectedRowKeys.length > 0">已选 {{ selectedRowKeys.length }} 项</span>
        <template v-if="reviewStatus === 'pending'">
          <Button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleBatchApprove">
            批量通过
          </Button>
          <Button danger :disabled="selectedRowKeys.length === 0" @click="handleBatchReject">
            批量拒绝
          </Button>
        </template>
        <template v-if="reviewStatus === 'pushed'">
          <Button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleBatchRePush">
            批量重新推送
          </Button>
        </template>
        <template v-if="reviewStatus === 'approved'">
          <Button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleBatchPush">
            批量推送
          </Button>
          <Button :disabled="selectedRowKeys.length === 0" @click="handleBatchCancel">
            批量取消审核
          </Button>
          <Button danger :disabled="selectedRowKeys.length === 0" @click="handleBatchReject">
            批量拒绝
          </Button>
        </template>
        <template v-if="reviewStatus === 'pushed-up'">
          <Button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleBatchApprove">
            批量通过
          </Button>
          <Button danger :disabled="selectedRowKeys.length === 0" @click="handleBatchReject">
            批量拒绝
          </Button>
        </template>
      </Space>
    </div>

    <!-- 数据表格 -->
    <Table
      v-if="reviewStatus !== 'push-logs'"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :row-selection="rowSelection"
      :pagination="false"
      row-key="id"
      size="middle"
      :scroll="{ x: 1100 }"
    />
    <Table
      v-else
      :columns="pushLogColumns"
      :data-source="pushLogData"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="middle"
    />

    <!-- 分页 -->
    <div style="margin-top: 16px; text-align: right;">
      <Pagination
        v-model:current="page"
        v-model:pageSize="pageSize"
        :total="total"
        show-size-changer
        :page-size-options="['10', '20', '50', '100']"
        @change="handlePageChange"
      />
    </div>

    <!-- 拒绝弹窗 -->
    <Modal
      v-model:open="rejectModalOpen"
      title="拒绝原因"
      @ok="rejectForm.ids ? handleBatchRejectConfirm() : handleRejectConfirm()"
    >
      <Form layout="vertical">
        <Form.Item label="拒绝原因（可选）">
          <Input.TextArea v-model:value="rejectForm.reason" placeholder="请输入拒绝原因" :rows="3" />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 服务器配置抽屉 -->
    <Drawer
      v-model:open="serverConfigDrawerOpen"
      title="服务器配置管理"
      width="600"
    >
      <Button type="primary" style="margin-bottom: 16px;" @click="openServerConfigModal()">
        + 新增配置
      </Button>

      <Table
        :columns="[
          { title: '名称', dataIndex: 'name', key: 'name' },
          { title: '服务器地址', dataIndex: 'host', key: 'host' },
          { title: '端口', dataIndex: 'port', key: 'port' },
          { title: '操作', key: 'action', width: 160,
            customRender: ({ record }) => {
              return h(Space, { size: 'small' }, () => [
                h(Button, { type: 'link', size: 'small', onClick: () => openServerConfigModal(record) }, () => '编辑'),
                h(Button, { type: 'link', size: 'small', onClick: () => handleTestConnection(record, false) }, () => '测试'),
                h(Button, { type: 'link', size: 'small', danger: true, onClick: () => handleDeleteServerConfig(record.id) }, () => '删除')
              ])
            }
          }
        ]"
        :data-source="serverConfigs"
        row-key="id"
        size="small"
        :pagination="false"
      />
    </Drawer>

    <!-- 服务器配置编辑弹窗 -->
    <Modal
      v-model:open="serverConfigModalOpen"
      :title="serverConfigForm.id ? '编辑配置' : '新增配置'"
      :confirm-loading="saveLoading"
      @ok="handleSaveServerConfig"
    >
      <Form layout="vertical">
        <Form.Item label="配置名称" required>
          <Input v-model:value="serverConfigForm.name" />
        </Form.Item>
        <Form.Item label="服务器地址" required>
          <Input v-model:value="serverConfigForm.host" placeholder="如：192.168.1.10" />
        </Form.Item>
        <Form.Item label="端口" required>
          <Input v-model:value="serverConfigForm.port" type="number" />
        </Form.Item>
        <Form.Item>
          <Button type="default" :loading="serverConfigTestLoading"
                  @click="handleTestConnection(serverConfigForm, true)">
            测试连接
          </Button>
        </Form.Item>
      </Form>
    </Modal>

    <!-- 生成标题弹窗 -->
    <Modal
      v-model:open="generateModalOpen"
      title="生成标题"
      :mask-closable="false"
      :confirm-loading="generating"
      @ok="handleGenerate"
    >
      <Form layout="vertical" style="margin-top: 12px;">
        <div style="background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
          <div style="font-size: 13px; color: #096dd9; margin-bottom: 8px;">
            <strong>生成说明</strong>
          </div>
          <div style="font-size: 12px; color: #096dd9; line-height: 1.8;">
            1. 选择平台和赛道，不选则生成全部<br>
            2. 数量指每个平台下每个赛道生成的标题数<br>
            3. 系统会按平台分批调用 Claude Code 生成<br>
            4. 生成的标题会自动进入「待审核」列表
          </div>
        </div>
        <Form.Item label="选择平台-赛道">
          <Select v-model:value="generateTrackIds" mode="multiple" placeholder="不选则生成全部赛道" style="width: 100%;">
            <Select.Option v-for="t in sortedTrackOptions" :key="t.id" :value="t.id">{{ t.displayLabel }}</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="每个组合生成数量" required>
          <Input v-model:value="generateCount" type="number" min="1" max="20" placeholder="例如：3" />
        </Form.Item>
        <Form.Item label="输出文件路径">
          <Input v-model:value="generateOutputPath" placeholder="不填则默认保存到项目 export 目录" />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">留空默认保存到项目根目录/export/下，带时间戳文件名</div>
        </Form.Item>
      </Form>
    </Modal>

    <!-- 编辑标题弹窗 -->
    <Modal
      v-model:open="editModalOpen"
      title="编辑标题"
      :confirm-loading="editLoading"
      @ok="handleEditConfirm"
    >
      <Form layout="vertical">
        <Form.Item label="标题" required>
          <Input v-model:value="editForm.title" placeholder="请输入标题" />
        </Form.Item>
        <Form.Item label="描述">
          <Input.TextArea v-model:value="editForm.description" placeholder="请输入描述" :rows="3" />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 改赛道弹窗 -->
    <Modal
      v-model:open="changeTrackModalOpen"
      title="修改赛道"
      :confirm-loading="changeTrackLoading"
      @ok="handleChangeTrackConfirm"
    >
      <Form layout="vertical">
        <Form.Item label="选择赛道" required>
          <Select v-model:value="changeTrackForm.trackId" placeholder="请选择赛道" style="width: 100%;">
            <Select.Option v-for="t in sortedTrackOptions" :key="t.id" :value="t.id">{{ t.displayLabel }}</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
    </div>
  </div>
</template>

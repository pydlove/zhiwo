<script setup>
import { ref, onMounted, computed, h } from 'vue'
import dayjs from 'dayjs'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message, Pagination, Descriptions, DatePicker } from 'ant-design-vue'
import { listTitles, saveTitle, deleteTitle, importTitles, matchTodayTitles, unbindRecommendation, generateTitles, getGenerateStatus, cancelGenerate, generatePostsForToday, getGeneratePostStatus, cancelGeneratePost, getDefaultPromptTemplate, savePromptTemplate, exportTitleLibrary, exportTitleLibraryBatch, exportTitleList, exportTitleListBatch, importArticles, sendTitleEmail, batchSendTitleEmail } from '../api/titleLibrary.js'
import { listTracks } from '../api/track.js'
import { listUsers } from '../api/user.js'
import mammoth from 'mammoth'

const tableData = ref([])
const tracks = ref([])
const allUsers = ref([])
const loading = ref(false)

const selectedRowKeys = ref([])
const selectedRows = ref([])

const rowSelection = {
  onChange: (keys, rows) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  },
}

const searchKeyword = ref('')
const searchPlatform = ref('')
const searchTrack = ref('')
const searchUserName = ref('')
const searchMatched = ref('')
const searchPushDate = ref(null)

const platformOptions = [
  { label: '公众号', value: '公众号' },
  { label: '今日头条', value: '今日头条' },
  { label: '百家号', value: '百家号' },
]

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

const columns = [
  { title: '标题内容', dataIndex: 'title', ellipsis: true, width: 260 },
  { title: '描述', dataIndex: 'description', ellipsis: true, width: 150 },
  { title: '推荐日期', dataIndex: 'pushDate', width: 100 },
  { title: '平台', dataIndex: 'platform', width: 90 },
  { title: '赛道', dataIndex: 'trackName', width: 100 },
  { title: '使用次数', dataIndex: 'useCount', width: 80, align: 'center' },
  {
    title: '关联用户',
    dataIndex: 'recommendUserName',
    width: 110,
    customRender: ({ record }) => {
      if (!record.recommendUserName) return h('span', { style: 'color: #999;' }, '未匹配')
      return h(Button, {
        type: 'link',
        size: 'small',
        style: 'padding: 0;',
        onClick: () => handleViewUser(record),
      }, () => record.recommendUserName)
    },
  },
  { title: '用户样式', dataIndex: 'recommendUserTemplate', width: 100 },
  {
    title: '关联文章',
    key: 'post',
    width: 160,
    customRender: ({ record }) => {
      if (!record.subscriptionPostTitle) return h('span', { style: 'color: #999;' }, '未生成')
      return h(Button, {
        type: 'link',
        size: 'small',
        style: 'padding: 0; max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;',
        onClick: () => handlePreviewPost(record),
      }, () => record.subscriptionPostTitle)
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    align: 'center',
    customRender: ({ record }) => {
      const btns = [
        h(Button, { type: 'link', size: 'small', onClick: () => handleEdit(record) }, () => '编辑'),
        record.subscriptionPostId
          ? h(Button, {
              type: 'link',
              size: 'small',
              style: 'padding: 0 4px;',
              loading: sendEmailLoadingId.value === record.id,
              onClick: () => handleSendEmail(record),
            }, () => '发邮件')
          : null,
        h(Button, { type: 'link', danger: true, size: 'small', onClick: () => handleDelete(record) }, () => '删除'),
      ].filter(Boolean)
      return h('div', { style: 'display: flex; justify-content: center; align-items: center; flex-wrap: wrap; gap: 4px;' }, btns)
    },
  },
]

const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)

const paginatedData = computed(() => {
  // 服务端已分页，直接使用
  return tableData.value
})

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value.trim()
    if (searchPlatform.value) params.platform = searchPlatform.value
    if (searchTrack.value) params.trackId = searchTrack.value
    if (searchUserName.value) params.recommendUserName = searchUserName.value.trim()
    if (searchMatched.value !== '' && searchMatched.value !== undefined) params.matched = searchMatched.value
    if (searchPushDate.value) params.pushDate = searchPushDate.value.format('YYYY-MM-DD')
    params.page = currentPage.value
    params.pageSize = pageSize.value

    const [result, trackList, userList] = await Promise.all([
      listTitles(params),
      listTracks().catch(() => []),
      listUsers().catch(() => []),
    ])
    // 后端分页返回 { list, total }，向后兼容直接返回数组的情况
    if (result && Array.isArray(result.list)) {
      tableData.value = result.list
      totalCount.value = result.total || 0
    } else {
      tableData.value = result || []
      totalCount.value = result?.length || 0
    }
    tracks.value = trackList || []
    allUsers.value = (userList || []).map(u => ({ id: u.id, username: u.username, contact: u.phone || u.email || u.wxId || '-' }))
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败: ' + (e?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handlePageChange() {
  loadData()
}

function handleReset() {
  searchKeyword.value = ''
  searchPlatform.value = ''
  searchTrack.value = ''
  searchUserName.value = ''
  searchMatched.value = ''
  searchPushDate.value = null
  currentPage.value = 1
  loadData()
}

const modalOpen = ref(false)
const modalTitle = ref('新增标题')
const form = ref({ title: '', description: '', pushDate: null, platform: '', trackId: '', recommendUserId: '', recommendUserName: '' })
const saving = ref(false)

function handleAdd() {
  modalTitle.value = '新增标题'
  form.value = { title: '', description: '', pushDate: null, platform: '', trackId: '', recommendUserId: '', recommendUserName: '' }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑标题'
  form.value = {
    id: record.id,
    title: record.title,
    description: record.description,
    pushDate: record.pushDate ? dayjs(record.pushDate) : null,
    platform: record.platform,
    trackId: record.trackId,
    recommendUserId: record.recommendUserId || '',
    recommendUserName: record.recommendUserName || '',
    recommendDate: record.recommendDate,
    recommendUserTemplate: record.recommendUserTemplate,
  }
  modalOpen.value = true
}

async function handleUnbind() {
  if (!form.value.id) return
  Modal.confirm({
    title: '确认解绑',
    content: `确定要解绑标题「${form.value.title}」的关联用户吗？`,
    onOk: async () => {
      try {
        await unbindRecommendation(form.value.id)
        message.success('解绑成功')
        modalOpen.value = false
        loadData()
      } catch (e) {
        message.error('解绑失败')
      }
    },
  })
}

const sendEmailLoadingId = ref(null)

async function handleSendEmail(record) {
  sendEmailLoadingId.value = record.id
  try {
    const result = await sendTitleEmail(record.id)
    message.success(`邮件已发送至 ${result.email || '用户邮箱'}`)
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '发送失败')
  } finally {
    sendEmailLoadingId.value = null
  }
}

async function handleBatchSendEmail() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要发送邮件的标题')
    return
  }
  try {
    const result = await batchSendTitleEmail(selectedRowKeys.value)
    const { success = 0, failed = 0, errors = [] } = result
    if (failed === 0) {
      message.success(`全部发送成功，共 ${success} 封邮件`)
    } else {
      Modal.info({
        title: '批量发送结果',
        width: 520,
        content: h('div', { style: 'max-height: 320px; overflow-y: auto;' }, [
          h('div', { style: 'margin-bottom: 12px; font-size: 14px;' }, [
            h('span', { style: 'color: #52c41a; font-weight: 500;' }, `成功 ${success} 封`),
            h('span', { style: 'margin: 0 8px;' }, ' / '),
            h('span', { style: 'color: #f5222d; font-weight: 500;' }, `失败 ${failed} 封`),
          ]),
          errors.length > 0
            ? h('div', { style: 'display: flex; flex-direction: column; gap: 4px;' },
                errors.map((err, idx) =>
                  h('div', { key: idx, style: 'font-size: 12px; color: #666; padding: 4px 0; border-bottom: 1px dashed #f0f0f0;' },
                    `${err.title || err.titleId}：${err.reason}`
                  )
                )
              )
            : null,
        ]),
      })
    }
    selectedRowKeys.value = []
    selectedRows.value = []
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '批量发送失败')
  }
}

function handleUserChange(userId) {
  if (!userId) {
    form.value.recommendUserId = ''
    form.value.recommendUserName = ''
    return
  }
  const user = allUsers.value.find(u => u.id === userId)
  if (user) {
    form.value.recommendUserId = user.id
    form.value.recommendUserName = user.username
  }
}

async function handleSave() {
  if (!form.value.title || !form.value.title.trim()) {
    message.warning('请输入标题内容')
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value }
    if (payload.pushDate && typeof payload.pushDate.format === 'function') {
      payload.pushDate = payload.pushDate.format('YYYY-MM-DD')
    }
    if (!payload.recommendUserId) {
      payload.recommendUserId = null
      payload.recommendUserName = null
    }
    await saveTitle(payload)
    message.success('保存成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除标题「${record.title}」吗？`,
    onOk: async () => {
      try {
        await deleteTitle(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

// Match today
const matching = ref(false)

async function handleMatchToday() {
  matching.value = true
  try {
    const result = await matchTodayTitles()
    message.success(`匹配完成：成功匹配 ${result.matched} 条，跳过 ${result.skipped} 条`)
    loadData()
  } catch (e) {
    message.error('匹配失败: ' + (e?.message || '未知错误'))
  } finally {
    matching.value = false
  }
}

// Export rule modal
const exportRuleModalOpen = ref(false)
const exportRuleContent = ref('')
const exportRuleSaving = ref(false)
const exportRuleTemplateId = ref(null)

const defaultExportRuleContent = `以下是生成规则：
1、该sheet之前的每个sheet都是需要生成文章，在根目录下根据sheet名称创建文件夹，此文件夹作为该赛道的创作目录，根目录路径是/Users/panyong/aio_project/公众号/自媒体/;
2、创作规则：根据每个sheet里面的标题进行创作，并且你要根据你创作的内容生成一个120个字符以内的(包含标点符号)描述，描述要求符合SEO的原则，填写到"描述"列，"创作日期"列如果为空，就填写为为当天日期，如果不为空，就不用管，如果创作完成，"是否创作完成"填写是；
3、输出规则：输出的文件都是docx格式，并且根据每行数据的日期，在创作目录下创建一个输出目录，输出目录就是"创作日期"，文件输出到输出目录下，例如：/Users/panyong/aio_project/公众号/自媒体/职场/{date}；
4、输出文件样式：根据"样式风格"列的内容，去/Users/panyong/aio_project/小程序/services/admin-backend/styles目录下查找对应的样式文件，参考对应的样式输出文章；
5、文件内容图片生成规则：内容中要插入图片，你可以自行下载相关图片（下载一些和标题呼应的图片，可以多找一些资源，不要总是那么几张，下载的图片必须是16:9的）；
6、爆款标题生成原则：爆款标题要求通过标题就能吸引读者，也就是网上俗称的"标题党"，字数严禁超过30个字符；
7、对于"是否创作完成"填写"是"的数据，就不要再重复创作了，要创作"是否创作完成"为空或者"否"的数据；`

async function openExportRuleModal() {
  exportRuleModalOpen.value = true
  exportRuleSaving.value = false
  try {
    const res = await getDefaultPromptTemplate('export_rule')
    if (res && res.content) {
      exportRuleContent.value = res.content
      exportRuleTemplateId.value = res.id || null
    } else {
      exportRuleContent.value = defaultExportRuleContent
      exportRuleTemplateId.value = null
    }
  } catch (e) {
    exportRuleContent.value = defaultExportRuleContent
    exportRuleTemplateId.value = null
  }
}

async function handleSaveExportRule() {
  if (!exportRuleContent.value || !exportRuleContent.value.trim()) {
    message.warning('生成规则内容不能为空')
    return
  }
  exportRuleSaving.value = true
  try {
    const data = {
      id: exportRuleTemplateId.value,
      name: '导出Excel生成规则',
      content: exportRuleContent.value.trim(),
      type: 'export_rule',
      isDefault: 1,
    }
    const res = await savePromptTemplate(data)
    exportRuleTemplateId.value = res.id
    message.success('生成规则保存成功')
  } catch (e) {
    message.error('保存失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    exportRuleSaving.value = false
  }
}

async function handleExportTitleList() {
  try {
    let blob
    if (selectedRowKeys.value.length > 0) {
      blob = await exportTitleListBatch(selectedRowKeys.value)
    } else {
      const params = {}
      if (searchKeyword.value) params.keyword = searchKeyword.value.trim()
      if (searchPlatform.value) params.platform = searchPlatform.value
      if (searchTrack.value) params.trackId = searchTrack.value
      if (searchUserName.value) params.recommendUserName = searchUserName.value.trim()
      if (searchMatched.value !== '' && searchMatched.value !== undefined) params.matched = searchMatched.value
      if (searchPushDate.value) params.pushDate = searchPushDate.value.format('YYYY-MM-DD')
      blob = await exportTitleList(params)
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `标题库_${new Date().toISOString().slice(0,10).replace(/-/g,'')}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}

async function handleExportFromRule() {
  exportRuleModalOpen.value = false
  try {
    let blob
    if (selectedRowKeys.value.length > 0) {
      blob = await exportTitleLibraryBatch(selectedRowKeys.value)
    } else {
      const params = {}
      if (searchKeyword.value) params.keyword = searchKeyword.value.trim()
      if (searchPlatform.value) params.platform = searchPlatform.value
      if (searchTrack.value) params.trackId = searchTrack.value
      if (searchUserName.value) params.recommendUserName = searchUserName.value.trim()
      if (searchMatched.value !== '' && searchMatched.value !== undefined) params.matched = searchMatched.value
      if (searchPushDate.value) params.pushDate = searchPushDate.value.format('YYYY-MM-DD')
      blob = await exportTitleLibrary(params)
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `标题库导出_${new Date().toISOString().slice(0,10).replace(/-/g,'')}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}

// Import articles modal
const importArticleModalOpen = ref(false)
const importArticleFiles = ref([])
const importArticleLoading = ref(false)
const importArticleResult = ref(null)

function openImportArticleModal() {
  importArticleModalOpen.value = true
  importArticleFiles.value = []
  importArticleResult.value = null
}

function onImportArticleFileChange(e) {
  importArticleFiles.value = Array.from(e.target.files || [])
}

async function handleImportArticles() {
  if (importArticleFiles.value.length === 0) {
    message.warning('请选择文件夹或文件')
    return
  }
  importArticleLoading.value = true
  try {
    const result = await importArticles(importArticleFiles.value)
    importArticleResult.value = result
    if (result.success > 0) {
      message.success(`导入完成：成功 ${result.success} 条，跳过 ${result.skip} 条`)
      loadData()
    } else {
      message.warning(`未成功导入任何文件，跳过 ${result.skip} 条`)
    }
  } catch (e) {
    message.error('导入失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    importArticleLoading.value = false
  }
}

// Prompt template modal
const promptModalOpen = ref(false)
const promptContent = ref('')
const promptSaving = ref(false)
const promptTemplateId = ref(null)

const defaultPromptContent = `请根据以下标题和描述，生成一篇完整的公众号风格文章。

标题：{title}
描述：{description}

{styleDesc}

{styleRef}
要求：
1. 文章必须围绕标题主题展开，内容充实、有深度、有观点
2. 文章结构清晰，包含开头引入、正文论述、结尾总结
3. 适合公众号传播，语言自然流畅，有阅读吸引力
4. 文章长度适中，约800-1500字
5. 输出纯HTML正文内容（不含html/head/body标签，只返回div包裹的内容），使用h1/h2/p/blockquote等标签组织内容
6. 不要在任何标签上添加 style 属性或 class 属性，保持标签纯净
7. 只输出纯JSON，不要markdown代码块，不要任何额外文字，不要在JSON前添加任何说明

格式：{"content":"<div>文章HTML内容</div>"}`

async function openPromptModal() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要生成文章的标题')
    return
  }
  promptModalOpen.value = true
  promptSaving.value = false
  try {
    const res = await getDefaultPromptTemplate('generate_post')
    if (res && res.content) {
      promptContent.value = res.content
      promptTemplateId.value = res.id || null
    } else {
      promptContent.value = defaultPromptContent
      promptTemplateId.value = null
    }
  } catch (e) {
    promptContent.value = defaultPromptContent
    promptTemplateId.value = null
  }
}

async function handleSavePrompt() {
  if (!promptContent.value || !promptContent.value.trim()) {
    message.warning('提示词内容不能为空')
    return
  }
  promptSaving.value = true
  try {
    const data = {
      id: promptTemplateId.value,
      name: '生成文章默认提示词',
      content: promptContent.value.trim(),
      type: 'generate_post',
      isDefault: 1,
    }
    const res = await savePromptTemplate(data)
    promptTemplateId.value = res.id
    message.success('提示词保存成功')
  } catch (e) {
    message.error('保存失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  } finally {
    promptSaving.value = false
  }
}

function handleGenerateFromPrompt() {
  promptModalOpen.value = false
  doGeneratePosts()
}

// Generate posts for today
const generatingPost = ref(false)
const generatePostTaskId = ref(null)
const generatePostProgress = ref(0)
const generatePostStatusMsg = ref('')
let generatePostPollTimer = null

function stopGeneratePostPoll() {
  if (generatePostPollTimer) {
    clearInterval(generatePostPollTimer)
    generatePostPollTimer = null
  }
}

async function pollGeneratePostStatus(taskId) {
  try {
    const status = await getGeneratePostStatus(taskId)
    generatePostProgress.value = status.progress || 0
    generatePostStatusMsg.value = status.message || ''
    if (status.status === 'completed') {
      stopGeneratePostPoll()
      generatingPost.value = false
      generatePostTaskId.value = null
      message.success(status.message || '文章生成完成')
      loadData()
    } else if (status.status === 'failed') {
      stopGeneratePostPoll()
      generatingPost.value = false
      generatePostTaskId.value = null
      message.error('生成失败: ' + (status.message || '未知错误'))
    }
  } catch (e) {
    console.error('poll post error:', e)
  }
}

async function doGeneratePosts() {
  generatingPost.value = true
  generatePostProgress.value = 0
  generatePostStatusMsg.value = '提交中...'
  try {
    const result = await generatePostsForToday(selectedRowKeys.value)
    generatePostTaskId.value = result.taskId
    generatePostStatusMsg.value = '任务已提交，开始生成文章...'
    generatePostPollTimer = setInterval(() => {
      if (generatePostTaskId.value) {
        pollGeneratePostStatus(generatePostTaskId.value)
      }
    }, 3000)
  } catch (e) {
    generatingPost.value = false
    message.error('提交失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
  }
}

async function handleCancelGeneratePost() {
  if (!generatePostTaskId.value) {
    generatingPost.value = false
    stopGeneratePostPoll()
    return
  }
  try {
    await cancelGeneratePost(generatePostTaskId.value)
    stopGeneratePostPoll()
    generatingPost.value = false
    generatePostTaskId.value = null
    generatePostProgress.value = 0
    generatePostStatusMsg.value = ''
    message.info('已取消生成')
  } catch (e) {
    stopGeneratePostPoll()
    generatingPost.value = false
    generatePostTaskId.value = null
    message.info('已取消生成')
  }
}

// Preview / Download post
const previewModalOpen = ref(false)
const previewRecord = ref(null)
const previewHtmlContent = ref('')
const previewLoading = ref(false)

async function handlePreviewPost(record) {
  previewRecord.value = record
  previewHtmlContent.value = ''
  previewModalOpen.value = true
  previewLoading.value = true

  const fileUrl = record.subscriptionPostFileUrl
  if (!fileUrl) {
    previewLoading.value = false
    return
  }

  const url = fileUrl.startsWith('http') ? fileUrl : apiBaseUrl + fileUrl
  const ext = (fileUrl.split('.').pop() || '').toLowerCase()

  try {
    if (ext === 'docx' || ext === 'doc') {
      const res = await fetch(url)
      if (!res.ok) throw new Error('文件获取失败')
      const arrayBuffer = await res.arrayBuffer()
      const result = await mammoth.convertToHtml({ arrayBuffer })
      previewHtmlContent.value = result.value
    } else {
      // 其他格式直接用 iframe
      previewHtmlContent.value = ''
    }
  } catch (e) {
    message.error('文件预览失败：' + (e.message || '未知错误'))
    previewHtmlContent.value = ''
  } finally {
    previewLoading.value = false
  }
}

function handleDownloadPost(record) {
  if (!record.subscriptionPostFileUrl) return
  const url = record.subscriptionPostFileUrl.startsWith('http')
    ? record.subscriptionPostFileUrl
    : apiBaseUrl + record.subscriptionPostFileUrl
  const a = document.createElement('a')
  a.href = url
  a.download = record.subscriptionPostTitle + '.html'
  a.click()
}

// User detail modal
const userModalOpen = ref(false)
const selectedUserRecord = ref(null)

function handleViewUser(record) {
  selectedUserRecord.value = record
  userModalOpen.value = true
}

// Import
const importModalOpen = ref(false)
const importExcelFile = ref(null)
const importLoading = ref(false)

function openImportModal() {
  importModalOpen.value = true
  importExcelFile.value = null
}

function onImportFileChange(e) {
  importExcelFile.value = e.target.files?.[0] || null
}

function downloadTemplate() {
  const headers = ['标题', '平台', '赛道名称']
  const csvContent = headers.join(',') + '\n示例标题,公众号,情感故事'
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '标题导入模板.csv'
  a.click()
  URL.revokeObjectURL(url)
}

async function handleImport() {
  if (!importExcelFile.value) {
    message.warning('请选择 Excel 文件')
    return
  }
  importLoading.value = true
  try {
    const result = await importTitles(importExcelFile.value)
    const created = result.created || 0
    const updated = result.updated || 0
    message.success(`导入完成：新增 ${created} 条，更新 ${updated} 条，跳过 ${result.skip} 条`)
    if (result.errors && result.errors.length) {
      console.warn('导入错误：', result.errors)
    }
    importModalOpen.value = false
    loadData()
  } catch (e) {
    message.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

// Generate titles
const generateModalOpen = ref(false)
const generateCount = ref(3)
const generateOutputPath = ref('')
const generatePlatforms = ref([])
const generateTrackIds = ref([])
const generating = ref(false)
const generateProgress = ref(0)
const generateTaskId = ref(null)
const generateStatusMsg = ref('')
let generatePollTimer = null

const filteredTracksForGenerate = computed(() => {
  if (!generatePlatforms.value || generatePlatforms.value.length === 0) {
    return tracks.value
  }
  return tracks.value.filter(t => {
    if (!t.platforms) return false
    const trackPlatforms = t.platforms.split(',')
    return generatePlatforms.value.some(p => trackPlatforms.includes(p))
  })
})

function openGenerateModal() {
  generateModalOpen.value = true
  generateCount.value = 3
  generateOutputPath.value = ''
  generatePlatforms.value = []
  generateTrackIds.value = []
}

function onPlatformChange() {
  if (!generatePlatforms.value || generatePlatforms.value.length === 0) {
    return
  }
  // Remove selected tracks that don't support any of the selected platforms
  generateTrackIds.value = generateTrackIds.value.filter(trackId => {
    const track = tracks.value.find(t => t.id === trackId)
    if (!track || !track.platforms) return false
    const trackPlatforms = track.platforms.split(',')
    return generatePlatforms.value.some(p => trackPlatforms.includes(p))
  })
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
      platforms: generatePlatforms.value,
      trackIds: generateTrackIds.value,
    })
    generateTaskId.value = result.taskId
    generateModalOpen.value = false
    generateStatusMsg.value = '任务已提交，开始生成...'
    // Start polling every 3 seconds
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
    // Even if backend call fails, stop local polling
    stopGeneratePoll()
    generating.value = false
    generateTaskId.value = null
    message.info('已取消生成')
  }
}

onMounted(loadData)

</script>

<template>
  <Card title="标题库管理" :bordered="false">
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap;">
      <Input v-model:value="searchKeyword" placeholder="搜索标题关键词" style="width: 220px;" @pressEnter="handleSearch" />
      <Select v-model:value="searchPlatform" placeholder="选择平台" style="width: 140px;" allowClear>
        <Select.Option v-for="p in platformOptions" :key="p.value" :value="p.value">{{ p.label }}</Select.Option>
      </Select>
      <Select v-model:value="searchTrack" placeholder="选择赛道" style="width: 160px;" allowClear>
        <Select.Option v-for="t in tracks" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
      </Select>
      <Input v-model:value="searchUserName" placeholder="关联用户" style="width: 140px;" @pressEnter="handleSearch" />
      <Select v-model:value="searchMatched" placeholder="匹配状态" style="width: 130px;" allowClear>
        <Select.Option value="1">已匹配</Select.Option>
        <Select.Option value="0">未匹配</Select.Option>
      </Select>
      <DatePicker v-model:value="searchPushDate" placeholder="推荐日期" style="width: 140px;" />
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button style="margin-left: auto;" @click="openImportModal">导入标题</Button>
      <Button @click="handleExportTitleList">导出标题</Button>
      <Button type="primary" ghost :loading="matching" @click="handleMatchToday">匹配今日推荐</Button>
      <Button @click="openGenerateModal">生成标题</Button>
      <Button type="primary" ghost :disabled="selectedRowKeys.length === 0" @click="handleBatchSendEmail">批量发邮件</Button>
      <Button @click="openExportRuleModal">导出</Button>
      <Button @click="openImportArticleModal">导入文章</Button>
      <Button type="primary" @click="handleAdd">+ 新增标题</Button>
    </div>

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

    <div v-if="generatingPost" style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <div style="font-size: 13px; color: #389e0d;">
          <strong>正在生成文章</strong> — {{ generatePostStatusMsg }}
        </div>
        <Button type="link" danger size="small" style="padding: 0;" @click="handleCancelGeneratePost">取消生成</Button>
      </div>
      <div style="width: 100%; background: #d9d9d9; border-radius: 4px; height: 8px;">
        <div :style="{ width: generatePostProgress + '%', background: '#52c41a', height: '8px', borderRadius: '4px', transition: 'width 0.5s' }" />
      </div>
      <div style="font-size: 12px; color: #389e0d; margin-top: 4px; text-align: right;">{{ generatePostProgress }}%</div>
    </div>

    <Table :columns="columns" :data-source="paginatedData" :pagination="false" row-key="id" :loading="loading" :row-selection="rowSelection" />

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination
        v-model:current="currentPage"
        v-model:pageSize="pageSize"
        :total="totalCount"
        show-size-changer
        :page-size-options="['10', '20', '50']"
        :show-total="total => `共 ${total} 条`"
        @change="handlePageChange"
      />
    </div>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" :confirm-loading="saving" @ok="handleSave">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="标题内容" required>
        <Input.TextArea v-model:value="form.title" placeholder="请输入标题内容" :rows="3" />
      </Form.Item>
      <Form.Item label="描述">
        <Input.TextArea v-model:value="form.description" placeholder="请输入标题SEO描述（30字以内）" :rows="2" />
      </Form.Item>
      <Form.Item label="推荐日期">
        <DatePicker v-model:value="form.pushDate" placeholder="选择推荐日期" style="width: 100%;" />
      </Form.Item>
      <Form.Item label="适用平台">
        <Select v-model:value="form.platform" placeholder="选择平台" allowClear>
          <Select.Option v-for="p in platformOptions" :key="p.value" :value="p.value">{{ p.label }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="关联赛道">
        <Select v-model:value="form.trackId" placeholder="选择赛道" allowClear>
          <Select.Option v-for="t in tracks" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="关联用户">
        <Select v-model:value="form.recommendUserId" placeholder="选择关联用户（可选）" allowClear style="width: 100%;" @change="handleUserChange">
          <Select.Option v-for="u in allUsers" :key="u.id" :value="u.id">{{ u.username }}（{{ u.contact }}）</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item v-if="form.recommendUserName" label="当前关联">
        <div style="display: flex; align-items: center; gap: 12px;">
          <span style="color: #333;">{{ form.recommendUserName }}</span>
          <span style="color: #999; font-size: 12px;">推荐日期：{{ form.recommendDate || '-' }}</span>
          <span style="color: #999; font-size: 12px;">样式：{{ form.recommendUserTemplate || '-' }}</span>
          <Button v-if="form.id" type="link" danger size="small" style="margin-left: auto;" @click="handleUnbind">解绑</Button>
        </div>
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="userModalOpen" title="用户详情" :footer="null" :mask-closable="true" width="480">
    <Descriptions v-if="selectedUserRecord" layout="vertical" :column="1" style="margin-top: 12px;">
      <Descriptions.Item label="用户名称">{{ selectedUserRecord.recommendUserName || '-' }}</Descriptions.Item>
      <Descriptions.Item label="用户样式">{{ selectedUserRecord.recommendUserTemplate || '-' }}</Descriptions.Item>
      <Descriptions.Item label="推荐日期">{{ selectedUserRecord.recommendDate || '-' }}</Descriptions.Item>
      <Descriptions.Item label="标题内容">{{ selectedUserRecord.title }}</Descriptions.Item>
      <Descriptions.Item label="适用平台">{{ selectedUserRecord.platform || '-' }}</Descriptions.Item>
      <Descriptions.Item label="关联赛道">{{ selectedUserRecord.trackName || '-' }}</Descriptions.Item>
    </Descriptions>
  </Modal>

  <Modal v-model:open="importModalOpen" title="批量导入标题" :mask-closable="false" :confirm-loading="importLoading" @ok="handleImport">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
        <div style="font-size: 13px; color: #389e0d; margin-bottom: 8px;">
          <strong>导入说明</strong>
        </div>
        <div style="font-size: 12px; color: #389e0d; line-height: 1.8;">
          1. 下载导入模板，按格式填写数据<br>
          2. Excel 列顺序：标题 | 平台 | 赛道名称<br>
          3. 第一行为表头，数据从第二行开始<br>
          4. 赛道名称需与系统中已存在的赛道名称一致<br>
          5. 支持 .xlsx / .xls 格式
        </div>
      </div>
      <Form.Item>
        <Button size="small" @click="downloadTemplate">下载导入模板</Button>
      </Form.Item>
      <Form.Item label="Excel 文件" required>
        <input type="file" accept=".xlsx,.xls" @change="onImportFileChange">
        <div style="font-size: 12px; color: #999; margin-top: 4px;">支持 .xlsx / .xls</div>
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="generateModalOpen" title="生成标题" :mask-closable="false" :confirm-loading="generating" @ok="handleGenerate">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
        <div style="font-size: 13px; color: #096dd9; margin-bottom: 8px;">
          <strong>生成说明</strong>
        </div>
        <div style="font-size: 12px; color: #096dd9; line-height: 1.8;">
          1. 选择平台和赛道，不选则生成全部<br>
          2. 数量指每个平台下每个赛道生成的标题数<br>
          3. 系统会按平台分批调用 Claude Code 生成<br>
          4. 生成结果包含标题、平台、赛道名称和 SEO 描述
        </div>
      </div>
      <Form.Item label="选择平台">
        <Select v-model:value="generatePlatforms" mode="multiple" placeholder="不选则生成全部平台" style="width: 100%;" @change="onPlatformChange">
          <Select.Option v-for="p in platformOptions" :key="p.value" :value="p.value">{{ p.label }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="选择赛道">
        <Select v-model:value="generateTrackIds" mode="multiple" placeholder="不选则生成全部赛道" style="width: 100%;">
          <Select.Option v-for="t in filteredTracksForGenerate" :key="t.id" :value="t.id">{{ t.name }}</Select.Option>
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

  <Modal v-model:open="previewModalOpen" title="文章预览" :footer="null" :mask-closable="true" width="720">
    <div v-if="previewRecord" style="margin-top: 12px;">
      <div style="font-size: 16px; font-weight: 600; margin-bottom: 12px;">{{ previewRecord.subscriptionPostTitle }}</div>
      <div style="border: 1px solid #f0f0f0; border-radius: 6px; background: #fafafa; min-height: 360px; max-height: 560px; overflow: auto;">
        <div v-if="previewLoading" style="padding: 24px; text-align: center; color: #999;">正在加载预览...</div>
        <div
          v-else-if="previewHtmlContent"
          class="article-preview-content"
          style="padding: 20px; background: #fff; font-size: 15px; line-height: 1.8; color: #374151;"
          v-html="previewHtmlContent"
        />
        <iframe
          v-else-if="previewRecord.subscriptionPostFileUrl"
          :src="previewRecord.subscriptionPostFileUrl.startsWith('http') ? previewRecord.subscriptionPostFileUrl : (apiBaseUrl + previewRecord.subscriptionPostFileUrl)"
          style="width: 100%; height: 560px; border: 0;"
        />
        <div v-else style="padding: 24px; color: #999; text-align: center;">暂无文件</div>
      </div>
    </div>
  </Modal>

  <Modal v-model:open="exportRuleModalOpen" title="导出Excel生成规则" :mask-closable="false" width="720" :footer="null">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="background: #fff7e6; border: 1px solid #ffd591; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
        <div style="font-size: 13px; color: #d46b08; margin-bottom: 8px;">
          <strong>规则说明</strong>
        </div>
        <div style="font-size: 12px; color: #d46b08; line-height: 1.8;">
          该规则会作为 Excel 最后一个 sheet「生成规则」的内容，Claude 导出时将根据此规则处理文章生成逻辑。
        </div>
      </div>
      <Form.Item label="生成规则内容" required>
        <Input.TextArea v-model:value="exportRuleContent" placeholder="请输入导出Excel的生成规则..." :rows="16" />
      </Form.Item>
      <div style="display: flex; justify-content: flex-end; gap: 12px;">
        <Button @click="exportRuleModalOpen = false">取消</Button>
        <Button type="default" :loading="exportRuleSaving" @click="handleSaveExportRule">保存</Button>
        <Button type="primary" @click="handleExportFromRule">导出</Button>
      </div>
    </Form>
  </Modal>

  <Modal v-model:open="promptModalOpen" title="生成文章提示词" :mask-closable="false" width="720" :footer="null">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
        <div style="font-size: 13px; color: #096dd9; margin-bottom: 8px;">
          <strong>可用变量</strong>
        </div>
        <div style="font-size: 12px; color: #096dd9; line-height: 1.8;">
          <code>{title}</code> — 标题内容&nbsp;&nbsp;
          <code>{description}</code> — 标题描述&nbsp;&nbsp;
          <code>{styleDesc}</code> — 样式描述（字体、颜色、间距等）&nbsp;&nbsp;
          <code>{styleRef}</code> — 样式参考文件路径
        </div>
        <div style="font-size: 12px; color: #096dd9; margin-top: 4px;">
          提示词保存后会作为默认模板，后续生成文章时将自动使用。
        </div>
      </div>
      <Form.Item label="提示词内容" required>
        <Input.TextArea v-model:value="promptContent" placeholder="请输入生成文章的提示词..." :rows="16" />
      </Form.Item>
      <div style="display: flex; justify-content: flex-end; gap: 12px;">
        <Button @click="promptModalOpen = false">取消</Button>
        <Button type="default" :loading="promptSaving" @click="handleSavePrompt">保存</Button>
        <Button type="primary" :loading="generatingPost" @click="handleGenerateFromPrompt">生成</Button>
      </div>
    </Form>
  </Modal>

  <Modal v-model:open="importArticleModalOpen" title="导入文章" :mask-closable="false" width="640" :footer="null">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px;">
        <div style="font-size: 13px; color: #389e0d; margin-bottom: 8px;">
          <strong>导入说明</strong>
        </div>
        <div style="font-size: 12px; color: #389e0d; line-height: 1.8;">
          1. 选择包含生成后文章的文件夹（仅支持 .doc / .docx 格式）<br>
          2. 系统会根据文件名自动匹配标题库中的记录<br>
          3. 匹配成功后会自动创建文章并关联到对应的推荐记录
        </div>
      </div>
      <Form.Item label="选择文件夹" required>
        <input
          type="file"
          webkitdirectory
          directory
          multiple
          @change="onImportArticleFileChange"
          style="display: block; margin-bottom: 8px;"
        >
        <div v-if="importArticleFiles.length > 0" style="font-size: 12px; color: #666;">
          已选择 {{ importArticleFiles.length }} 个文件
        </div>
      </Form.Item>
      <div v-if="importArticleResult" style="margin-bottom: 16px;">
        <div style="background: #f0f0f0; border-radius: 8px; padding: 12px 16px;">
          <div style="font-size: 13px; color: #333; margin-bottom: 8px;">
            <strong>导入结果</strong>：成功 {{ importArticleResult.success }} 条，跳过 {{ importArticleResult.skip }} 条
          </div>
          <div v-if="importArticleResult.details && importArticleResult.details.length > 0">
            <div style="font-size: 12px; color: #389e0d; margin-bottom: 4px;">成功明细：</div>
            <div v-for="(item, idx) in importArticleResult.details" :key="idx"
                 style="font-size: 12px; color: #666; padding: 2px 0; border-bottom: 1px dashed #e8e8e8;">
              {{ item.file }} → {{ item.title }}（{{ item.user }}）
            </div>
          </div>
          <div v-if="importArticleResult.errors && importArticleResult.errors.length > 0" style="margin-top: 8px;">
            <div style="font-size: 12px; color: #cf1322; margin-bottom: 4px;">失败原因：</div>
            <div v-for="(item, idx) in importArticleResult.errors" :key="idx"
                 style="font-size: 12px; color: #666; padding: 2px 0; border-bottom: 1px dashed #e8e8e8;">
              {{ item.file }}：{{ item.reason }}
            </div>
          </div>
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; gap: 12px;">
        <Button @click="importArticleModalOpen = false">关闭</Button>
        <Button type="primary" :loading="importArticleLoading" @click="handleImportArticles">开始导入</Button>
      </div>
    </Form>
  </Modal>
</template>

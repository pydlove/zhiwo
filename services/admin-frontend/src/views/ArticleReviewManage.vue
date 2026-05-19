<script setup>
import { ref, onMounted, h, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import {
  Card, Tabs, Table, Button, Tag, DatePicker, Spin, Empty, Modal, message
} from 'ant-design-vue'
import { DownloadOutlined, CopyOutlined } from '@ant-design/icons-vue'
import { renderAsync } from 'docx-preview'
import { useDocxHighlight } from '../composables/useDocxHighlight.js'
import { listPendingReview, listReviewHistory, reviewTitle, batchReview, generateImagePost, getImagePosts } from '../api/titleLibrary.js'

const STORAGE_KEY = 'article-review-date'
const router = useRouter()
const { highlightStats, applyHighlight, clearHighlight } = useDocxHighlight()

const activeTab = ref('pending')
const selectedDate = ref(dayjs())
const loading = ref(false)
const pendingData = ref([])
const historyData = ref([])

const previewModalOpen = ref(false)
const previewLoading = ref(false)
const previewRecord = ref(null)
const docxContainerRef = ref(null)

const imagePostLoading = ref(false)
const imagePostUrls = ref([])
const imagePostModalOpen = ref(false)
const currentImagePostTitleId = ref('')
const imagePreviewOpen = ref(false)
const previewImageUrl = ref('')

const selectedRowKeys = ref([])
const selectedRows = ref([])

const rowSelection = {
  onChange: (keys, rows) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  },
}

function getSavedDate() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) return dayjs(saved)
  return dayjs()
}

function saveDate() {
  localStorage.setItem(STORAGE_KEY, selectedDate.value.format('YYYY-MM-DD'))
}

async function loadPending() {
  loading.value = true
  try {
    const date = selectedDate.value.format('YYYY-MM-DD')
    const result = await listPendingReview(date)
    pendingData.value = result || []
  } catch (e) {
    message.error('加载待审核列表失败')
    pendingData.value = []
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  loading.value = true
  try {
    const date = selectedDate.value.format('YYYY-MM-DD')
    const result = await listReviewHistory(date)
    historyData.value = result || []
  } catch (e) {
    message.error('加载审核历史失败')
    historyData.value = []
  } finally {
    loading.value = false
  }
}

function loadData() {
  selectedRowKeys.value = []
  selectedRows.value = []
  if (activeTab.value === 'pending') {
    loadPending()
  } else {
    loadHistory()
  }
}

function onDateChange() {
  saveDate()
  loadData()
}

function onTabChange() {
  loadData()
}

async function handleReview(record, action) {
  try {
    await reviewTitle(record.id, action)
    const actionMap = {
      confirm: '已确认',
      reject: '已打回',
      aiPass: 'AI味已标记通过',
      aiHeavy: 'AI味已标记重',
    }
    message.success(actionMap[action] || '操作成功')
    loadPending()
  } catch (e) {
    message.error('操作失败')
  }
}

async function handlePreviewAction(action) {
  if (!previewRecord.value) return
  try {
    await reviewTitle(previewRecord.value.id, action)
    const actionMap = {
      confirm: '已确认',
      reject: '已打回',
      aiPass: 'AI味已标记通过',
      aiHeavy: 'AI味已标记重',
    }
    message.success(actionMap[action] || '操作成功')
    if (action === 'confirm' || action === 'reject') {
      previewModalOpen.value = false
    }
    loadPending()
  } catch (e) {
    message.error('操作失败')
  }
}

async function handleBatchReview(action) {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择数据')
    return
  }
  try {
    await batchReview(selectedRowKeys.value, action)
    const actionMap = {
      confirm: '批量确认成功',
      reject: '批量打回成功',
      aiPass: '批量标记AI味通过成功',
      aiHeavy: '批量标记AI味重成功',
    }
    message.success(actionMap[action] || '批量操作成功')
    selectedRowKeys.value = []
    selectedRows.value = []
    loadPending()
  } catch (e) {
    message.error('批量操作失败')
  }
}

async function handlePreview(record) {
  previewRecord.value = record
  previewModalOpen.value = true
  previewLoading.value = true
  // 清除之前的高亮统计
  clearHighlight(docxContainerRef.value)
  // 预加载贴图（如果有）
  imagePostUrls.value = []
  if (record.imagePostUrls) {
    try {
      const images = await getImagePosts(record.id)
      imagePostUrls.value = images || []
    } catch (e) { /* ignore preload failure */ }
  }
  const fileUrl = record.generatedFileUrl
  if (!fileUrl) {
    previewLoading.value = false
    return
  }
  try {
    const cacheBustUrl = fileUrl + (fileUrl.includes('?') ? '&' : '?') + '_t=' + Date.now()
    const res = await fetch(cacheBustUrl)
    const blob = await res.blob()
    if (blob.size === 0) {
      throw new Error('文件内容为空')
    }
    previewLoading.value = false
    await nextTick()
    if (docxContainerRef.value) {
      docxContainerRef.value.innerHTML = ''
      await nextTick()
      try {
        await renderAsync(blob, docxContainerRef.value, null, {
          className: 'docx-preview',
          inWrapper: false,
        })
        const styleEl = document.createElement('style')
        styleEl.textContent = `
          .docx-preview {
            font-family: 'Microsoft YaHei', '微软雅黑', sans-serif !important;
            zoom: 0.85;
            line-height: 2.0 !important;
          }
          .docx-preview del, .docx-preview s, .docx-preview strike {
            display: none !important;
          }
          .docx-preview [style*="line-through"] {
            display: none !important;
          }
          .docx-preview p[class*="heading-3"], .docx-preview p[class*="heading3"] {
            display: none !important;
          }
        `
        docxContainerRef.value.appendChild(styleEl)
        // 违禁词/敏感词高亮
        const checkResult = previewRecord.value?.bannedWordCheckResult
        if (checkResult) {
          let parsed = checkResult
          if (typeof checkResult === 'string') {
            try { parsed = JSON.parse(checkResult) } catch (e) { parsed = null }
          }
          if (parsed && parsed.matches) {
            applyHighlight(docxContainerRef.value, parsed.matches, parsed.totalChars)
          }
        }
      } catch (renderErr) {
        console.error('docx render error:', renderErr)
        docxContainerRef.value.innerHTML = '<div style="color:#999;text-align:center;padding:40px;">文件解析失败</div>'
      }
    }
  } catch (e) {
    message.error('预览失败: ' + (e.message || '未知错误'))
    previewLoading.value = false
  }
}

function handleDownload(record) {
  const fileUrl = record.generatedFileUrl
  if (!fileUrl) {
    message.warning('暂无文件可下载')
    return
  }
  const link = document.createElement('a')
  link.href = fileUrl + (fileUrl.includes('?') ? '&' : '?') + '_t=' + Date.now() + '&download=1'
  link.download = record.generatedFileName || (record.title + '.docx')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handleCopyPreview() {
  if (!docxContainerRef.value) {
    message.warning('暂无内容可复制')
    return
  }
  const text = docxContainerRef.value.innerText || ''
  if (!text.trim()) {
    message.warning('暂无内容可复制')
    return
  }
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      message.success('已复制全文到剪贴板')
    }).catch(() => {
      fallbackCopyText(text)
    })
  } else {
    fallbackCopyText(text)
  }
}

function fallbackCopyText(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    const success = document.execCommand('copy')
    if (success) {
      message.success('已复制全文到剪贴板')
    } else {
      message.error('复制失败')
    }
  } catch (e) {
    message.error('复制失败')
  }
  document.body.removeChild(textarea)
}

function handleGoToMatch(record) {
  router.push({ path: '/title-match', query: { keyword: record.title } })
}

function handleGoToMatchByDate() {
  router.push({ path: '/title-match', query: { recommendDate: selectedDate.value.format('YYYY-MM-DD') } })
}

async function handleGenerateImagePost(record) {
  if (!record || !record.id) return
  Modal.confirm({
    title: '生成贴图',
    content: '确认生成该文章的贴图？生成后可在邮件推送时一并发送。',
    async onOk() {
      imagePostLoading.value = true
      try {
        const images = await generateImagePost(record.id)
        message.success(`贴图生成成功，共 ${images.length} 张`)
        imagePostUrls.value = images || []
        currentImagePostTitleId.value = record.id
        imagePostModalOpen.value = true
      } catch (e) {
        message.error('贴图生成失败: ' + (e?.response?.data?.msg || e?.message || '未知错误'))
      } finally {
        imagePostLoading.value = false
      }
    },
  })
}

async function openImagePostModal(record) {
  if (!record || !record.id) return
  currentImagePostTitleId.value = record.id
  imagePostUrls.value = []
  imagePostModalOpen.value = true
  imagePostLoading.value = true
  try {
    const images = await getImagePosts(record.id)
    imagePostUrls.value = images || []
  } catch (e) {
    message.error('加载贴图失败')
  } finally {
    imagePostLoading.value = false
  }
}

function handleDownloadImage(url) {
  if (!url) return
  const link = document.createElement('a')
  link.href = url + (url.includes('?') ? '&' : '?') + '_t=' + Date.now() + '&download=1'
  link.download = url.substring(url.lastIndexOf('/') + 1)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handlePreviewImage(url) {
  if (!url) return
  previewImageUrl.value = url
  imagePreviewOpen.value = true
}

function getAiFlavorTag(record) {
  if (record.aiFlavorStatus === 2) {
    return h(Tag, { color: 'error' }, () => 'AI味重')
  }
  if (record.aiFlavorStatus === 1) {
    return h(Tag, { color: 'success' }, () => '已通过')
  }
  return h(Tag, { color: 'default' }, () => '未检测')
}

function getConfirmStatusTag(record) {
  const status = record.confirmStatus
  if (status === 1) {
    return h(Tag, { color: 'success' }, () => '已确认')
  }
  if (status === 2) {
    return h(Tag, { color: 'error' }, () => '已拒绝')
  }
  return h(Tag, { color: 'default' }, () => '未确认')
}

const pendingColumns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true, width: 280,
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; align-items: center; gap: 6px;' }, [
        h('a', {
          style: 'color: #1890ff; cursor: pointer;',
          onClick: () => handlePreview(record),
        }, record.title || '-'),
        record.imagePostUrls ? h(Tag, { color: 'purple', style: 'font-size: 10px; line-height: 14px; padding: 0 4px;' }, () => '贴图') : null,
      ])
    },
  },
  { title: '关联用户', key: 'user', width: 140,
    customRender: ({ record }) => record.recommendUserName || '-' },
  { title: '推荐时间', dataIndex: 'recommendDate', key: 'recommendDate', width: 120 },
  { title: 'AI味', key: 'aiFlavor', width: 100, align: 'center',
    customRender: ({ record }) => getAiFlavorTag(record) },
  { title: '操作', key: 'action', width: 260, align: 'center',
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; gap: 6px; justify-content: center; flex-wrap: wrap;' }, [
        h(Button, { type: 'primary', size: 'small', onClick: () => handleReview(record, 'confirm') }, () => '确认'),
        h(Button, { danger: true, size: 'small', onClick: () => handleReview(record, 'reject') }, () => '打回'),
        h(Button, { type: 'link', size: 'small', onClick: () => handleGoToMatch(record) }, () => '去匹配'),
        h(Button, { type: 'link', size: 'small', onClick: () => openImagePostModal(record) }, () => '贴图'),
      ])
    },
  },
]

const historyColumns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true, width: 280,
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; align-items: center; gap: 6px;' }, [
        h('a', {
          style: 'color: #1890ff; cursor: pointer;',
          onClick: () => handlePreview(record),
        }, record.title || '-'),
        record.imagePostUrls ? h(Tag, { color: 'purple', style: 'font-size: 10px; line-height: 14px; padding: 0 4px;' }, () => '贴图') : null,
      ])
    },
  },
  { title: '关联用户', key: 'user', width: 140,
    customRender: ({ record }) => record.recommendUserName || '-' },
  { title: '推荐时间', dataIndex: 'recommendDate', key: 'recommendDate', width: 120 },
  { title: 'AI味', key: 'aiFlavor', width: 100, align: 'center',
    customRender: ({ record }) => getAiFlavorTag(record) },
  { title: '确认状态', key: 'confirmStatus', width: 100, align: 'center',
    customRender: ({ record }) => getConfirmStatusTag(record) },
  { title: '操作', key: 'action', width: 180, align: 'center',
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; gap: 6px; justify-content: center;' }, [
        h(Button, { type: 'link', size: 'small', onClick: () => handleGoToMatch(record) }, () => '去匹配'),
        h(Button, { type: 'link', size: 'small', onClick: () => openImagePostModal(record) }, () => '贴图'),
      ])
    },
  },
]

onMounted(() => {
  selectedDate.value = getSavedDate()
  loadData()
})
</script>

<template>
  <Card :bordered="false">
    <template #title>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>文章审核管理</span>
        <Button size="small" @click="handleGoToMatchByDate">标题匹配</Button>
      </div>
    </template>
    <!-- 日期选择 -->
    <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
      <span style="font-size: 14px; color: #595959;">推荐日期：</span>
      <DatePicker v-model:value="selectedDate" @change="onDateChange" />
    </div>

    <!-- 批量操作 -->
    <div v-if="activeTab === 'pending' && selectedRowKeys.length > 0" style="margin-bottom: 16px; padding: 8px 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
      <span style="font-size: 14px; color: #096dd9;">已选择 {{ selectedRowKeys.length }} 条</span>
      <Button type="primary" size="small" @click="handleBatchReview('confirm')">批量确认</Button>
      <Button danger size="small" @click="handleBatchReview('reject')">批量打回</Button>
    </div>

    <Tabs v-model:activeKey="activeTab" @change="onTabChange">
      <Tabs.TabPane key="pending" tab="待审核">
        <Spin :spinning="loading">
          <Table
            :columns="pendingColumns"
            :data-source="pendingData"
            row-key="id"
            :row-selection="rowSelection"
            :scroll="{ x: 'max-content' }"
            :pagination="false"
          >
            <template #emptyText>
              <Empty description="该日期下暂无待审核数据" />
            </template>
          </Table>
        </Spin>
      </Tabs.TabPane>

      <Tabs.TabPane key="history" tab="审核历史">
        <Spin :spinning="loading">
          <Table
            :columns="historyColumns"
            :data-source="historyData"
            row-key="id"
            :scroll="{ x: 'max-content' }"
            :pagination="false"
          >
            <template #emptyText>
              <Empty description="该日期下暂无审核历史" />
            </template>
          </Table>
        </Spin>
      </Tabs.TabPane>
    </Tabs>
  </Card>

  <!-- 预览弹窗 -->
  <Modal v-model:open="previewModalOpen" :title="previewRecord?.recommendUserName ? `文章预览（${previewRecord.recommendUserName}）` : '文章预览'" :footer="null" :mask-closable="true" width="700">
    <div style="max-height: 70vh; overflow-y: auto;">
      <div v-if="!previewLoading && previewRecord" style="position: sticky; top: 0; background: #fff; z-index: 10; display: flex; gap: 12px; margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; flex-wrap: wrap;">
        <Button size="small" @click="handleDownload(previewRecord)">
          <DownloadOutlined />
          下载
        </Button>
        <Button size="small" @click="handleCopyPreview">
          <CopyOutlined />
          复制全文
        </Button>
        <Button size="small" :loading="imagePostLoading" @click="handleGenerateImagePost(previewRecord)">生成贴图</Button>
        <Button size="small" @click="openImagePostModal(previewRecord)">查看贴图</Button>
        <div v-if="activeTab === 'pending'" style="display: flex; gap: 8px; margin-left: auto; flex-wrap: wrap;">
          <Button type="primary" size="small" @click="() => handlePreviewAction('confirm')">确认</Button>
          <Button danger size="small" @click="() => handlePreviewAction('reject')">打回</Button>
          <Button size="small" @click="() => handlePreviewAction('aiPass')">AI味通过</Button>
          <Button size="small" @click="() => handlePreviewAction('aiHeavy')">AI味重</Button>
        </div>
      </div>
      <div v-if="previewLoading" style="padding: 24px; text-align: center; color: #999;">正在加载预览...</div>
      <div ref="docxContainerRef" style="padding: 16px; margin: 0 auto; max-width: 640px;"></div>

      <!-- 贴图预览区 -->
      <div v-if="imagePostUrls.length > 0" style="border-top: 1px solid #f0f0f0; padding: 16px;">
        <div style="font-size: 14px; font-weight: 500; color: #262626; margin-bottom: 12px;">文章贴图</div>
        <div style="display: flex; flex-direction: row; gap: 12px; overflow-x: auto;">
          <div v-for="(url, idx) in imagePostUrls" :key="idx" style="border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; flex-shrink: 0; width: 240px;">
            <img :src="url" style="width: 100%; height: 400px; object-fit: cover; display: block; cursor: zoom-in;" @click="handlePreviewImage(url)" />
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 6px 10px; background: #fafafa;">
              <span style="font-size: 11px; color: #999;">第 {{ idx + 1 }} 张</span>
              <Button size="small" style="font-size: 11px; padding: 0 6px;" @click="handleDownloadImage(url)">下载</Button>
            </div>
          </div>
        </div>
      </div>

      <!-- 违禁词统计栏 -->
      <div v-if="highlightStats.totalChars > 0" style="position: sticky; bottom: 0; background: #fff; border-top: 1px solid #f0f0f0; padding: 8px 16px; display: flex; gap: 16px; font-size: 13px; flex-wrap: wrap;">
        <span>全文:{{ highlightStats.totalChars }}字</span>
        <span>极限词:{{ highlightStats.极限词 || 0 }}个</span>
        <span>诱导词:{{ highlightStats.诱导词 || 0 }}个</span>
        <span>敏感词:{{ highlightStats.敏感词 || 0 }}个</span>
        <span>医疗词:{{ highlightStats.医疗词 || 0 }}个</span>
        <span>金融词:{{ highlightStats.金融词 || 0 }}个</span>
        <span>政治敏感:{{ highlightStats.政治敏感 || 0 }}个</span>
        <span>其他:{{ highlightStats.其他 || 0 }}个</span>
      </div>
    </div>
  </Modal>

  <!-- 贴图预览弹窗 -->
  <Modal v-model:open="imagePostModalOpen" title="贴图预览" :footer="null" :mask-closable="true" width="900">
    <div v-if="imagePostLoading" style="padding: 24px; text-align: center;">
      <Spin />
      <div style="margin-top: 8px; color: #999;">正在加载贴图...</div>
    </div>
    <div v-else-if="imagePostUrls.length === 0" style="padding: 24px; text-align: center; color: #999;">
      暂无贴图，点击"生成贴图"按钮创建
    </div>
    <div v-else style="display: flex; flex-direction: row; gap: 16px; padding: 8px; overflow-x: auto;">
      <div v-for="(url, idx) in imagePostUrls" :key="idx" style="border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; flex-shrink: 0; width: 300px;">
        <img :src="url" style="width: 100%; height: 500px; object-fit: cover; display: block; cursor: zoom-in;" @click="handlePreviewImage(url)" />
        <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: #fafafa;">
          <span style="font-size: 12px; color: #999;">第 {{ idx + 1 }} 张</span>
          <Button size="small" @click="handleDownloadImage(url)">下载</Button>
        </div>
      </div>
    </div>
  </Modal>

  <!-- 大图缩放预览 -->
  <Modal v-model:open="imagePreviewOpen" :footer="null" :mask-closable="true" width="auto" style="max-width: 90vw;">
    <div style="display: flex; justify-content: center; align-items: center; padding: 8px;">
      <img :src="previewImageUrl" style="max-width: 100%; max-height: 80vh; border-radius: 8px;" />
    </div>
  </Modal>
</template>

<style scoped>
.docx-preview {
  font-family: 'Microsoft YaHei', '微软雅黑', sans-serif !important;
  zoom: 0.85;
}
.docx-preview del, .docx-preview s, .docx-preview strike {
  display: none !important;
}
.docx-preview [style*="line-through"] {
  display: none !important;
}
.docx-preview p[class*="heading-3"], .docx-preview p[class*="heading3"] {
  display: none !important;
}
</style>

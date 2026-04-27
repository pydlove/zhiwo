<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message } from 'ant-design-vue'
import { listGuides, saveGuide, deleteGuide, batchUpdateRecommended, generateGuides, exportGuides, importGuides } from '../api/guide.js'
import { Switch } from 'ant-design-vue'
import mammoth from 'mammoth'
import JSZip from 'jszip'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const search = ref('')
const categoryFilter = ref(undefined)
const statusFilter = ref(undefined)
const recommendedFilter = ref(undefined)

const rawData = ref([])
const selectedRowKeys = ref([])
const selectedRows = ref([])

const categoryColorMap = {
  '标题技巧': 'blue',
  '开头写法': 'orange',
  '结构模板': 'purple',
  '平台规则': 'green',
  '创作技巧': 'blue',
  '养号技巧': 'orange',
}

const columns = [
  { title: '技巧标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '分类', key: 'category', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '推荐', key: 'isRecommended', width: 80 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 150 },
  { title: '操作', key: 'action', width: 220 },
]

const rowSelection = {
  onChange: (keys, rows) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  },
}

const modalOpen = ref(false)
const modalTitle = ref('新增创作技巧')
const form = ref({
  title: '', category: undefined, description: '', contentType: '平台阅读', sortOrder: 1, status: '已上架', isRecommended: 0, link: '', content: '<p>在这里输入创作技巧内容...</p>'
})
const editingId = ref(null)
const editorRef = ref(null)
const toolbarConfig = { excludeKeys: ['uploadVideo', 'insertVideo', 'uploadImage'] }
const editorConfig = { placeholder: '请输入内容...', MENU_CONF: {} }
editorConfig.MENU_CONF['uploadImage'] = {
  server: '/api/upload',
  fieldName: 'file',
}

function handleCreated(editor) {
  editorRef.value = editor
}

async function loadData() {
  try {
    const params = {}
    if (recommendedFilter.value !== undefined) {
      params.isRecommended = recommendedFilter.value
    }
    const list = await listGuides(params)
    rawData.value = list.map(g => ({
      ...g,
      sortOrder: g.sortOrder || 0,
      isRecommended: g.isRecommended || 0,
      updateTime: g.updatedAt ? g.updatedAt.slice(0, 16).replace('T', ' ') : '-',
    }))
    selectedRowKeys.value = []
    selectedRows.value = []
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败')
  }
}

const filteredData = computed(() => {
  let list = rawData.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(g => (g.title || '').includes(keyword))
  }
  if (categoryFilter.value) {
    list = list.filter(g => g.category === categoryFilter.value)
  }
  if (statusFilter.value) {
    list = list.filter(g => g.status === statusFilter.value)
  }
  if (recommendedFilter.value !== undefined) {
    list = list.filter(g => g.isRecommended === recommendedFilter.value)
  }
  return list
})

function handleSearch() {
  // computed already reactive
}

function handleReset() {
  search.value = ''
  categoryFilter.value = undefined
  statusFilter.value = undefined
  recommendedFilter.value = undefined
}

function handleAdd() {
  modalTitle.value = '新增创作技巧'
  editingId.value = null
  form.value = { title: '', category: undefined, description: '', contentType: '平台阅读', sortOrder: 1, status: '已上架', isRecommended: 0, link: '', content: '<p>在这里输入创作技巧内容...</p>' }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑创作技巧'
  editingId.value = record.id
  const isLink = record.link && record.link.trim()
  form.value = {
    title: record.title,
    category: record.category,
    description: record.description || '',
    contentType: isLink ? '外部链接' : '平台阅读',
    sortOrder: record.sortOrder || 1,
    status: record.status || '已上架',
    isRecommended: record.isRecommended || 0,
    link: record.link || '',
    content: record.content || '<p>在这里输入创作技巧内容...</p>',
  }
  modalOpen.value = true
}

function extractDescriptionFromHtml(html) {
  if (!html) return ''
  const text = html.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  return text.slice(0, 50)
}

async function handleSave() {
  if (!form.value.title || !form.value.category) {
    message.warning('请填写必填项')
    return
  }
  if (form.value.contentType === '外部链接' && !form.value.link) {
    message.warning('请填写外部链接')
    return
  }
  let description = form.value.description
  if (!description && form.value.contentType === '平台阅读' && form.value.content) {
    description = extractDescriptionFromHtml(form.value.content)
  }
  try {
    await saveGuide({
      id: editingId.value || undefined,
      title: form.value.title,
      category: form.value.category,
      description: description || undefined,
      content: form.value.contentType === '平台阅读' ? form.value.content : undefined,
      link: form.value.contentType === '外部链接' ? form.value.link : undefined,
      sortOrder: parseInt(form.value.sortOrder, 10) || 0,
      status: form.value.status,
      isRecommended: form.value.isRecommended ? 1 : 0,
    })
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function triggerWordImport() {
  const el = document.getElementById('word-import')
  if (el) el.click()
}

async function convertDocxToHtml(arrayBuffer) {
  const zip = await JSZip.loadAsync(arrayBuffer)

  // 1. Load image relationships
  const relsPath = 'word/_rels/document.xml.rels'
  const relsFile = zip.file(relsPath)
  const imageMap = {}
  if (relsFile) {
    const relsStr = await relsFile.async('string')
    const relsParser = new DOMParser()
    const relsDoc = relsParser.parseFromString(relsStr, 'application/xml')
    const rels = relsDoc.getElementsByTagName('Relationship')
    for (let i = 0; i < rels.length; i++) {
      const r = rels[i]
      const id = r.getAttribute('Id')
      const target = r.getAttribute('Target')
      const type = r.getAttribute('Type')
      if (type && type.includes('image') && id && target) {
        imageMap[id] = target
      }
    }
  }

  // 2. Helper: extract image as base64
  async function getImageBase64(rId) {
    const target = imageMap[rId]
    if (!target) return null
    const imagePath = target.startsWith('media/') ? `word/${target}` : `word/media/${target}`
    const imgFile = zip.file(imagePath)
    if (!imgFile) return null
    const blob = await imgFile.async('blob')
    return new Promise((resolve) => {
      const reader = new FileReader()
      reader.onloadend = () => resolve(reader.result)
      reader.readAsDataURL(blob)
    })
  }

  // 3. Helper: parse a single paragraph into HTML
  async function parseParagraph(p) {
    let pStyle = ''
    let headingTag = 'p'
    const pPr = p.getElementsByTagName('w:pPr')[0]
    if (pPr) {
      const jc = pPr.getElementsByTagName('w:jc')[0]
      if (jc) {
        const align = jc.getAttribute('w:val')
        if (align) pStyle += `text-align: ${align};`
      }
      const pStyleEl = pPr.getElementsByTagName('w:pStyle')[0]
      if (pStyleEl) {
        const styleVal = pStyleEl.getAttribute('w:val')
        if (styleVal) {
          if (styleVal === 'Heading1' || styleVal === '1') headingTag = 'h2'
          else if (styleVal === 'Heading2' || styleVal === '2') headingTag = 'h3'
          else if (styleVal === 'Heading3' || styleVal === '3') headingTag = 'h3'
        }
      }
    }

    let pHtml = ''
    const children = p.childNodes
    for (let j = 0; j < children.length; j++) {
      const node = children[j]
      if (node.nodeName === 'w:r') {
        const r = node
        const rPr = r.getElementsByTagName('w:rPr')[0]
        let tag = 'span'
        let style = ''

        if (rPr) {
          if (rPr.getElementsByTagName('w:b').length) tag = 'strong'
          if (rPr.getElementsByTagName('w:i').length) {
            tag = tag === 'strong' ? 'strong' : 'em'
            if (tag === 'strong') style += 'font-style: italic;'
          }
          if (rPr.getElementsByTagName('w:u').length) style += 'text-decoration: underline;'
          if (rPr.getElementsByTagName('w:strike').length) style += 'text-decoration: line-through;'

          const color = rPr.getElementsByTagName('w:color')[0]
          if (color) {
            const val = color.getAttribute('w:val')
            if (val) style += `color: #${val};`
          }
        }

        // Check for inline image in this run
        const drawing = r.getElementsByTagName('w:drawing')[0]
        const pict = r.getElementsByTagName('w:pict')[0]
        if (drawing || pict) {
          const imgNode = drawing || pict
          const blips = imgNode.getElementsByTagName('a:blip')
          for (let b = 0; b < blips.length; b++) {
            const embed = blips[b].getAttribute('r:embed')
            if (embed) {
              const dataUrl = await getImageBase64(embed)
              if (dataUrl) {
                pHtml += `<img src="${dataUrl}" style="max-width:100%;height:auto;display:block;margin:12px 0;" />`
              }
            }
          }
          // Also check for v:imagedata (older format)
          const vImages = imgNode.getElementsByTagName('v:imagedata')
          for (let b = 0; b < vImages.length; b++) {
            const embed = vImages[b].getAttribute('r:id')
            if (embed) {
              const dataUrl = await getImageBase64(embed)
              if (dataUrl) {
                pHtml += `<img src="${dataUrl}" style="max-width:100%;height:auto;display:block;margin:12px 0;" />`
              }
            }
          }
        }

        const texts = r.getElementsByTagName('w:t')
        let text = ''
        for (let k = 0; k < texts.length; k++) text += texts[k].textContent
        if (text) {
          const styleAttr = style ? ` style="${style}"` : ''
          pHtml += `<${tag}${styleAttr}>${text}</${tag}>`
        }
      }
    }

    const pStyleAttr = pStyle ? ` style="${pStyle}"` : ''
    return `<${headingTag}${pStyleAttr}>${pHtml}</${headingTag}>`
  }

  // 4. Parse document.xml
  const xmlStr = await zip.file('word/document.xml').async('string')
  const parser = new DOMParser()
  const xmlDoc = parser.parseFromString(xmlStr, 'application/xml')
  const paragraphs = xmlDoc.getElementsByTagName('w:p')
  let html = ''
  for (let i = 0; i < paragraphs.length; i++) {
    html += await parseParagraph(paragraphs[i])
  }
  return html
}

async function handleWordImport(e) {
  const file = e.target.files[0]
  if (!file) return
  if (!file.name.endsWith('.doc') && !file.name.endsWith('.docx')) {
    message.warning('请上传 .doc 或 .docx 格式的 Word 文件')
    e.target.value = ''
    return
  }
  try {
    const arrayBuffer = await file.arrayBuffer()
    let html = ''
    if (file.name.endsWith('.docx')) {
      html = await convertDocxToHtml(arrayBuffer)
    } else {
      const result = await mammoth.convertToHtml({ arrayBuffer })
      html = result.value
    }
    form.value.content = html
    form.value.title = file.name.replace(/\.(docx?)$/i, '')
    if (!form.value.description) {
      form.value.description = extractDescriptionFromHtml(html)
    }
    message.success('Word 导入成功')
  } catch (err) {
    console.error(err)
    message.error('Word 导入失败，请检查文件格式')
  }
  e.target.value = ''
}

async function toggleStatus(record) {
  const newStatus = record.status === '已上架' ? '已下架' : '已上架'
  try {
    await saveGuide({
      id: record.id,
      title: record.title,
      category: record.category,
      description: record.description,
      content: record.content,
      link: record.link,
      sortOrder: record.sortOrder,
      status: newStatus,
      isRecommended: record.isRecommended || 0,
    })
    message.success(newStatus === '已上架' ? '已上架' : '已下架')
    loadData()
  } catch (e) {
    message.error('操作失败')
  }
}

async function batchRecommend(isRecommended) {
  const ids = selectedRowKeys.value
  if (!ids || ids.length === 0) {
    message.warning('请先选择数据')
    return
  }
  try {
    await batchUpdateRecommended(ids, isRecommended)
    message.success(isRecommended === 1 ? '批量推荐成功' : '批量取消推荐成功')
    selectedRowKeys.value = []
    selectedRows.value = []
    loadData()
  } catch (e) {
    message.error('操作失败')
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除该创作技巧？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteGuide(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

const previewOpen = ref(false)
const previewRecord = ref(null)

function openPreview(record) {
  previewRecord.value = record
  previewOpen.value = true
}

function closePreview() {
  previewOpen.value = false
  previewRecord.value = null
}

// AI 生成相关
const generateModalOpen = ref(false)
const generateForm = ref({ category: undefined, count: 1 })
const generateLoading = ref(false)
const generatedPreviewOpen = ref(false)
const generatedGuides = ref([])
const saveGeneratedLoading = ref(false)
const currentPreviewGuide = ref(null)
const currentPreviewOpen = ref(false)

function openGenerateModal() {
  generateForm.value = { category: undefined, count: 1 }
  generateModalOpen.value = true
}

async function handleGenerate() {
  if (!generateForm.value.category) {
    message.warning('请选择内容类型')
    return
  }
  generateLoading.value = true
  try {
    const list = await generateGuides(generateForm.value.category, generateForm.value.count)
    generatedGuides.value = Array.isArray(list) ? list : []
    generateModalOpen.value = false
    generatedPreviewOpen.value = true
    message.success(`成功生成 ${generatedGuides.value.length} 篇创作技巧`)
  } catch (e) {
    console.error('generate error:', e)
    message.error(e.message || '生成失败，请确保服务器已安装 Claude Code CLI（npm install -g @anthropic-ai/claude-code）')
  } finally {
    generateLoading.value = false
  }
}

function openCurrentPreview(guide) {
  currentPreviewGuide.value = guide
  currentPreviewOpen.value = true
}

function closeCurrentPreview() {
  currentPreviewOpen.value = false
  currentPreviewGuide.value = null
}

async function saveGeneratedGuides() {
  if (!generatedGuides.value.length) return
  saveGeneratedLoading.value = true
  try {
    for (const guide of generatedGuides.value) {
      await saveGuide({
        title: guide.title,
        category: guide.category,
        description: guide.description,
        content: guide.content,
        sortOrder: guide.sortOrder || 1,
        status: guide.status || '已上架',
        isRecommended: guide.isRecommended || 0,
      })
    }
    message.success(`成功保存 ${generatedGuides.value.length} 篇创作技巧`)
    generatedPreviewOpen.value = false
    generatedGuides.value = []
    loadData()
  } catch (e) {
    console.error('save generated error:', e)
    message.error('保存失败')
  } finally {
    saveGeneratedLoading.value = false
  }
}

// Export / Import
async function handleExport() {
  try {
    let blob
    if (selectedRowKeys.value.length > 0) {
      blob = await exportGuides(selectedRowKeys.value)
    } else {
      blob = await exportGuides([])
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `创作技巧导出_${new Date().toISOString().slice(0,10).replace(/-/g,'')}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}

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

function downloadImportTemplate() {
  const headers = ['ID', '技巧标题', '分类', '描述', '内容', '链接', '排序', '状态', '是否推荐']
  const csvContent = headers.join(',') + '\n,示例标题,创作技巧,描述内容,HTML内容,,1,已上架,0'
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '创作技巧导入模板.csv'
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
    const result = await importGuides(importExcelFile.value)
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

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="search" placeholder="搜索技巧标题" style="width: 240px;" />
      <Select v-model:value="categoryFilter" placeholder="全部分类" style="min-width: 140px;" allow-clear>
        <Select.Option value="标题技巧">标题技巧</Select.Option>
        <Select.Option value="开头写法">开头写法</Select.Option>
        <Select.Option value="结构模板">结构模板</Select.Option>
        <Select.Option value="平台规则">平台规则</Select.Option>
        <Select.Option value="创作技巧">创作技巧</Select.Option>
        <Select.Option value="养号技巧">养号技巧</Select.Option>
      </Select>
      <Select v-model:value="statusFilter" placeholder="全部状态" style="min-width: 140px;" allow-clear>
        <Select.Option value="已上架">已上架</Select.Option>
        <Select.Option value="已下架">已下架</Select.Option>
      </Select>
      <Select v-model:value="recommendedFilter" placeholder="全部推荐" style="min-width: 140px;" allow-clear>
        <Select.Option :value="1">已推荐</Select.Option>
        <Select.Option :value="0">未推荐</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button @click="handleExport">导出</Button>
      <Button @click="openImportModal">导入</Button>
      <Button type="primary" danger style="margin-left: auto;" @click="openGenerateModal">
        <span style="margin-right: 4px;">✨</span>AI 生成
      </Button>
      <Button type="primary" @click="handleAdd">+ 新增技巧</Button>
    </div>

    <div v-if="selectedRowKeys.length > 0" style="margin-bottom: 16px; padding: 8px 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px; display: flex; align-items: center; gap: 12px;">
      <span style="font-size: 14px;">已选择 {{ selectedRowKeys.length }} 项</span>
      <Button size="small" type="primary" @click="batchRecommend(1)">批量推荐</Button>
      <Button size="small" @click="batchRecommend(0)">批量取消推荐</Button>
    </div>

    <Table :columns="columns" :data-source="filteredData" :pagination="false" row-key="id" :row-selection="rowSelection">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'category'">
          <Tag :color="categoryColorMap[record.category] || 'default'">{{ record.category }}</Tag>
        </template>
        <template v-if="column.key === 'status'">
          <Tag :color="record.status === '已上架' ? 'green' : 'default'">{{ record.status }}</Tag>
        </template>
        <template v-if="column.key === 'isRecommended'">
          <Tag :color="record.isRecommended === 1 ? 'red' : 'default'">{{ record.isRecommended === 1 ? '推荐' : '-' }}</Tag>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a style="margin-right: 12px;" @click="openPreview(record)">预览</a>
          <a style="margin-right: 12px;" :style="{ color: record.status === '已上架' ? '#f5222d' : '#1890ff' }" @click="toggleStatus(record)">
            {{ record.status === '已上架' ? '下架' : '上架' }}
          </a>
          <a style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="800">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="技巧标题" required>
          <Input v-model:value="form.title" placeholder="请输入技巧标题" />
        </Form.Item>
        <Form.Item label="所属分类" required>
          <Select v-model:value="form.category" placeholder="请选择分类">
            <Select.Option value="标题技巧">标题技巧</Select.Option>
            <Select.Option value="开头写法">开头写法</Select.Option>
            <Select.Option value="结构模板">结构模板</Select.Option>
            <Select.Option value="平台规则">平台规则</Select.Option>
            <Select.Option value="创作技巧">创作技巧</Select.Option>
            <Select.Option value="养号技巧">养号技巧</Select.Option>
          </Select>
        </Form.Item>
      </div>
      <Form.Item label="描述">
        <Input.TextArea v-model:value="form.description" :rows="2" placeholder="请输入描述，用于用户端列表展示" />
      </Form.Item>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="内容类型" required>
          <Select v-model:value="form.contentType">
            <Select.Option value="平台阅读">平台阅读</Select.Option>
            <Select.Option value="外部链接">外部链接</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="排序权重">
          <Input type="number" v-model:value="form.sortOrder" placeholder="数字越小越靠前" />
        </Form.Item>
      </div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="状态">
          <Select v-model:value="form.status">
            <Select.Option value="已上架">已上架</Select.Option>
            <Select.Option value="已下架">已下架</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="首页推荐">
          <Switch v-model:checked="form.isRecommended" :checkedValue="1" :unCheckedValue="0" />
        </Form.Item>
      </div>

      <template v-if="form.contentType === '外部链接'">
        <Form.Item label="外部链接" required>
          <Input v-model:value="form.link" placeholder="请输入外部文章链接，如：https://mp.weixin.qq.com/..." />
          <div style="font-size: 12px; color: #999; margin-top: 4px;">用户端点击该技巧时将在新窗口打开此链接</div>
        </Form.Item>
      </template>

      <template v-if="form.contentType === '平台阅读'">
        <Form.Item label="技巧内容" required>
          <div style="border: 1px solid #e5e7eb; z-index: 100;">
            <Toolbar style="border-bottom: 1px solid #e5e7eb;" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
            <Editor v-model="form.content" style="height: 300px; overflow-y: hidden;" :defaultConfig="editorConfig" mode="default" @onCreated="handleCreated" />
          </div>
          <div style="font-size: 12px; color: #999; margin-top: 4px;">支持 HTML 标签，前端用户在创作中心直接阅读</div>
          <div style="margin-top: 8px;">
            <input id="word-import" type="file" accept=".doc,.docx" style="display: none;" @change="handleWordImport">
            <Button size="small" @click="triggerWordImport">导入 Word</Button>
          </div>
        </Form.Item>
      </template>
    </Form>
  </Modal>

  <Modal v-model:open="previewOpen" title="技巧预览" :footer="null" :width="800" @cancel="closePreview">
    <div v-if="previewRecord" style="padding: 8px 0;">
      <div style="font-size: 18px; font-weight: 600; margin-bottom: 12px;">{{ previewRecord.title }}</div>
      <Tag :color="categoryColorMap[previewRecord.category] || 'default'" style="margin-bottom: 12px;">{{ previewRecord.category }}</Tag>
      <div v-if="previewRecord.link" style="margin-bottom: 12px;">
        <a :href="previewRecord.link" target="_blank" style="color: #2563eb;">打开外部链接 ↗</a>
      </div>
      <div v-else class="guide-preview-content" style="font-size: 14px; line-height: 1.8; color: #374151; overflow-wrap: break-word;" v-html="previewRecord.content"></div>
    </div>
  </Modal>

  <!-- AI 生成配置弹窗 -->
  <Modal v-model:open="generateModalOpen" title="AI 生成创作技巧" :mask-closable="false" :width="480" @ok="handleGenerate">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="内容类型" required>
        <Select v-model:value="generateForm.category" placeholder="请选择要生成的内容类型">
          <Select.Option value="标题技巧">标题技巧</Select.Option>
          <Select.Option value="开头写法">开头写法</Select.Option>
          <Select.Option value="结构模板">结构模板</Select.Option>
          <Select.Option value="平台规则">平台规则</Select.Option>
          <Select.Option value="创作技巧">创作技巧</Select.Option>
          <Select.Option value="养号技巧">养号技巧</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="生成数量">
        <Select v-model:value="generateForm.count" placeholder="生成数量">
          <Select.Option :value="1">1 篇</Select.Option>
          <Select.Option :value="2">2 篇</Select.Option>
          <Select.Option :value="3">3 篇</Select.Option>
          <Select.Option :value="4">4 篇</Select.Option>
          <Select.Option :value="5">5 篇</Select.Option>
        </Select>
      </Form.Item>
      <div style="padding: 12px; background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 4px; font-size: 13px; color: #52c41a;">
        <div style="font-weight: 600; margin-bottom: 4px;">生成说明</div>
        <div>· 调用 Claude AI 生成高质量创作技巧</div>
        <div>· 自动匹配「中古风」排版样式</div>
        <div>· 自动下载 16:9 配图插入内容</div>
        <div>· 生成时间约 10-30 秒/篇</div>
      </div>
    </Form>
    <template #footer>
      <Button @click="generateModalOpen = false">取消</Button>
      <Button type="primary" :loading="generateLoading" @click="handleGenerate">开始生成</Button>
    </template>
  </Modal>

  <!-- 生成结果预览弹窗 -->
  <Modal v-model:open="generatedPreviewOpen" title="生成结果预览" :mask-closable="false" :width="860" :footer="null">
    <div style="margin-top: 12px; max-height: 70vh; overflow-y: auto;">
      <div v-for="(guide, idx) in generatedGuides" :key="idx" style="margin-bottom: 20px; border: 1px solid #e8e8e8; border-radius: 4px; overflow: hidden;">
        <div style="padding: 12px 16px; background: #fafafa; border-bottom: 1px solid #e8e8e8; display: flex; align-items: center; justify-content: space-between;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <Tag :color="categoryColorMap[guide.category] || 'default'">{{ guide.category }}</Tag>
            <span style="font-weight: 600; color: #262626;">{{ guide.title }}</span>
          </div>
          <Button size="small" @click="openCurrentPreview(guide)">预览内容</Button>
        </div>
        <div style="padding: 12px 16px; color: #595959; font-size: 13px;">
          {{ guide.description }}
        </div>
      </div>
      <div v-if="!generatedGuides.length" style="text-align: center; color: #999; padding: 40px;">暂无生成内容</div>
    </div>
    <div style="margin-top: 16px; padding-top: 16px; border-top: 1px solid #e8e8e8; display: flex; justify-content: flex-end; gap: 12px;">
      <Button @click="generatedPreviewOpen = false">关闭</Button>
      <Button type="primary" :loading="saveGeneratedLoading" @click="saveGeneratedGuides">
        全部保存（{{ generatedGuides.length }} 篇）
      </Button>
    </div>
  </Modal>

  <!-- 单篇生成内容预览 -->
  <Modal v-model:open="currentPreviewOpen" :title="currentPreviewGuide?.title" :footer="null" :width="800" @cancel="closeCurrentPreview">
    <div v-if="currentPreviewGuide" style="padding: 8px 0;">
      <Tag :color="categoryColorMap[currentPreviewGuide.category] || 'default'" style="margin-bottom: 12px;">{{ currentPreviewGuide.category }}</Tag>
      <div class="guide-preview-content" style="font-size: 14px; line-height: 1.8; color: #374151; overflow-wrap: break-word;" v-html="currentPreviewGuide.content"></div>
    </div>
  </Modal>

  <!-- 导入 Excel 弹窗 -->
  <Modal v-model:open="importModalOpen" title="导入创作技巧" :mask-closable="false" :width="480" @ok="handleImport">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="选择 Excel 文件" required>
        <input type="file" accept=".xlsx,.xls,.csv" @change="onImportFileChange" style="display: block; margin-bottom: 8px;">
        <div style="font-size: 12px; color: #999;">支持 .xlsx / .xls / .csv 格式，第一行为表头</div>
      </Form.Item>
      <div style="padding: 12px; background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 4px; font-size: 13px; color: #52c41a;">
        <div style="font-weight: 600; margin-bottom: 4px;">导入规则</div>
        <div>· ID 列有值且存在则更新，否则新增</div>
        <div>· 必填列：技巧标题、分类</div>
        <div>· 状态默认值：已上架；推荐默认值：0</div>
      </div>
    </Form>
    <template #footer>
      <Button @click="importModalOpen = false">取消</Button>
      <Button @click="downloadImportTemplate">下载模板</Button>
      <Button type="primary" :loading="importLoading" @click="handleImport">开始导入</Button>
    </template>
  </Modal>
</template>

<style>
.guide-preview-content img,
.guide-edit-preview img {
  max-width: 100%;
  height: auto;
  display: block;
}
</style>

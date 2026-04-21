<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message } from 'ant-design-vue'
import { listGuides, saveGuide, deleteGuide } from '../api/guide.js'
import mammoth from 'mammoth'
import JSZip from 'jszip'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const search = ref('')
const categoryFilter = ref(undefined)
const statusFilter = ref(undefined)

const rawData = ref([])

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
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 150 },
  { title: '操作', key: 'action', width: 220 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增创作技巧')
const form = ref({
  title: '', category: undefined, description: '', contentType: '平台阅读', sortOrder: 1, status: '已上架', link: '', content: '<p>在这里输入创作技巧内容...</p>'
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
    const list = await listGuides()
    rawData.value = list.map(g => ({
      ...g,
      sortOrder: g.sortOrder || 0,
      updateTime: g.updatedAt ? g.updatedAt.slice(0, 16).replace('T', ' ') : '-',
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
    list = list.filter(g => (g.title || '').includes(keyword))
  }
  if (categoryFilter.value) {
    list = list.filter(g => g.category === categoryFilter.value)
  }
  if (statusFilter.value) {
    list = list.filter(g => g.status === statusFilter.value)
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
}

function handleAdd() {
  modalTitle.value = '新增创作技巧'
  editingId.value = null
  form.value = { title: '', category: undefined, description: '', contentType: '平台阅读', sortOrder: 1, status: '已上架', link: '', content: '<p>在这里输入创作技巧内容...</p>' }
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
  const xmlStr = await zip.file('word/document.xml').async('string')
  const parser = new DOMParser()
  const xmlDoc = parser.parseFromString(xmlStr, 'application/xml')
  const paragraphs = xmlDoc.getElementsByTagName('w:p')
  let html = ''

  for (let i = 0; i < paragraphs.length; i++) {
    const p = paragraphs[i]
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
    const runs = p.getElementsByTagName('w:r')
    for (let j = 0; j < runs.length; j++) {
      const r = runs[j]
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

      const texts = r.getElementsByTagName('w:t')
      let text = ''
      for (let k = 0; k < texts.length; k++) text += texts[k].textContent
      if (text) {
        const styleAttr = style ? ` style="${style}"` : ''
        pHtml += `<${tag}${styleAttr}>${text}</${tag}>`
      }
    }

    const pStyleAttr = pStyle ? ` style="${pStyle}"` : ''
    html += `<${headingTag}${pStyleAttr}>${pHtml}</${headingTag}>`
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
    })
    message.success(newStatus === '已上架' ? '已上架' : '已下架')
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
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button type="primary" style="margin-left: auto;" @click="handleAdd">+ 新增技巧</Button>
    </div>

    <Table :columns="columns" :data-source="filteredData" :pagination="false" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'category'">
          <Tag :color="categoryColorMap[record.category] || 'default'">{{ record.category }}</Tag>
        </template>
        <template v-if="column.key === 'status'">
          <Tag :color="record.status === '已上架' ? 'green' : 'default'">{{ record.status }}</Tag>
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
      <Form.Item label="状态">
        <Select v-model:value="form.status" style="max-width: 352px;">
          <Select.Option value="已上架">已上架</Select.Option>
          <Select.Option value="已下架">已下架</Select.Option>
        </Select>
      </Form.Item>

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
</template>

<style>
.guide-preview-content img,
.guide-edit-preview img {
  max-width: 100%;
  height: auto;
  display: block;
}
</style>

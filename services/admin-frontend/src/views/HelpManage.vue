<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message } from 'ant-design-vue'
import { listHelps, saveHelp, deleteHelp } from '../api/help.js'
import { listHelpCategories, saveHelpCategory, deleteHelpCategory } from '../api/helpCategory.js'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import mammoth from 'mammoth'
import JSZip from 'jszip'

const search = ref('')
const categoryFilter = ref(undefined)
const statusFilter = ref(undefined)

const rawData = ref([])
const categories = ref([])

const categoryColorMap = computed(() => {
  const map = {}
  categories.value.forEach(c => {
    map[c.name] = c.color || 'default'
  })
  return map
})

const columns = [
  { title: '文档标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '分类', key: 'category', width: 120 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 150 },
  { title: '操作', key: 'action', width: 220 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增帮助文档')
const form = ref({
  title: '', category: undefined, sortOrder: 1, status: '已上架', content: '<p>在这里输入帮助文档内容...</p>'
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
    const list = await listHelps()
    rawData.value = list.map(h => ({
      ...h,
      sortOrder: h.sortOrder || 0,
      updateTime: h.updatedAt ? h.updatedAt.slice(0, 16).replace('T', ' ') : '-',
    }))
  } catch (e) {
    console.error('loadData error:', e)
    message.error('加载失败')
  }
}

async function loadCategories() {
  try {
    const list = await listHelpCategories()
    categories.value = list.map(c => ({
      ...c,
      sortOrder: c.sortOrder || 0,
    }))
  } catch (e) {
    console.error('loadCategories error:', e)
  }
}

const filteredData = computed(() => {
  let list = rawData.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(h => (h.title || '').includes(keyword))
  }
  if (categoryFilter.value) {
    list = list.filter(h => h.category === categoryFilter.value)
  }
  if (statusFilter.value) {
    list = list.filter(h => h.status === statusFilter.value)
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
  modalTitle.value = '新增帮助文档'
  editingId.value = null
  form.value = { title: '', category: undefined, sortOrder: 1, status: '已上架', content: '<p>在这里输入帮助文档内容...</p>' }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑帮助文档'
  editingId.value = record.id
  form.value = {
    title: record.title,
    category: record.category,
    sortOrder: record.sortOrder || 1,
    status: record.status || '已上架',
    content: record.content || '<p>在这里输入帮助文档内容...</p>',
  }
  modalOpen.value = true
}

function triggerWordImport() {
  const el = document.getElementById('help-word-import')
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
    message.success('Word 导入成功')
  } catch (err) {
    console.error(err)
    message.error('Word 导入失败，请检查文件格式')
  }
  e.target.value = ''
}

async function handleSave() {
  if (!form.value.title || !form.value.category) {
    message.warning('请填写必填项')
    return
  }
  try {
    await saveHelp({
      id: editingId.value || undefined,
      title: form.value.title,
      category: form.value.category,
      content: form.value.content,
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
    await saveHelp({
      id: record.id,
      title: record.title,
      category: record.category,
      content: record.content,
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
    title: '确认删除该帮助文档？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteHelp(record.id)
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

// Category management
const categoryModalOpen = ref(false)
const categoryForm = ref({ name: '', color: 'blue', sortOrder: 1 })
const categoryEditingId = ref(null)
const categoryColors = ['blue', 'orange', 'purple', 'cyan', 'green', 'red', 'magenta', 'gold', 'lime', 'geekblue']

function openCategoryModal() {
  categoryModalOpen.value = true
  categoryEditingId.value = null
  categoryForm.value = { name: '', color: 'blue', sortOrder: 1 }
}

function editCategory(record) {
  categoryEditingId.value = record.id
  categoryForm.value = { name: record.name, color: record.color || 'blue', sortOrder: record.sortOrder || 1 }
}

async function saveCategory() {
  if (!categoryForm.value.name) {
    message.warning('请输入分类名称')
    return
  }
  try {
    await saveHelpCategory({
      id: categoryEditingId.value || undefined,
      name: categoryForm.value.name,
      color: categoryForm.value.color,
      sortOrder: parseInt(categoryForm.value.sortOrder, 10) || 0,
    })
    message.success(categoryEditingId.value ? '修改成功' : '新增成功')
    categoryEditingId.value = null
    categoryForm.value = { name: '', color: 'blue', sortOrder: 1 }
    loadCategories()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDeleteCategory(record) {
  Modal.confirm({
    title: '确认删除该分类？',
    content: `删除后，属于「${record.name}」的帮助文档将不显示分类颜色。`,
    async onOk() {
      try {
        await deleteHelpCategory(record.id)
        message.success('删除成功')
        loadCategories()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="search" placeholder="搜索文档标题" style="width: 240px;" />
      <Select v-model:value="categoryFilter" placeholder="全部分类" style="min-width: 140px;" allow-clear>
        <Select.Option v-for="c in categories" :key="c.id" :value="c.name">{{ c.name }}</Select.Option>
      </Select>
      <Select v-model:value="statusFilter" placeholder="全部状态" style="min-width: 140px;" allow-clear>
        <Select.Option value="已上架">已上架</Select.Option>
        <Select.Option value="已下架">已下架</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button style="margin-left: auto;" @click="openCategoryModal">管理分类</Button>
      <Button type="primary" @click="handleAdd">+ 新增文档</Button>
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
        <Form.Item label="文档标题" required>
          <Input v-model:value="form.title" placeholder="请输入文档标题" />
        </Form.Item>
        <Form.Item label="所属分类" required>
          <Select v-model:value="form.category" placeholder="请选择分类">
            <Select.Option v-for="c in categories" :key="c.id" :value="c.name">{{ c.name }}</Select.Option>
          </Select>
        </Form.Item>
      </div>
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
      <Form.Item label="文档内容" required>
        <div style="border: 1px solid #e5e7eb; z-index: 100;">
          <Toolbar style="border-bottom: 1px solid #e5e7eb;" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
          <Editor v-model="form.content" style="height: 300px; overflow-y: hidden;" :defaultConfig="editorConfig" mode="default" @onCreated="handleCreated" />
        </div>
        <div style="font-size: 12px; color: #999; margin-top: 4px;">支持 HTML 标签，前端用户在帮助中心直接阅读</div>
        <div style="margin-top: 8px;">
          <input id="help-word-import" type="file" accept=".doc,.docx" style="display: none;" @change="handleWordImport">
          <Button size="small" @click="triggerWordImport">导入 Word</Button>
        </div>
      </Form.Item>
    </Form>
  </Modal>

  <Modal v-model:open="previewOpen" title="文档预览" :footer="null" :width="800" @cancel="closePreview">
    <div v-if="previewRecord" style="padding: 8px 0;">
      <div style="font-size: 20px; font-weight: 600; margin-bottom: 12px;">{{ previewRecord.title }}</div>
      <Tag :color="categoryColorMap[previewRecord.category] || 'default'" style="margin-bottom: 16px;">{{ previewRecord.category }}</Tag>
      <div class="help-preview-content" style="font-size: 15px; line-height: 1.8; color: #374151; overflow-wrap: break-word;" v-html="previewRecord.content"></div>
    </div>
  </Modal>

  <!-- Category Management Modal -->
  <Modal v-model:open="categoryModalOpen" title="管理帮助分类" :mask-closable="false" :footer="null" :width="560">
    <div style="margin-top: 12px;">
      <div style="display: flex; gap: 12px; margin-bottom: 20px; align-items: flex-end;">
        <div style="flex: 1;">
          <div style="font-size: 12px; color: #999; margin-bottom: 4px;">分类名称</div>
          <Input v-model:value="categoryForm.name" placeholder="输入分类名称" />
        </div>
        <div style="width: 120px;">
          <div style="font-size: 12px; color: #999; margin-bottom: 4px;">标签颜色</div>
          <Select v-model:value="categoryForm.color" placeholder="颜色">
            <Select.Option v-for="color in categoryColors" :key="color" :value="color">
              <Tag :color="color" size="small">{{ color }}</Tag>
            </Select.Option>
          </Select>
        </div>
        <div style="width: 80px;">
          <div style="font-size: 12px; color: #999; margin-bottom: 4px;">排序</div>
          <Input type="number" v-model:value="categoryForm.sortOrder" min="0" />
        </div>
        <Button type="primary" @click="saveCategory">{{ categoryEditingId ? '保存' : '新增' }}</Button>
        <Button v-if="categoryEditingId" @click="categoryEditingId = null; categoryForm = { name: '', color: 'blue', sortOrder: 1 }">取消</Button>
      </div>

      <Table :columns="[
        { title: '分类名称', dataIndex: 'name', key: 'name', width: 160 },
        { title: '颜色', key: 'color', width: 100 },
        { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
        { title: '操作', key: 'action', width: 120 },
      ]" :data-source="categories" :pagination="false" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'color'">
            <Tag :color="record.color || 'default'" size="small">{{ record.color || 'default' }}</Tag>
          </template>
          <template v-if="column.key === 'action'">
            <a style="margin-right: 12px;" @click="editCategory(record)">编辑</a>
            <a style="color: #f5222d;" @click="handleDeleteCategory(record)">删除</a>
          </template>
        </template>
      </Table>
      <div v-if="!categories.length" style="text-align: center; color: #999; padding: 24px;">暂无分类，请添加</div>
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

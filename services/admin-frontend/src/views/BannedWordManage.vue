<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { Card, Table, Button, Tag, Modal, Form, Input, Select, message } from 'ant-design-vue'
import { listBannedWords, saveBannedWord, deleteBannedWord, exportBannedWords, importBannedWords } from '../api/bannedWord.js'

const data = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const editingId = ref(null)
const form = ref({ word: '', replacement: '', category: '极限词', severity: 'block' })
const filterCategory = ref('')
const importInputRef = ref(null)

const categoryOptions = ['极限词', '医疗词', '金融词', '诱导词', '政治敏感', '敏感词', '其他']
const severityOptions = [
  { value: 'block', label: '严禁' },
  { value: 'caution', label: '慎用' },
]

const filteredData = computed(() => {
  if (!filterCategory.value) return data.value
  return data.value.filter(item => item.category === filterCategory.value)
})

async function loadData() {
  loading.value = true
  try {
    const result = await listBannedWords()
    data.value = result || []
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openModal(record) {
  if (record) {
    editingId.value = record.id
    form.value = {
      word: record.word || '',
      replacement: record.replacement || '',
      category: record.category || '极限词',
      severity: record.severity || 'block',
    }
  } else {
    editingId.value = null
    form.value = { word: '', replacement: '', category: '极限词', severity: 'block' }
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.word || !form.value.word.trim()) {
    message.warning('请输入违禁词')
    return
  }
  try {
    const payload = { ...form.value, word: form.value.word.trim() }
    if (editingId.value) {
      payload.id = editingId.value
    }
    await saveBannedWord(payload)
    message.success(editingId.value ? '修改成功' : '添加成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除违禁词 "${record.word}" 吗？`,
    onOk: async () => {
      try {
        await deleteBannedWord(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

async function handleExport() {
  try {
    const blob = await exportBannedWords()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '违禁词库_' + new Date().toISOString().slice(0, 10) + '.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}

function handleImportClick() {
  importInputRef.value?.click()
}

async function handleImportChange(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    const result = await importBannedWords(file)
    message.success(`导入成功：${result.success || 0} 条，跳过 ${result.skip || 0} 条`)
    loadData()
  } catch (err) {
    message.error(err?.response?.data?.msg || '导入失败')
  }
  e.target.value = ''
}

const columns = [
  {
    title: '违禁词',
    dataIndex: 'word',
    width: 150,
  },
  {
    title: '替换词',
    dataIndex: 'replacement',
    width: 180,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '分类',
    dataIndex: 'category',
    width: 120,
  },
  {
    title: '等级',
    dataIndex: 'severity',
    width: 100,
    align: 'center',
    customRender: ({ text }) => {
      const isBlock = text === 'block'
      return h(Tag, { color: isBlock ? 'red' : 'orange' }, () => isBlock ? '严禁' : '慎用')
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    align: 'center',
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; gap: 8px; justify-content: center;' }, [
        h(Button, { type: 'link', size: 'small', onClick: () => openModal(record) }, () => '编辑'),
        h(Button, { type: 'link', size: 'small', danger: true, onClick: () => handleDelete(record) }, () => '删除'),
      ])
    },
  },
]

onMounted(loadData)
</script>

<template>
  <Card title="违禁词管理" :bordered="false">
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: flex-end;">
      <Select v-model:value="filterCategory" placeholder="筛选分类" style="width: 160px;" allowClear>
        <Select.Option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</Select.Option>
      </Select>
      <Button type="primary" @click="() => openModal(null)">新增违禁词</Button>
      <Button @click="handleExport">导出</Button>
      <Button @click="handleImportClick">导入</Button>
      <input ref="importInputRef" type="file" accept=".xlsx" style="display: none;" @change="handleImportChange" />
    </div>

    <Table
      :columns="columns"
      :data-source="filteredData"
      row-key="id"
      :loading="loading"
      :pagination="false"
      size="small"
    />
  </Card>

  <Modal
    v-model:open="modalOpen"
    :title="editingId ? '编辑违禁词' : '新增违禁词'"
    @ok="handleSave"
  >
    <Form layout="vertical" :model="form">
      <Form.Item label="违禁词" required>
        <Input v-model:value="form.word" placeholder="请输入违禁词" />
      </Form.Item>
      <Form.Item label="替换词">
        <Input v-model:value="form.replacement" placeholder="请输入替换词，为空则直接避免" />
      </Form.Item>
      <Form.Item label="分类">
        <Select v-model:value="form.category">
          <Select.Option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item label="等级">
        <Select v-model:value="form.severity">
          <Select.Option v-for="s in severityOptions" :key="s.value" :value="s.value">{{ s.label }}</Select.Option>
        </Select>
      </Form.Item>
    </Form>
  </Modal>
</template>

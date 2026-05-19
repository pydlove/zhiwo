<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { Card, Table, Button, Tag, Modal, Form, Input, Select, Switch, message } from 'ant-design-vue'
import { listWritingStyles, saveWritingStyle, deleteWritingStyle, listWritingStyleCategories } from '../api/writingStyle.js'

const data = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const editingId = ref(null)
const form = ref({ originalWord: '', styleWord: '', category: '通用', isActive: 1 })
const filterCategory = ref('')
const categories = ref([])

const filteredData = computed(() => {
  if (!filterCategory.value) return data.value
  return data.value.filter(item => item.category === filterCategory.value)
})

async function loadData() {
  loading.value = true
  try {
    const [listResult, catResult] = await Promise.all([
      listWritingStyles(),
      listWritingStyleCategories(),
    ])
    data.value = listResult || []
    categories.value = catResult || []
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
      originalWord: record.originalWord || '',
      styleWord: record.styleWord || '',
      category: record.category || '通用',
      isActive: record.isActive != null ? record.isActive : 1,
    }
  } else {
    editingId.value = null
    form.value = { originalWord: '', styleWord: '', category: '通用', isActive: 1 }
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.originalWord || !form.value.originalWord.trim()) {
    message.warning('请输入原词')
    return
  }
  if (!form.value.styleWord || !form.value.styleWord.trim()) {
    message.warning('请输入风格替换词')
    return
  }
  try {
    const payload = {
      originalWord: form.value.originalWord.trim(),
      styleWord: form.value.styleWord.trim(),
      category: form.value.category || '通用',
      isActive: form.value.isActive,
    }
    if (editingId.value) {
      payload.id = editingId.value
    }
    await saveWritingStyle(payload)
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
    content: `确定删除 "${record.originalWord}" → "${record.styleWord}" 吗？`,
    onOk: async () => {
      try {
        await deleteWritingStyle(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

const columns = [
  {
    title: '原词',
    dataIndex: 'originalWord',
    width: 180,
  },
  {
    title: '风格替换词',
    dataIndex: 'styleWord',
    width: 180,
  },
  {
    title: '分类',
    dataIndex: 'category',
    width: 120,
  },
  {
    title: '启用状态',
    dataIndex: 'isActive',
    width: 100,
    align: 'center',
    customRender: ({ text }) => {
      return h(Tag, { color: text === 1 ? 'green' : 'default' }, () => text === 1 ? '已启用' : '已禁用')
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
  <Card title="写作风格库" :bordered="false">
    <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: flex-end;">
      <Select
        show-search
        v-model:value="filterCategory"
        placeholder="筛选分类"
        style="width: 160px;"
        allowClear
      >
        <Select.Option v-for="c in categories" :key="c" :value="c" :label="c">{{ c }}</Select.Option>
      </Select>
      <Button type="primary" @click="() => openModal(null)">新增风格词</Button>
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
    :title="editingId ? '编辑风格词' : '新增风格词'"
    @ok="handleSave"
  >
    <Form layout="vertical" :model="form">
      <Form.Item label="原词" required>
        <Input v-model:value="form.originalWord" placeholder="请输入原词" />
      </Form.Item>
      <Form.Item label="风格替换词" required>
        <Input v-model:value="form.styleWord" placeholder="请输入风格替换词" />
      </Form.Item>
      <Form.Item label="分类">
        <Select
          show-search
          v-model:value="form.category"
          placeholder="请选择或输入分类"
          allowClear
          :options="categories.map(c => ({ value: c, label: c }))"
        >
        </Select>
      </Form.Item>
      <Form.Item label="启用状态">
        <Switch v-model:checked="form.isActive" :checkedValue="1" :unCheckedValue="0" />
      </Form.Item>
    </Form>
  </Modal>
</template>

<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { Table, Button, Input, Select, Tag, Modal, Form, Switch, message, Popconfirm } from 'ant-design-vue'
import { listPromptTemplates, savePromptTemplate, deletePromptTemplate } from '../api/promptTemplate.js'

const templates = ref([])
const loading = ref(false)
const keyword = ref('')
const typeFilter = ref('')

const typeOptions = [
  { label: '全部类别', value: '' },
  { label: '生成标题', value: 'generate_title' },
  { label: '标题风格', value: 'title_style' },
]

const modalOpen = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = ref({
  id: '',
  name: '',
  content: '',
  type: 'generate_title',
  isDefault: 0,
})

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  content: [{ required: true, message: '请输入提示词内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类别', trigger: 'change' }],
}

const filteredList = computed(() => {
  if (!keyword.value.trim()) return templates.value
  const kw = keyword.value.trim().toLowerCase()
  return templates.value.filter(t =>
    (t.name && t.name.toLowerCase().includes(kw)) ||
    (t.content && t.content.toLowerCase().includes(kw))
  )
})

function fetchList() {
  loading.value = true
  const params = {}
  if (typeFilter.value) params.type = typeFilter.value
  listPromptTemplates(params)
    .then(res => {
      templates.value = res || []
    })
    .catch(() => {
      message.error('加载提示词模板失败')
    })
    .finally(() => {
      loading.value = false
    })
}

function onSearch() {
  // 前端筛选即可，因为数据量不大
}

function onTypeChange() {
  fetchList()
}

function openCreate() {
  modalTitle.value = '新增提示词'
  form.value = {
    id: '',
    name: '',
    content: '',
    type: 'generate_title',
    isDefault: 0,
  }
  modalOpen.value = true
}

function openEdit(record) {
  modalTitle.value = '编辑提示词'
  form.value = {
    id: record.id,
    name: record.name,
    content: record.content,
    type: record.type || 'generate_title',
    isDefault: record.isDefault === 1 ? 1 : 0,
  }
  modalOpen.value = true
}

async function handleModalOk() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  const payload = {
    id: form.value.id || undefined,
    name: form.value.name,
    content: form.value.content,
    type: form.value.type,
    isDefault: form.value.isDefault,
  }
  try {
    await savePromptTemplate(payload)
    message.success(form.value.id ? '修改成功' : '新增成功')
    modalOpen.value = false
    fetchList()
  } catch (e) {
    message.error(e?.message || '保存失败')
  }
}

async function handleDelete(record) {
  try {
    await deletePromptTemplate(record.id)
    message.success('删除成功')
    fetchList()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

function getTypeLabel(type) {
  const map = { generate_title: '生成标题', generate_post: '生成文章', title_style: '标题风格' }
  return map[type] || type
}

const columns = [
  {
    title: '模板名称',
    dataIndex: 'name',
    key: 'name',
    ellipsis: true,
    width: 200,
  },
  {
    title: '类别',
    dataIndex: 'type',
    key: 'type',
    width: 120,
    customRender: ({ text }) => {
      return h(Tag, { color: 'blue' }, () => getTypeLabel(text))
    },
  },
  {
    title: '默认',
    dataIndex: 'isDefault',
    key: 'isDefault',
    width: 80,
    customRender: ({ text }) => {
      return text === 1 ? h(Tag, { color: 'green' }, () => '是') : h(Tag, { color: 'default' }, () => '否')
    },
  },
  {
    title: '内容预览',
    dataIndex: 'content',
    key: 'content',
    ellipsis: true,
    customRender: ({ text }) => {
      return h('span', { title: text }, text)
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 170,
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    align: 'center',
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; justify-content: center; gap: 8px;' }, [
        h(Button, { type: 'link', size: 'small', onClick: () => openEdit(record) }, () => '编辑'),
        h(Popconfirm, {
          title: '确定删除该提示词吗？',
          onConfirm: () => handleDelete(record),
        }, () => h(Button, { type: 'link', size: 'small', danger: true }, () => '删除')),
      ])
    },
  },
]

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div>
    <div style="display: flex; gap: 12px; margin-bottom: 16px;">
      <Input
        v-model:value="keyword"
        placeholder="搜索模板名称或内容"
        style="width: 260px"
        @pressEnter="onSearch"
      />
      <Select
        v-model:value="typeFilter"
        :options="typeOptions"
        style="width: 140px"
        placeholder="类别筛选"
        @change="onTypeChange"
      />
      <Button type="primary" @click="onSearch">搜索</Button>
      <Button style="margin-left: auto;" type="primary" @click="openCreate">新增提示词</Button>
    </div>

    <Table
      :columns="columns"
      :dataSource="filteredList"
      :loading="loading"
      rowKey="id"
      :pagination="{ pageSize: 20 }"
      size="small"
      :scroll="{ x: 1000 }"
    />

    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      width="700"
      :maskClosable="false"
      @ok="handleModalOk"
    >
      <Form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        style="margin-top: 12px;"
      >
        <Form.Item label="模板名称" name="name">
          <Input v-model:value="form.name" placeholder="请输入模板名称" />
        </Form.Item>
        <Form.Item label="类别" name="type">
          <Select
            v-model:value="form.type"
            :options="typeOptions.filter(o => o.value)"
            placeholder="请选择类别"
          />
        </Form.Item>
        <Form.Item label="设为默认">
          <Switch v-model:checked="form.isDefault" :checkedValue="1" :unCheckedValue="0" />
        </Form.Item>
        <Form.Item label="提示词内容" name="content">
          <Input.TextArea
            v-model:value="form.content"
            :rows="12"
            placeholder="请输入提示词内容"
          />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

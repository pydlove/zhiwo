<script setup>
import { ref, onMounted, computed, h } from 'vue'
import { Table, Button, Input, Modal, Form, Switch, message, Popconfirm } from 'ant-design-vue'
import { listTitleBannedWords, saveTitleBannedWord, deleteTitleBannedWord } from '../api/titleBannedWord.js'

const words = ref([])
const loading = ref(false)
const keyword = ref('')

const modalOpen = ref(false)
const modalTitle = ref('')
const formRef = ref()
const form = ref({
  id: '',
  word: '',
  category: '通用',
  isActive: 1,
})

const rules = {
  word: [{ required: true, message: '请输入禁用词', trigger: 'blur' }],
}

const filteredList = computed(() => {
  if (!keyword.value.trim()) return words.value
  const kw = keyword.value.trim().toLowerCase()
  return words.value.filter(w =>
    (w.word && w.word.toLowerCase().includes(kw)) ||
    (w.category && w.category.toLowerCase().includes(kw))
  )
})

function fetchList() {
  loading.value = true
  listTitleBannedWords()
    .then(res => {
      words.value = res || []
    })
    .catch(() => {
      message.error('加载禁用词失败')
    })
    .finally(() => {
      loading.value = false
    })
}

function openCreate() {
  modalTitle.value = '新增禁用词'
  form.value = {
    id: '',
    word: '',
    category: '通用',
    isActive: 1,
  }
  modalOpen.value = true
}

function openEdit(record) {
  modalTitle.value = '编辑禁用词'
  form.value = {
    id: record.id,
    word: record.word,
    category: record.category || '通用',
    isActive: record.isActive != null ? record.isActive : 1,
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
    word: form.value.word.trim(),
    category: form.value.category.trim(),
    isActive: form.value.isActive,
  }
  try {
    await saveTitleBannedWord(payload)
    message.success(form.value.id ? '修改成功' : '新增成功')
    modalOpen.value = false
    fetchList()
  } catch (e) {
    message.error(e?.message || '保存失败')
  }
}

async function handleDelete(record) {
  try {
    await deleteTitleBannedWord(record.id)
    message.success('删除成功')
    fetchList()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

const columns = [
  {
    title: '禁用词',
    dataIndex: 'word',
    key: 'word',
    width: 200,
  },
  {
    title: '分类',
    dataIndex: 'category',
    key: 'category',
    width: 140,
  },
  {
    title: '状态',
    dataIndex: 'isActive',
    key: 'isActive',
    width: 100,
    align: 'center',
    customRender: ({ text }) => {
      return text === 1 ? '启用' : '禁用'
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
        h(Popconfirm, { title: '确定删除该禁用词吗？', onConfirm: () => handleDelete(record) }, () =>
          h(Button, { type: 'link', size: 'small', danger: true }, () => '删除')
        ),
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
        placeholder="搜索禁用词或分类"
        style="width: 260px"
      />
      <Button type="primary" style="margin-left: auto;" @click="openCreate">新增禁用词</Button>
    </div>

    <Table
      :columns="columns"
      :dataSource="filteredList"
      :loading="loading"
      rowKey="id"
      :pagination="{ pageSize: 20 }"
      size="small"
      :scroll="{ x: 800 }"
    />

    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      width="480"
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
        <Form.Item label="禁用词" name="word">
          <Input v-model:value="form.word" placeholder="请输入禁用词" />
        </Form.Item>
        <Form.Item label="分类">
          <Input v-model:value="form.category" placeholder="例如：通用、政治、广告" />
        </Form.Item>
        <Form.Item label="是否启用">
          <Switch v-model:checked="form.isActive" :checkedValue="1" :unCheckedValue="0" />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

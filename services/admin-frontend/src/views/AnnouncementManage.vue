<script setup>
import { ref, onMounted, h } from 'vue'
import { Card, Button, Table, Tag, Modal, Form, Input, Switch, message, Popconfirm } from 'ant-design-vue'
import { listAnnouncements, saveAnnouncement, deleteAnnouncement } from '../api/announcement.js'

const loading = ref(false)
const tableData = ref([])

const modalOpen = ref(false)
const modalTitle = ref('新增公告')
const form = ref({ id: '', type: 'article_push', content: '', isEnabled: 1 })
const saving = ref(false)

const typeMap = {
  article_push: '文章推送公告',
}

function openAddModal() {
  modalTitle.value = '新增公告'
  form.value = { id: '', type: 'article_push', content: '', isEnabled: 1 }
  modalOpen.value = true
}

function openEditModal(record) {
  modalTitle.value = '编辑公告'
  form.value = { ...record }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.content || !form.value.content.trim()) {
    message.warning('请输入公告内容')
    return
  }
  saving.value = true
  try {
    await saveAnnouncement(form.value)
    message.success(form.value.id ? '更新成功' : '新增成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record) {
  try {
    await deleteAnnouncement(record.id)
    message.success('删除成功')
    loadData()
  } catch (e) {
    message.error(e?.response?.data?.msg || e?.message || '删除失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await listAnnouncements()
    tableData.value = data || []
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const columns = [
  {
    title: '公告类型',
    key: 'type',
    width: 140,
    customRender: ({ record }) => typeMap[record.type] || record.type || '-',
  },
  {
    title: '公告内容',
    key: 'content',
    ellipsis: true,
    customRender: ({ record }) => h('span', { innerHTML: record.content }),
  },
  {
    title: '状态',
    key: 'isEnabled',
    width: 90,
    align: 'center',
    customRender: ({ record }) => {
      return record.isEnabled === 1
        ? h(Tag, { color: 'success' }, () => '开启')
        : h(Tag, { color: 'default' }, () => '关闭')
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    align: 'center',
    customRender: ({ record }) => {
      return h('div', { style: 'display: flex; gap: 8px; justify-content: center;' }, [
        h(Button, { type: 'link', size: 'small', onClick: () => openEditModal(record) }, () => '编辑'),
        h(Popconfirm, {
          title: '确认删除？',
          onConfirm: () => handleDelete(record),
        }, {
          default: () => h(Button, { type: 'link', danger: true, size: 'small' }, () => '删除'),
        }),
      ])
    },
  },
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <div>
    <Card title="公告管理" :bordered="false">
      <div style="margin-bottom: 16px;">
        <Button type="primary" @click="openAddModal">+ 新增公告</Button>
      </div>

      <Table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        row-key="id"
        :pagination="false"
      />
    </Card>

    <Modal
      v-model:open="modalOpen"
      :title="modalTitle"
      :confirm-loading="saving"
      @ok="handleSave"
      :width="600"
    >
      <Form layout="vertical" style="margin-top: 12px;">
        <Form.Item label="公告类型" required>
          <Input v-model:value="form.type" disabled placeholder="article_push" />
        </Form.Item>
        <Form.Item label="公告内容" required>
          <Input.TextArea
            v-model:value="form.content"
            placeholder="支持 HTML 格式，将作为邮件正文中的公告区域展示"
            :rows="6"
            :maxlength="2000"
            show-count
          />
        </Form.Item>
        <Form.Item label="是否开启">
          <Switch v-model:checked="form.isEnabled" :checkedValue="1" :unCheckedValue="0" />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

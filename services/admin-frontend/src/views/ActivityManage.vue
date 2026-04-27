<script setup>
import { ref, onMounted } from 'vue'
import { Table, Button, Modal, Form, Input, Switch, message, Popconfirm, InputNumber } from 'ant-design-vue'
import { listActivities, saveActivity, deleteActivity, sendActivityEmail } from '../api/activity.js'
import { listUsers } from '../api/user.js'

const data = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const modalTitle = ref('新增活动')
const editForm = ref({ title: '', content: '', qrCodeUrl: '', status: 1, sortOrder: 0 })
const formRef = ref(null)

const emailModalOpen = ref(false)
const emailActivity = ref(null)
const allUsers = ref([])
const selectedUserIds = ref([])
const emailSending = ref(false)

const columns = [
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '活动标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '二维码', dataIndex: 'qrCodeUrl', key: 'qrCodeUrl', width: 120,
    customRender: ({ text }) => text ? '有' : '无' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100,
    customRender: ({ text }) => text === 1 ? '已上架' : '已下架' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180,
    customRender: ({ text }) => text ? text.slice(0, 19).replace('T', ' ') : '' },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
]

async function loadData() {
  loading.value = true
  try {
    data.value = await listActivities()
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  modalTitle.value = '新增活动'
  editForm.value = { title: '', content: '', qrCodeUrl: '', status: 1, sortOrder: 0 }
  modalOpen.value = true
}

function openEdit(record) {
  modalTitle.value = '编辑活动'
  editForm.value = { ...record }
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value.validate()
    await saveActivity(editForm.value)
    message.success('保存成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    if (e?.errorFields) return
    message.error('保存失败：' + (e.message || ''))
  }
}

async function handleDelete(id) {
  try {
    await deleteActivity(id)
    message.success('删除成功')
    loadData()
  } catch (e) {
    message.error('删除失败')
  }
}

function openEmailModal(record) {
  emailActivity.value = record
  selectedUserIds.value = []
  emailModalOpen.value = true
  loadUsers()
}

async function loadUsers() {
  try {
    const users = await listUsers()
    allUsers.value = users.filter(u => u.status === 1 && u.email)
  } catch (e) {
    message.error('加载用户失败')
  }
}

async function handleSendEmail() {
  if (!selectedUserIds.value.length) {
    message.warning('请选择要推送的用户')
    return
  }
  emailSending.value = true
  try {
    const res = await sendActivityEmail(emailActivity.value.id, selectedUserIds.value)
    message.success(`推送成功，共发送 ${res.success || 0} 封邮件`)
    emailModalOpen.value = false
  } catch (e) {
    message.error('推送失败：' + (e.message || ''))
  } finally {
    emailSending.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h2 style="margin: 0; font-size: 20px; font-weight: 600;">活动管理</h2>
      <Button type="primary" @click="openAdd">+ 新增活动</Button>
    </div>

    <Table :columns="columns" :data-source="data" :loading="loading" row-key="id" size="middle"
      :pagination="{ pageSize: 10 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <div style="display: flex; gap: 8px;">
            <Button size="small" @click="openEdit(record)">编辑</Button>
            <Button size="small" type="primary" @click="openEmailModal(record)">推送邮件</Button>
            <Popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <Button size="small" danger>删除</Button>
            </Popconfirm>
          </div>
        </template>
      </template>
    </Table>

    <!-- 编辑/新增弹窗 -->
    <Modal v-model:open="modalOpen" :title="modalTitle" width="720" :footer="null">
      <Form ref="formRef" :model="editForm" layout="vertical">
        <Form.Item label="活动标题" name="title" :rules="[{ required: true, message: '请输入活动标题' }]">
          <Input v-model:value="editForm.title" placeholder="请输入活动标题" />
        </Form.Item>
        <Form.Item label="活动内容" name="content">
          <Input.TextArea v-model:value="editForm.content" :rows="6" placeholder="支持 HTML 格式" />
        </Form.Item>
        <Form.Item label="二维码图片 URL" name="qrCodeUrl">
          <Input v-model:value="editForm.qrCodeUrl" placeholder="二维码图片 URL，邮件中会展示" />
        </Form.Item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
          <Form.Item label="排序" name="sortOrder">
            <InputNumber v-model:value="editForm.sortOrder" :min="0" style="width: 100%;" />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Switch v-model:checked="editForm.status" :checked-value="1" :unchecked-value="0"
              checked-children="上架" un-checked-children="下架" />
          </Form.Item>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <Button @click="modalOpen = false">取消</Button>
          <Button type="primary" @click="handleSave">保存</Button>
        </div>
      </Form>
    </Modal>

    <!-- 推送邮件弹窗 -->
    <Modal v-model:open="emailModalOpen" :title="'推送活动：' + (emailActivity?.title || '')" width="560"
      :confirm-loading="emailSending" @ok="handleSendEmail">
      <div style="margin-bottom: 12px; font-size: 13px; color: #666;">
        选择要接收活动邮件的用户（仅展示有邮箱的正常用户）
      </div>
      <a-select v-model:value="selectedUserIds" mode="multiple" style="width: 100%;" placeholder="请选择用户"
        :options="allUsers.map(u => ({ label: u.username + '（' + u.email + '）', value: u.id }))" />
    </Modal>
  </div>
</template>

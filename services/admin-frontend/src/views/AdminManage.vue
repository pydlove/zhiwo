<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Select, Button, Table, Tag, Modal, Form, message, Pagination } from 'ant-design-vue'
import { listAdmins, saveAdmin, deleteAdmin } from '../api/admin.js'

const search = ref('')
const roleFilter = ref(undefined)
const statusFilter = ref(undefined)

const data = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const roleColorMap = {
  '超级管理员': 'purple',
  '内容管理员': 'blue',
  '运营管理员': 'green',
}

const columns = [
  { title: '管理员账号', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '角色', key: 'role', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '最近登录', dataIndex: 'lastLogin', key: 'lastLogin', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120 },
  { title: '操作', key: 'action', width: 180 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增管理员')
const form = ref({ username: '', name: '', role: undefined, password: 'Abc123456', status: 1 })
const editingId = ref(null)

async function loadData() {
  try {
    const list = await listAdmins()
    data.value = list.map(a => ({
      ...a,
      statusText: a.status === 1 ? '正常' : '已禁用',
      createTime: a.createdAt ? a.createdAt.slice(0, 10) : '-',
      lastLogin: a.lastLogin ? a.lastLogin.slice(0, 16).replace('T', ' ') : '-',
    }))
  } catch (e) {
    message.error('加载失败')
  }
}

const filteredData = computed(() => {
  let list = data.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(a => (a.username || '').includes(keyword) || (a.name || '').includes(keyword))
  }
  if (roleFilter.value) {
    list = list.filter(a => a.role === roleFilter.value)
  }
  if (statusFilter.value) {
    const target = statusFilter.value === '正常' ? 1 : 0
    list = list.filter(a => a.status === target)
  }
  return list
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

function handleSearch() {
  currentPage.value = 1
}

function handleReset() {
  search.value = ''
  roleFilter.value = undefined
  statusFilter.value = undefined
  currentPage.value = 1
}

function handleAdd() {
  modalTitle.value = '新增管理员'
  editingId.value = null
  form.value = { username: '', name: '', role: undefined, password: 'Abc123456', status: 1 }
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑管理员'
  editingId.value = record.id
  form.value = {
    username: record.username,
    name: record.name || '',
    role: record.role || undefined,
    password: '',
    status: record.status === 1 ? 1 : 0,
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.username || !form.value.name || !form.value.role) {
    message.warning('请填写必填项')
    return
  }
  try {
    const payload = {
      id: editingId.value || undefined,
      username: form.value.username,
      name: form.value.name,
      role: form.value.role,
      status: form.value.status,
    }
    if (!editingId.value && form.value.password) {
      payload.password = form.value.password
    }
    await saveAdmin(payload)
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(record) {
  if (record.role === '超级管理员') {
    message.warning('超级管理员不可删除')
    return
  }
  Modal.confirm({
    title: '确认删除该管理员？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteAdmin(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

async function resetPassword(record) {
  try {
    await saveAdmin({ id: record.id, password: 'Abc123456' })
    message.success('密码已重置为 Abc123456')
  } catch (e) {
    message.error('重置失败')
  }
}

async function toggleStatus(record) {
  if (record.role === '超级管理员') {
    message.warning('超级管理员不可禁用')
    return
  }
  const newStatus = record.status === 1 ? 0 : 1
  try {
    await saveAdmin({ id: record.id, status: newStatus })
    message.success(newStatus === 1 ? '已启用' : '已禁用')
    loadData()
  } catch (e) {
    message.error('操作失败')
  }
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="search" placeholder="搜索管理员账号/姓名" style="width: 240px;" />
      <Select v-model:value="roleFilter" placeholder="全部角色" style="min-width: 140px;" allow-clear>
        <Select.Option value="超级管理员">超级管理员</Select.Option>
        <Select.Option value="内容管理员">内容管理员</Select.Option>
        <Select.Option value="运营管理员">运营管理员</Select.Option>
      </Select>
      <Select v-model:value="statusFilter" placeholder="全部状态" style="min-width: 140px;" allow-clear>
        <Select.Option value="正常">正常</Select.Option>
        <Select.Option value="已禁用">已禁用</Select.Option>
      </Select>
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button type="primary" style="margin-left: auto;" @click="handleAdd">+ 新增管理员</Button>
    </div>

    <Table :columns="columns" :data-source="paginatedData" :pagination="false" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <Tag :color="roleColorMap[record.role] || 'default'">{{ record.role }}</Tag>
        </template>
        <template v-if="column.key === 'status'">
          <Tag :color="record.status === 1 ? 'green' : 'red'">{{ record.statusText }}</Tag>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a v-if="record.role !== '超级管理员'" style="margin-right: 12px;" @click="resetPassword(record)">重置密码</a>
          <a v-if="record.role !== '超级管理员'" style="margin-right: 12px;" :style="{ color: record.status === 1 ? '#f5222d' : '#1890ff' }" @click="toggleStatus(record)">
            {{ record.status === 1 ? '禁用' : '启用' }}
          </a>
          <a v-if="record.role !== '超级管理员'" style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination v-model:current="currentPage" v-model:page-size="pageSize" :total="filteredData.length" show-total />
    </div>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="600">
    <Form layout="vertical" style="margin-top: 12px;">
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <Form.Item label="管理员账号" required>
          <Input v-model:value="form.username" placeholder="请输入登录账号" />
        </Form.Item>
        <Form.Item label="姓名" required>
          <Input v-model:value="form.name" placeholder="请输入真实姓名" />
        </Form.Item>
      </div>
      <Form.Item label="分配角色" required>
        <Select v-model:value="form.role" placeholder="请选择角色">
          <Select.Option value="超级管理员">超级管理员</Select.Option>
          <Select.Option value="内容管理员">内容管理员</Select.Option>
          <Select.Option value="运营管理员">运营管理员</Select.Option>
        </Select>
      </Form.Item>
      <Form.Item v-if="!editingId" label="初始密码" required>
        <Input v-model:value="form.password" />
        <div style="font-size: 12px; color: #999; margin-top: 4px;">首次登录后需强制修改</div>
      </Form.Item>
      <Form.Item v-if="editingId" label="账号状态">
        <Select v-model:value="form.status">
          <Select.Option :value="1">正常</Select.Option>
          <Select.Option :value="0">已禁用</Select.Option>
        </Select>
      </Form.Item>
    </Form>
  </Modal>
</template>

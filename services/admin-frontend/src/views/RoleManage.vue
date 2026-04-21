<script setup>
import { ref, computed, onMounted } from 'vue'
import { Card, Input, Button, Table, Tag, Modal, Form, Checkbox, message, Pagination } from 'ant-design-vue'
import { listRoles, getRoleAdminCounts, saveRole, deleteRole } from '../api/role.js'

const search = ref('')
const data = ref([])
const adminCounts = ref({})
const currentPage = ref(1)
const pageSize = ref(10)

const permOptions = [
  { group: '内容管理', items: [
    { key: 'track', label: '赛道管理' },
    { key: 'blogger', label: '博主管理' },
    { key: 'post', label: '文章管理' },
    { key: 'guide', label: '创作技巧' },
    { key: 'help', label: '帮助文档' },
  ]},
  { group: '运营管理', items: [
    { key: 'user', label: '用户管理' },
    { key: 'dashboard', label: '仪表盘查看' },
    { key: 'creation-review', label: '创作审核' },
  ]},
  { group: '系统管理', items: [
    { key: 'admin', label: '管理员管理' },
    { key: 'role', label: '角色权限管理' },
    { key: 'config', label: '系统配置' },
    { key: 'style', label: '样式管理' },
  ]},
]

const permLabelMap = {}
permOptions.forEach(g => g.items.forEach(i => { permLabelMap[i.key] = i.label }))

const columns = [
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '权限范围', key: 'perms' },
  { title: '关联管理员数', dataIndex: 'adminCount', key: 'adminCount', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120 },
  { title: '操作', key: 'action', width: 160 },
]

const modalOpen = ref(false)
const modalTitle = ref('新增角色')
const form = ref({ name: '' })
const editingId = ref(null)
const checkedPerms = ref({})

function resetCheckedPerms() {
  const obj = {}
  permOptions.forEach(g => g.items.forEach(i => { obj[i.key] = false }))
  checkedPerms.value = obj
}

resetCheckedPerms()

async function loadData() {
  try {
    const [roles, counts] = await Promise.all([listRoles(), getRoleAdminCounts()])
    adminCounts.value = counts || {}
    data.value = roles.map(r => {
      let perms = []
      try {
        perms = JSON.parse(r.permissions || '[]')
      } catch (e) {}
      return {
        ...r,
        perms,
        adminCount: adminCounts.value[r.name] || 0,
        createTime: r.createdAt ? r.createdAt.slice(0, 10) : '-',
      }
    })
  } catch (e) {
    message.error('加载失败')
  }
}

const filteredData = computed(() => {
  let list = data.value
  const keyword = search.value.trim()
  if (keyword) {
    list = list.filter(r => (r.name || '').includes(keyword))
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
  currentPage.value = 1
}

function handleAdd() {
  modalTitle.value = '新增角色'
  editingId.value = null
  form.value = { name: '' }
  resetCheckedPerms()
  modalOpen.value = true
}

function handleEdit(record) {
  modalTitle.value = '编辑角色'
  editingId.value = record.id
  form.value = { name: record.name }
  resetCheckedPerms()
  if (record.perms && record.perms.includes('all')) {
    permOptions.forEach(g => g.items.forEach(i => { checkedPerms.value[i.key] = true }))
  } else if (record.perms) {
    record.perms.forEach(k => { checkedPerms.value[k] = true })
  }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.name) {
    message.warning('请输入角色名称')
    return
  }
  const selected = Object.keys(checkedPerms.value).filter(k => checkedPerms.value[k])
  if (selected.length === 0) {
    message.warning('请至少勾选一项权限')
    return
  }
  try {
    const permissions = selected.length === Object.keys(checkedPerms.value).length ? '["all"]' : JSON.stringify(selected)
    await saveRole({
      id: editingId.value || undefined,
      name: form.value.name,
      permissions,
    })
    message.success((editingId.value ? '编辑' : '新增') + '成功')
    modalOpen.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

function handleDelete(record) {
  if (record.name === '超级管理员') {
    message.warning('超级管理员不可删除')
    return
  }
  if (record.adminCount > 0) {
    message.warning('该角色下还有管理员，请先调整管理员角色后再删除')
    return
  }
  Modal.confirm({
    title: '确认删除该角色？',
    content: '删除后数据将不在列表中显示。',
    async onOk() {
      try {
        await deleteRole(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    },
  })
}

function permLabels(perms) {
  if (!perms || perms.length === 0) return []
  if (perms.includes('all')) return [{ key: 'all', label: '全部权限', color: 'purple' }]
  return perms.map(k => ({ key: k, label: permLabelMap[k] || k, color: 'blue' }))
}

onMounted(loadData)
</script>

<template>
  <Card :body-style="{ padding: '24px' }" style="border-radius: 2px;">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; align-items: center;">
      <Input v-model:value="search" placeholder="搜索角色名称" style="width: 240px;" />
      <Button type="primary" @click="handleSearch">查询</Button>
      <Button @click="handleReset">重置</Button>
      <Button type="primary" style="margin-left: auto;" @click="handleAdd">+ 新增角色</Button>
    </div>

    <Table :columns="columns" :data-source="paginatedData" :pagination="false" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'perms'">
          <Tag v-for="p in permLabels(record.perms)" :key="p.key" :color="p.color" style="margin-right: 6px; margin-bottom: 4px;">{{ p.label }}</Tag>
        </template>
        <template v-if="column.key === 'action'">
          <a style="margin-right: 12px;" @click="handleEdit(record)">编辑</a>
          <a style="color: #f5222d;" @click="handleDelete(record)">删除</a>
        </template>
      </template>
    </Table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <Pagination v-model:current="currentPage" v-model:page-size="pageSize" :total="filteredData.length" show-total />
    </div>
  </Card>

  <Modal v-model:open="modalOpen" :title="modalTitle" :mask-closable="false" @ok="handleSave" :width="800">
    <Form layout="vertical" style="margin-top: 12px;">
      <Form.Item label="角色名称" required>
        <Input v-model:value="form.name" placeholder="请输入角色名称，如：内容管理员" />
      </Form.Item>
      <Form.Item label="权限配置" required>
        <div v-for="g in permOptions" :key="g.group" style="border: 1px solid #f0f0f0; border-radius: 2px; padding: 16px; margin-bottom: 12px;">
          <div style="font-size: 14px; font-weight: 500; color: #262626; margin-bottom: 12px;">{{ g.group }}</div>
          <div style="display: flex; flex-wrap: wrap; gap: 16px;">
            <Checkbox v-for="item in g.items" :key="item.key" v-model:checked="checkedPerms[item.key]">{{ item.label }}</Checkbox>
          </div>
        </div>
        <div style="font-size: 12px; color: #999;">勾选后，该角色下的管理员将拥有对应菜单的访问权限</div>
      </Form.Item>
    </Form>
  </Modal>
</template>
